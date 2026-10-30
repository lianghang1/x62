package com.scaffold.module.system.api.api;

/**
 * 系统参数配置契约：DB 配置中心的跨模块读出口。
 *
 * <p>运行时可改、不重启生效的参数（上传上限、开关、阈值等）统一经本接口读取，
 * 实现侧带进程内缓存与多实例失效广播；存储结构与缓存机制不外泄。</p>
 *
 * @author lianghang
 */
public interface ConfigApi {

    /** 按键取字符串值；不存在或已停用返回 null。 */
    String getValue(String key);

    /** 按键取整型值；值非法或不存在返回 null。 */
    Integer getIntValue(String key);

    /** 按键取布尔值；{@code true/false}（忽略大小写），其余或不存在返回 null。 */
    Boolean getBoolValue(String key);
}
