package com.scaffold.server.lite.cemlink;

import com.scaffold.server.lite.cemlink.entity.WatermarkEntity;
import com.scaffold.server.lite.cemlink.increment.WatermarkStore;
import com.scaffold.server.lite.cemlink.repository.WatermarkRepository;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试辅助：内存版水位线仓储（模拟主库 JPA 行为，替代 sqlite 时代的临时文件）
 *
 * @author lianghang
 */
public final class TestWatermarkStores {

    private TestWatermarkStores() {
    }

    public static WatermarkStore inMemory() {
        Map<String, WatermarkEntity> store = new ConcurrentHashMap<>();
        WatermarkRepository repository = Mockito.mock(WatermarkRepository.class);
        Mockito.when(repository.findByDomainAndBizId(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(inv -> Optional.ofNullable(
                        store.get(inv.getArgument(0) + "/" + inv.getArgument(1))));
        Mockito.when(repository.save(Mockito.any(WatermarkEntity.class)))
                .thenAnswer(inv -> {
                    WatermarkEntity entity = inv.getArgument(0);
                    store.put(entity.getDomain() + "/" + entity.getBizId(), entity);
                    return entity;
                });
        return new WatermarkStore(repository);
    }
}
