package com.scaffold.starter.data.jpa;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import com.scaffold.starter.data.jpa.id.Snowflake;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 与审计持有器的单元测试。
 *
 * <p>验证雪花 ID 的唯一性、单实例内单调递增、不同 workerId 不冲突与非法节点
 * 标识快速失败，以及 {@code ScaffoldAuditorHolder} 的写入、读取与清理往返。</p>
 *
 * @author lianghang
 */
public class SnowflakeAndAuditTest {

    @Test
    public void snowflake_shouldBeUniqueAndMonotonic() {
        Snowflake snowflake = new Snowflake(1, 1);
        Set<Long> ids = new HashSet<>();
        long previous = -1;
        for (int i = 0; i < 10000; i++) {
            long id = snowflake.nextId();
            assertTrue(id > previous, "同一实例内应单调递增");
            previous = id;
            ids.add(id);
        }
        assertEquals(10000, ids.size(), "不允许重复");
    }

    @Test
    public void snowflake_differentWorkerShouldNotCollide() {
        Snowflake a = new Snowflake(1, 1);
        Snowflake b = new Snowflake(2, 1);
        assertNotEquals(a.nextId(), b.nextId());
    }

    @Test
    public void auditorHolder_shouldRoundTrip() {
        ScaffoldAuditorHolder.set("123456");
        assertEquals("123456", ScaffoldAuditorHolder.get());
        ScaffoldAuditorHolder.clear();
        assertEquals(null, ScaffoldAuditorHolder.get());
    }

    @Test
    public void snowflake_invalidWorkerShouldFail() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Snowflake(99, 1));
    }
}
