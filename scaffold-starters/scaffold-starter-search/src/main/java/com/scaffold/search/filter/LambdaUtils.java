package com.scaffold.search.filter;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

/**
 * 解析 SFunction lambda 引用的属性名。
 *
 * <p>原理：可序列化 lambda 会生成 {@code writeReplace} 方法，
 * 通过它拿到 {@link SerializedLambda}，再定位被引用的方法（getter），
 * 还原为属性名。
 */
public final class LambdaUtils {

    private LambdaUtils() {
    }

    /**
     * 解析 lambda 引用的属性名。
     *
     * @param fn 字段引用函数
     * @param <T> 实体类型
     * @return 属性名（如 {@code UserVO::getStatus} → {@code "status"}）
     */
    public static <T> String propertyName(SFunction<T, ?> fn) {
        try {
            Method writeReplace = fn.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(fn);

            String className = lambda.getImplClass().replace('/', '.');
            Class<?> clazz = Class.forName(className, false, LambdaUtils.class.getClassLoader());
            String methodName = lambda.getImplMethodName();

            Method method = findMethod(clazz, methodName);
            if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
                throw new IllegalArgumentException(
                        "SFunction 必须引用无参 getter 方法: " + clazz.getSimpleName() + "#" + methodName);
            }
            String name = methodName;
            if (name.startsWith("get") && name.length() > 3) {
                name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2) {
                name = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
            return name;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 SFunction lambda，请直接引用目标类的方法（如 UserVO::getStatus）", e);
        }
    }

    private static Method findMethod(Class<?> clazz, String methodName) {
        // lambda$ 前缀说明方法体内再包了一层 lambda，直接查找会失败，给出更友好的报错
        if (methodName.startsWith("lambda$")) {
            throw new IllegalArgumentException(
                    "SFunction 引用了 lambda 方法体（" + methodName + "），必须直接引用目标类的方法（如 UserVO::getStatus）");
        }
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        throw new IllegalArgumentException("找不到方法: " + clazz.getName() + "#" + methodName
                + " (declared: " + Arrays.toString(clazz.getDeclaredMethods()) + ")");
    }

    private static String decap(String s) {
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1)) && Character.isUpperCase(s.charAt(0))) {
            return s; // URL 之类的连续大写保持原样
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
