package com.scaffold.server.lite.demo;

import com.scaffold.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量版示例接口：验证零依赖场景下 HTTP 能力可用。
 *
 * <p>无 security starter 时本接口匿名可访问——lite 定位为单机运行形态，
 * 对外暴露接口时请自行接入鉴权或仅绑定内网地址。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/lite")
public class LitePingController {

    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "scaffold-server-lite");
        body.put("time", LocalDateTime.now().withNano(0).toString());
        body.put("mode", "single-machine, no db / no redis");
        return R.ok(body);
    }
}
