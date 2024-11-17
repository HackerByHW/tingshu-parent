package com.atguigu.tingshu.common.cache;

import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: HwCacheAspect
 * Package: com.atguigu.tingshu.common.cache
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/17 23:10
 * @Version 1.0
 */
 @Aspect
@Component
 public class HwCacheAspect {

        @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

       //当方法上有HwCache时候，这个方法执行
    @SneakyThrows
    @Around("@annotation(com.atguigu.tingshu.common.cache.HwCache)")
    public Object cacheAspect(ProceedingJoinPoint joinPoint) {
        //1 获取业务方法上注解里面前缀值 album:Info @HwCache(prefix="album:Info")
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        HwCache hwCache = signature.getMethod().getAnnotation(HwCache.class);
        String prefix = hwCache.prefix();

        //2 获取业务方法里面参数值 id值  public AlbumInfo getAlbumInfoById(Long id)
        Object[] args = joinPoint.getArgs();

        //3 根据前缀值 + id值生成redis的key
        String key = prefix+ Arrays.asList(args).toString();

        //4 查询redis
        Object o = redisTemplate.opsForValue().get(key);
        if(o == null) {
            try {
                //5 如果redis查询不到数据，查询mysql，把mysql数据放到redis里面，返回数据
                //5.1 添加分布式锁
                //获取锁对象
                RLock rLock = redissonClient.getLock(key + ":lock");
                //加锁
                boolean tryLock = rLock.tryLock(10, 30, TimeUnit.SECONDS);
                if(tryLock) {//加锁成功
                    try {
                        //查询mysql
                        Object obj = joinPoint.proceed(args);
                        if (null == obj){
                            // 并把结果放入缓存
                            obj = new Object();
                            this.redisTemplate.opsForValue().set(key, obj, 1,TimeUnit.HOURS);
                            return obj;
                        }
                        redisTemplate.opsForValue().set(key,obj,1,TimeUnit.HOURS);
                        return obj;
                    } finally {
                        rLock.unlock();
                    }
                } else {//加锁失败
                    return cacheAspect(joinPoint);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        } else {
            //6 如果redis查询到数据，直接返回
            return o;
        }
    }
}