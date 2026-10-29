package com.scaffold.module.system.biz.convert;

import com.scaffold.module.system.api.dto.DictDataDTO;
import com.scaffold.module.system.biz.entity.SysDict;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典转换（手写显式映射）。
 */
public final class DictConverter {

    private DictConverter() {
    }

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
