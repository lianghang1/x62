package com.scaffold.module.system.biz.service;

import com.scaffold.module.system.api.api.DictApi;
import com.scaffold.module.system.api.dto.DictDataDTO;
import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.module.system.biz.convert.DictConverter;
import com.scaffold.module.system.biz.entity.SysDict;
import com.scaffold.module.system.biz.repository.SysDictRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典实现：走两级缓存（starter-cache），其他模块通过 DictApi 使用、零感知实现细节。
 */
@Service
public class DictServiceImpl implements DictApi {

    private final SysDictRepository dictRepository;

    public DictServiceImpl(SysDictRepository dictRepository) {
        this.dictRepository = dictRepository;
    }

    @Override
    @Cacheable(cacheNames = "dict:type", key = "#type")
    public List<DictDataDTO> listByType(String type) {
        return DictConverter.toDTOList(dictRepository.findByTypeAndEnabledTrueAndDeletedFalseOrderBySortAsc(type));
    }

    @Override
    public String translate(String type, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        for (DictDataDTO dto : listByType(type)) {
            if (value.equals(dto.getValue())) {
                return dto.getLabel();
            }
        }
        return value;
    }

    @Override
    public void validate(String type, String value) {
        for (DictDataDTO dto : listByType(type)) {
            if (value.equals(dto.getValue())) {
                return;
            }
        }
        throw BizException.of(GlobalErrorCode.BAD_REQUEST, "非法字典值: " + type + "=" + value);
    }

    @Transactional
    @CacheEvict(cacheNames = "dict:type", key = "#dict.type")
    public SysDict save(SysDict dict) {
        return dictRepository.save(dict);
    }
}
