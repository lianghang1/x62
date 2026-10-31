package com.scaffold.starter.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池配置属性，绑定前缀 {@code scaffold.executor}。
 *
 * <p>两组池，均可按需覆盖：</p>
 * <ul>
 *   <li>{@code biz}——框架预置的业务并行池 {@code scaffoldBizExecutor}，业务方法内
 *       需要多线程时直接注入使用（TTL 上下文与 MDC traceId 自动透传）；
 *       业务定义同名 Bean 即整体接管本配置；</li>
 *   <li>{@code async}——框架预置的 {@code @Async} 专属池 {@code scaffoldAsyncExecutor}，
 *       经 AsyncConfigurer 显式指定。不依赖 Boot 默认 applicationTaskExecutor——后者
 *       只要 classpath 出现任意 Executor Bean 就会退避，依赖它等于依赖装配顺序。
 *       业务定义同名 Bean 即整体接管本配置。</li>
 * </ul>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.executor")
public class BizExecutorProperties {

    /** 业务并行池配置。 */
    private Biz biz = new Biz();

    /** @Async 默认池补充配置。 */
    private Async async = new Async();

    public Biz getBiz() {
        return biz;
    }

    public void setBiz(Biz biz) {
        this.biz = biz;
    }

    public Async getAsync() {
        return async;
    }

    public void setAsync(Async async) {
        this.async = async;
    }

    public static class Biz {

        /** 是否装配业务并行池（false 时不注册 Bean，业务自行定义）。 */
        private boolean enabled = true;

        /** 核心线程数。 */
        private int coreSize = 4;

        /** 最大线程数。 */
        private int maxSize = 8;

        /** 有界队列容量：防任务无限堆积打爆内存。 */
        private int queueCapacity = 1000;

        /** 线程名前缀，日志排查必备。 */
        private String namePrefix = "scaffold-biz-";

        /** 优雅关闭：等待任务完成的最长秒数。 */
        private int awaitTerminationSeconds = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public int getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }

    public static class Async {

        /** 核心线程数。 */
        private int coreSize = 8;

        /** 最大线程数。 */
        private int maxSize = 16;

        /** 有界队列容量（Boot 默认池无界 Integer.MAX_VALUE，这里收紧为 1000）。 */
        private int queueCapacity = 1000;

        /** 线程名前缀。 */
        private String namePrefix = "scaffold-async-";

        /** 优雅关闭：等待任务完成的最长秒数。 */
        private int awaitTerminationSeconds = 30;

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public int getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }
}
