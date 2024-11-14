package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * ClassName: HwLoginAspect
 * Package: com.atguigu.tingshu.common.login
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/4 17:16
 * @Version 1.0
 */
@Aspect
@Component
public class HwLoginAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    //环绕通知
    @SneakyThrows
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(hwLogin)")
    public Object around(ProceedingJoinPoint joinPoint, HwLogin hwLogin) {

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = sra.getRequest();

        //**第一步 从请求头获取token，token是否存在**
        String token = request.getHeader("token");

        //判断是否必须要进行登录校验 required = false
        if (hwLogin.required()) {//必须登录校验
            //判断token
            if (StringUtils.isEmpty(token)) {
                //前端根据返回值208，是否跳转登录页面
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
            //根据token查询redis
            UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(token);
            if (userInfo == null) {
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
        }
        // 获取redis里面userId 放到threadLocal
        if (!StringUtils.isEmpty(token)) {
            UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(token);
            if (userInfo != null) {
                Long userId = userInfo.getId();
                AuthContextHolder.setUserId(userId);

            }
        }
        // 执行业务方法，放行
        Object o = joinPoint.proceed();
        return o;

    }

}
