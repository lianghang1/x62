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
 * 字典服务实现。
 *
 * <p>实现 {@code DictApi} 契约：查询走两级缓存（starter-cache），
 * 管理端保存时按类型精准失效缓存，避免全量刷新；
 * 其他模块经 {@code DictApi} 使用，零感知实现细节。</p>
 *
 * @author lianghang
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

    /**
     * 管理端保存字典项。
     *
     * @param dict 待保存的字典实体
     * @return 保存后的实体
     */
    @Transactional
    @CacheEvict(cacheNames = "dict:type", key = "#dict.type")
    public SysDict save(SysDict dict) {
        return dictRepository.save(dict);
    }
}
