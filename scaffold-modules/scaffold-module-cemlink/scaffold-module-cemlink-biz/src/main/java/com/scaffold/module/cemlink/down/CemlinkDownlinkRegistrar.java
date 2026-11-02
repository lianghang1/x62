package com.scaffold.module.cemlink.down;

import com.linkora.client.LinkoraClient;
import com.scaffold.module.cemlink.client.CemlinkTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 下行监听装配：契约 client 自动扫描（@MallLinkoraDownlink）就绪后由此切换
 *
 * @author lianghang
 */
public class CemlinkDownlinkRegistrar implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CemlinkDownlinkRegistrar.class);

    private final LinkoraClient linkoraClient;
    private final ServiceItemDownlinkHandler serviceItemDownlinkHandler;
    private final WorkDownlinkHandler workDownlinkHandler;
    private final DepositBindDownlinkHandler depositBindDownlinkHandler;

    public CemlinkDownlinkRegistrar(LinkoraClient linkoraClient,
                                    ServiceItemDownlinkHandler serviceItemDownlinkHandler,
                                    WorkDownlinkHandler workDownlinkHandler,
                                    DepositBindDownlinkHandler depositBindDownlinkHandler) {
        this.linkoraClient = linkoraClient;
        this.serviceItemDownlinkHandler = serviceItemDownlinkHandler;
        this.workDownlinkHandler = workDownlinkHandler;
        this.depositBindDownlinkHandler = depositBindDownlinkHandler;
    }

    @Override
    public void afterSingletonsInstantiated() {
        linkoraClient.addListener(CemlinkTopics.DOWNLINK_SERVICE_ITEM, serviceItemDownlinkHandler);
        linkoraClient.addListener(CemlinkTopics.DOWNLINK_WORK_CHECK, workDownlinkHandler);
        linkoraClient.addListener(CemlinkTopics.DOWNLINK_DEPOSIT_BIND, depositBindDownlinkHandler);
        log.info("[cemlink] 下行监听已注册：服务项目回写 / 工单验收 / 寄存绑定");
    }
}
