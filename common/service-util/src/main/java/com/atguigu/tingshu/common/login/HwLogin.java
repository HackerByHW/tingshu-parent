package com.atguigu.tingshu.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: HwLogin
 * Package: com.atguigu.tingshu.common.login
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/4 14:02
 * @Version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HwLogin {
    /**
     * 是否必须要登录
     * @return
     */
    boolean required() default true;
}
