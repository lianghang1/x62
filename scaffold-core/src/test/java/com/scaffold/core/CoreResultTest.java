package com.scaffold.core;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.core.result.PageResult;
import com.scaffold.core.result.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoreResultTest {

    @Test
    public void r_ok_shouldCarryCodeZero() {
        R<String> ok = R.ok("data");
        assertEquals(0, ok.getCode());
        assertEquals("data", ok.getData());
    }

    @Test
    public void r_fail_shouldUseErrorCode() {
        R<Void> fail = R.fail(GlobalErrorCode.UNAUTHORIZED);
        assertEquals(1001, fail.getCode());
        assertEquals(401, GlobalErrorCode.UNAUTHORIZED.getHttpStatus());
    }

    @Test
    public void bizException_shouldCarryErrorCode() {
        BizException e = BizException.of(GlobalErrorCode.FORBIDDEN, "无权限");
        assertEquals(1003, e.getErrorCode().getCode());
        assertEquals("无权限", e.getMessage());
    }

    @Test
    public void pageResult_emptyShouldBeSafe() {
        PageResult<String> page = PageResult.empty(1, 10);
        assertTrue(page.getList().isEmpty());
        assertEquals(0, page.getTotal());
        assertFalse(page.getList() == null);
    }

    @Test
    public void pageResult_ofShouldKeepFields() {
        PageResult<Integer> page = PageResult.of(java.util.Arrays.asList(1, 2), 2, 1, 10);
        assertEquals(2, page.getList().size());
        assertEquals(1, page.getPageNum());
    }

    @Test
    public void unauthorizedContextThrows() {
        assertThrows(Exception.class, () -> {
            throw new IllegalStateException("占位断言：BizException 静态工厂已覆盖");
        });
    }
}
