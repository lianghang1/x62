package com.scaffold.starter.data.jpa.datachange;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 留痕测试服务：保存后故意抛异常，驱动事务回滚。
 *
 * @author lianghang
 */
@Service
public class RollbackService {

    @Autowired
    TrackedRepository trackedRepository;

    @Transactional
    public void saveThenFail(TrackedRecord record) {
        trackedRepository.save(record);
        throw new IllegalStateException("故意失败，驱动回滚");
    }
}
