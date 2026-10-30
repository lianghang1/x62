package com.scaffold.starter.job;

/**
 * 任务执行日志的落库出口抽象，与 JPA 类型解耦。
 *
 * <p>引入本接口是为了让 starter 在无数据库场景（如单机零依赖运行）下可整体加载：
 * JPA 相关类型只出现在 {@link JpaJobRunLogSink} 实现里，经条件装配按类路径决定是否生效，
 * 接口本身可脱离 JPA 加载——业务子类与调度链路零感知。</p>
 *
 * @author lianghang
 */
public interface JobRunLogSink {

    /**
     * 任务开始落一行 RUNNING 记录。
     *
     * @param jobName 任务名
     * @return 执行日志 id；落库不可用时返回 null（不影响任务执行）
     */
    Long begin(String jobName);

    /**
     * 任务结束回写终态。
     *
     * @param runLogId   begin 返回的日志 id，null 直接忽略
     * @param status     终态：SUCCESS / FAIL
     * @param costMillis 耗时（毫秒）
     * @param error      失败原因，成功时为 null
     */
    void finish(Long runLogId, String status, long costMillis, Exception error);
}
