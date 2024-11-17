package com.atguigu.tingshu.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: HwCache
 * Package: com.atguigu.tingshu.common.cache
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/17 23:03
 * @Version 1.0
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HwCache {
    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "cache";
}
