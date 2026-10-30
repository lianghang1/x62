package com.scaffold.starter.protection.lock;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 分布式锁切面，配合 {@link DistributedLock} 注解工作。
 *
 * <p>锁 key 支持 SpEL 表达式（如 "'order:' + #p0"），取锁失败抛 1409 业务异常；
 * finally 确保解锁——注解即锁，业务代码零侵入。</p>
 *
 * @author lianghang
 */
@Aspect
public class DistributedLockAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final RedisLockUtil lockUtil;

    public DistributedLockAspect(RedisLockUtil lockUtil) {
        this.lockUtil = lockUtil;
    }

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock lock) throws Throwable {
        String fullKey = lock.key() + ":" + resolveSpel(pjp, lock.key());
        String holder = lockUtil.tryLock(fullKey, lock.waitMillis(), lock.leaseSeconds());
        if (holder == null) {
            throw BizException.of(GlobalErrorCode.CONFLICT, "操作冲突，请稍后重试（获取锁失败）");
        }
        try {
            return pjp.proceed();
        } finally {
            lockUtil.unlock(fullKey, holder);
        }
    }

    private String resolveSpel(ProceedingJoinPoint pjp, String expression) {
        if (!expression.contains("#")) {
            return "";
        }
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] names = NAME_DISCOVERER.getParameterNames(signature.getMethod());
            Object[] args = pjp.getArgs();
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            Object value = PARSER.parseExpression(expression).getValue(context);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }
}
