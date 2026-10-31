package com.scaffold.starter.web.exception;

import com.scaffold.common.util.StringUtils;
import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.ErrorCode;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器。
 *
 * <p>规约：
 * <ul>
 *   <li>业务异常（BizException）只打 warn——它是可预期的业务失败，打 error 会淹没告警；</li>
 *   <li>未知异常打 error（自动带 MDC traceId）并向外隐藏堆栈细节；</li>
 *   <li>HTTP 状态码与业务码双轨一致：ErrorCode 未指定 httpStatus 时返回 200，认证/权限类返回 401/403。</li>
 * </ul>
 *
 * @author lianghang
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("[biz] code={} message={}", ec.getCode(), e.getMessage());
        return build(ec, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<R<Void>> handleValidation(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String message = fe == null ? GlobalErrorCode.BAD_REQUEST.getMessage()
                : fe.getField() + " " + fe.getDefaultMessage();
        return build(GlobalErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().isEmpty()
                ? GlobalErrorCode.BAD_REQUEST.getMessage()
                : e.getConstraintViolations().iterator().next().getMessage();
        return build(GlobalErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("[request] 请求体不可读: {}", e.getMessage());
        return build(GlobalErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return build(GlobalErrorCode.METHOD_NOT_ALLOWED, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception e) {
        log.error("[system] 未捕获异常", e);
        // 对外隐藏内部细节，避免泄漏堆栈/SQL 等信息
        return build(GlobalErrorCode.SYSTEM_ERROR, null);
    }

    private ResponseEntity<R<Void>> build(ErrorCode ec, String customMessage) {
        String message = StringUtils.defaultIfEmpty(customMessage, ec.getMessage());
        int httpStatus = ec.getHttpStatus() == -1 ? 200 : ec.getHttpStatus();
        return ResponseEntity.status(httpStatus).body(R.fail(ec, message));
    }
}
