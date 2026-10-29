package com.scaffold.module.system.api.api;

import com.scaffold.module.system.api.dto.DictDataDTO;

import java.util.List;

/**
 * 数据字典契约（业务性横切的标准案例，详见计划文档 1.2 节）。
 */
public interface DictApi {

    /** 翻译：字典项存储值 -> 显示文本；未命中返回原值 */
    String translate(String type, String value);

    /** 某类型的全部启用项 */
    List<DictDataDTO> listByType(String type);

    /** 校验 value 是否为 type 的合法字典值（不合法抛 BizException 1000） */
    void validate(String type, String value);
}
