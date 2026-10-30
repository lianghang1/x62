package com.scaffold.module.system.api.api;

import com.scaffold.module.system.api.dto.DictDataDTO;

import java.util.List;

/**
 * 数据字典契约，业务性横切的标准案例。
 *
 * <p>各模块通过本接口完成字典值翻译与合法性校验，不直接依赖字典表：
 * 翻译未命中时返回原值兜底；校验不合法时抛 {@code BizException}（错误码 1000），
 * 让调用方无需关心字典存储细节即可获得一致的横切语义。</p>
 *
 * @author lianghang
 */
public interface DictApi {

    /**
     * 翻译：字典项存储值转为显示文本。
     *
     * @param type  字典类型
     * @param value 存储值
     * @return 显示文本；未命中返回原值
     */
    String translate(String type, String value);

    /**
     * 查询某类型的全部启用项。
     *
     * @param type 字典类型
     * @return 启用的字典项列表
     */
    List<DictDataDTO> listByType(String type);

    /**
     * 校验 value 是否为 type 的合法字典值。
     *
     * @param type  字典类型
     * @param value 待校验的值
     */
    void validate(String type, String value);
}
