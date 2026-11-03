package com.scaffold.server.lite.cemlink.repository;

import com.scaffold.server.lite.cemlink.entity.WatermarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 采集水位线仓储（主库）
 *
 * @author lianghang
 */
public interface WatermarkRepository extends JpaRepository<WatermarkEntity, Long> {

    Optional<WatermarkEntity> findByDomainAndBizId(String domain, String bizId);
}
