package com.scaffold.starter.message;

import com.scaffold.starter.protection.abuse.AbuseException;
import com.scaffold.starter.protection.abuse.AbuseGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * 短信防刷闸门测试（mock AbuseGuard 验证开关与阈值联动）。
 *
 * @author lianghang
 */
class SmsAbuseGuardTest {

    @Test
    void disabledGuardSkipsAllChecks() {
        AbuseGuard abuseGuard = mock(AbuseGuard.class);
        SmsAbuseGuard.Config config = new SmsAbuseGuard.Config();
        config.setEnabled(false);
        SmsAbuseGuard guard = new SmsAbuseGuard(abuseGuard, config);

        assertDoesNotThrow(() -> guard.check("13800000000"));
    }

    @Test
    void intervalRejectionPropagatesAs429() {
        AbuseGuard abuseGuard = mock(AbuseGuard.class);
        doThrow(new AbuseException("sms", "phone:13800000000", "操作过于频繁"))
                .when(abuseGuard).checkInterval(anyString(), contains("13800000000"), anyInt());
        SmsAbuseGuard guard = new SmsAbuseGuard(abuseGuard, new SmsAbuseGuard.Config());

        AbuseException exception = assertThrows(AbuseException.class, () -> guard.check("13800000000"));
        assertEquals("sms", exception.getScene());
    }

    @Test
    void passThroughWhenAllGatesOk() {
        AbuseGuard abuseGuard = mock(AbuseGuard.class);
        doNothing().when(abuseGuard).checkInterval(anyString(), anyString(), anyInt());
        SmsAbuseGuard guard = new SmsAbuseGuard(abuseGuard, new SmsAbuseGuard.Config());

        assertDoesNotThrow(() -> guard.check("13800000000"));
    }
}
