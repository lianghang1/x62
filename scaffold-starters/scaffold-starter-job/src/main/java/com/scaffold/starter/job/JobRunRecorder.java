package com.scaffold.starter.job;

import com.scaffold.starter.protection.lock.RedisLockUtil;

/**
 * 任务运行环境的线程级载体：基类通过 ThreadLocal 感知当前日志出口与锁工具。
 *
 * <p>本类不触碰任何 JPA 类型（落库细节在 {@link JobRunLogSink} 实现里），
 * 无数据库、无 Redis 场景（单机零依赖运行）下各依赖为 null，
 * 自动退化为「不落日志、不加锁放行」的单机语义，业务子类无感知。</p>
 *
 * @author lianghang
 */
public final class JobRunRecorder {

    private static final ThreadLocal<JobRunRecorder> CURRENT = new ThreadLocal<>();

    private final JobRunLogSink sink;
    private final RedisLockUtil lockUtil;

    private JobRunRecorder(JobRunLogSink sink, RedisLockUtil lockUtil) {
        this.sink = sink;
        this.lockUtil = lockUtil;
    }

    public static void install(JobRunLogSink sink, RedisLockUtil lockUtil) {
        CURRENT.set(new JobRunRecorder(sink, lockUtil));
    }

    static JobRunRecorder current() {
        return CURRENT.get();
    }

    public static void uninstall() {
        CURRENT.remove();
    }

    /**
     * 尝试获取分布式锁。
     *
     * <p>返回持锁者标识，null 表示其他节点持有中（调用方跳过本次执行）；
     * 无 Redis 时为单机语义直接放行，返回伪持锁者 {@code local}，
     * 避免与「获取锁失败」混淆导致任务永远不执行。</p>
     */
    String tryLock(String key, long waitMillis, int leaseSeconds) {
        if (lockUtil == null) {
            return "local";
        }
        return lockUtil.tryLock(key, waitMillis, leaseSeconds);
    }

    void unlock(String key, String holder) {
        if (lockUtil != null) {
            lockUtil.unlock(key, holder);
        }
    }

    Long begin(String jobName) {
        if (sink == null) {
            return null;
        }
        return sink.begin(jobName);
    }

    void success(Long runLogId, long costMillis) {
        if (sink != null) {
            sink.finish(runLogId, "SUCCESS", costMillis, null);
        }
    }

    void fail(Long runLogId, long costMillis, Exception error) {
        if (sink != null) {
            sink.finish(runLogId, "FAIL", costMillis, error);
        }
    }
}
