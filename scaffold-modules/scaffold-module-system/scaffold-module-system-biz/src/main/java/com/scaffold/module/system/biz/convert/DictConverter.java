package com.scaffold.module.system.biz.convert;

import com.scaffold.module.system.api.dto.DictDataDTO;
import com.scaffold.module.system.biz.entity.SysDict;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典实体与 DTO 的转换器。
 *
 * <p>手写显式映射，字段一一对应、无隐藏拷贝语义；
 * 私有构造防止实例化，全部静态方法。</p>
 *
 * @author lianghang
 */
public final class DictConverter {

    private DictConverter() {
    }

    /**
     * 单条字典实体转 DTO。
     *
     * @param dict 字典实体，可为 {@code null}
     * @return 字典 DTO；入参为 {@code null} 时返回 {@code null}
     */
    public static DictDataDTO toDTO(SysDict dict) {
        if (dict == null) {
            return null;
        }
        DictDataDTO dto = new DictDataDTO();
        dto.setId(dict.getId());
        dto.setType(dict.getType());
        dto.setLabel(dict.getLabel());
        dto.setValue(dict.getValue());
        dto.setSort(dict.getSort());
        dto.setEnabled(dict.getEnabled());
        return dto;
    }

    /**
     * 字典实体列表转 DTO 列表。
     *
     * @param dicts 字典实体列表，可为 {@code null}
     * @return DTO 列表；入参为 {@code null} 时返回空列表
     */
    public static List<DictDataDTO> toDTOList(List<SysDict> dicts) {
        List<DictDataDTO> list = new ArrayList<>();
        if (dicts != null) {
            for (SysDict d : dicts) {
                list.add(toDTO(d));
            }
        }
        return list;
    }
}
