package com.scaffold.starter.data.jpa.datachange;

import com.scaffold.common.constant.TraceConstants;
import com.scaffold.common.event.EventOps;
import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 数据变更 Hibernate 事件监听器：对标注 {@link DataChange} 的实体，
 * 在事务提交后发布 {@link DataChangedEvent}（UPDATE 为字段级新旧值 diff）。
 *
 * <p>注册在 POST_COMMIT 组——只在数据真正落库后触发，回滚不发；
 * 发布动作本身不落库，由应用层异步消费。任何异常都吞掉只告警——
 * 留痕是尽力而为的副动作，不能反噬主流程。</p>
 *
 * <p>集合字段与懒加载代理不可安全求值，跳过该字段；审计字段
 * （create/update 时间与操作人）每次更新都变，属噪音，不参与 diff。</p>
 *
 * @author lianghang
 */
public class DataChangeEventListener implements PostCommitInsertEventListener,
        PostCommitUpdateEventListener, PostCommitDeleteEventListener, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DataChangeEventListener.class);

    /** 不参与 diff 的审计字段 */
    private static final List<String> EXCLUDED_FIELDS = Arrays.asList(
            "createTime", "updateTime", "createBy", "updateBy");

    /** 值不可安全求值的标记（集合/懒加载代理），diff 时跳过该字段 */
    private static final String UNSAFE = "\u0000unsafe";

    /** 单值最大长度，超长截断 */
    private static final int MAX_VALUE_LENGTH = 300;

    /** 单次变更最多记录字段数 */
    private static final int MAX_CHANGES = 50;

    private final EntityManagerFactory entityManagerFactory;

    public DataChangeEventListener(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void afterPropertiesSet() {
        SessionFactoryImplementor sessionFactory =
                entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry =
                sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_COMMIT_INSERT).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_COMMIT_UPDATE).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_COMMIT_DELETE).appendListener(this);
        log.info("数据变更留痕监听器已注册（@DataChange 实体生效）");
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        DataChange annotation = annotationOf(event.getPersister());
        if (annotation == null) {
            return;
        }
        try {
            List<DataChangedEvent.FieldChange> changes = new ArrayList<>();
            String[] names = event.getPersister().getPropertyNames();
            Object[] state = event.getState();
            for (int i = 0; i < names.length && changes.size() < MAX_CHANGES; i++) {
                String value = format(state[i]);
                if (value == null || UNSAFE.equals(value) || EXCLUDED_FIELDS.contains(names[i])) {
                    continue;
                }
                // deleted 初值恒为 false，属噪音；UPDATE 置位时才值得留痕
                if ("deleted".equals(names[i])) {
                    continue;
                }
                changes.add(new DataChangedEvent.FieldChange(names[i], null, value));
            }
            publish(annotation, tableNameOf(event.getPersister()), event.getId(),
                    DataChangedEvent.OperateType.INSERT, changes);
        } catch (Exception e) {
            log.warn("[data-change] 新增留痕失败: {}", event.getPersister().getEntityName(), e);
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        DataChange annotation = annotationOf(event.getPersister());
        if (annotation == null) {
            return;
        }
        try {
            List<DataChangedEvent.FieldChange> changes = new ArrayList<>();
            String[] names = event.getPersister().getPropertyNames();
            Object[] oldState = event.getOldState();
            Object[] newState = event.getState();
            for (int i = 0; i < names.length && changes.size() < MAX_CHANGES; i++) {
                if (EXCLUDED_FIELDS.contains(names[i])) {
                    continue;
                }
                String oldValue = format(oldState[i]);
                String newValue = format(newState[i]);
                if (UNSAFE.equals(oldValue) || UNSAFE.equals(newValue)) {
                    continue;
                }
                if (!Objects.equals(oldValue, newValue)) {
                    changes.add(new DataChangedEvent.FieldChange(names[i], oldValue, newValue));
                }
            }
            publish(annotation, tableNameOf(event.getPersister()), event.getId(),
                    DataChangedEvent.OperateType.UPDATE, changes);
        } catch (Exception e) {
            log.warn("[data-change] 更新留痕失败: {}", event.getPersister().getEntityName(), e);
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        DataChange annotation = annotationOf(event.getPersister());
        if (annotation == null) {
            return;
        }
        try {
            publish(annotation, tableNameOf(event.getPersister()), event.getId(),
                    DataChangedEvent.OperateType.DELETE, new ArrayList<>());
        } catch (Exception e) {
            log.warn("[data-change] 删除留痕失败: {}", event.getPersister().getEntityName(), e);
        }
    }

    /** 实体是否标注 {@link DataChange}。 */
    private DataChange annotationOf(org.hibernate.persister.entity.EntityPersister persister) {
        Class<?> mappedClass = persister.getMappedClass();
        return mappedClass.getAnnotation(DataChange.class);
    }

    /** 表名：EntityPersister 接口不暴露，降级取实现类或实体名。 */
    private String tableNameOf(org.hibernate.persister.entity.EntityPersister persister) {
        return persister instanceof AbstractEntityPersister
                ? ((AbstractEntityPersister) persister).getTableName()
                : persister.getEntityName();
    }

    /** 提交失败（回滚）：不发事件。 */
    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
        // 回滚不发事件——留痕只对成功落库生效
    }

    /** 提交失败（回滚）：不发事件。 */
    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
        // 回滚不发事件——留痕只对成功落库生效
    }

    /** 提交失败（回滚）：不发事件。 */
    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
        // 回滚不发事件——留痕只对成功落库生效
    }

    /** 声明需要提交后延迟分发——否则事件在事务内即时触发而非提交后。 */
    @Override
    public boolean requiresPostCommitHanding(org.hibernate.persister.entity.EntityPersister persister) {
        return true;
    }

    /** 组装并发布事件：操作人/traceId 在当前（请求）线程取齐，异步消费方无需还原上下文。 */
    private void publish(DataChange annotation, String tableName, Object id,
                         DataChangedEvent.OperateType type, List<DataChangedEvent.FieldChange> changes) {
        String businessName = annotation.value().isEmpty()
                ? "" : annotation.value();
        EventOps.publish(new DataChangedEvent(businessName, tableName, String.valueOf(id),
                type, changes, ScaffoldAuditorHolder.get(), MDC.get(TraceConstants.TRACE_ID_KEY),
                LocalDateTime.now()));
    }

    /** 值格式化：null 透传；集合/代理返回不可求值标记；其余字符串化并截断。 */
    private String format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof PersistentCollection || value instanceof HibernateProxy) {
            return UNSAFE;
        }
        String text = String.valueOf(value);
        return text.length() > MAX_VALUE_LENGTH ? text.substring(0, MAX_VALUE_LENGTH) + "..." : text;
    }
}
