package com.scaffold.starter.data.jpa.datachange;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 留痕测试事件收集器：同步收集 {@link DataChangedEvent} 供断言。
 *
 * @author lianghang
 */
@Component
public class EventCollector {

    private final List<DataChangedEvent> events = new ArrayList<>();

    @EventListener
    public void onDataChanged(DataChangedEvent event) {
        events.add(event);
    }

    public List<DataChangedEvent> events() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}
