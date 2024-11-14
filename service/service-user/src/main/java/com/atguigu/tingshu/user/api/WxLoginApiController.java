package com.atguigu.tingshu.user.api;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.HwLogin;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 更新用户信息
     * @param userInfoVo
     * @return
     */
    @HwLogin
    @Operation(summary = "更新用户信息")
    @PostMapping("updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo){
        //  获取到用户Id
        Long userId = AuthContextHolder.getUserId();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());

        //  执行更新方法
        userInfoService.updateById(userInfo);
        return Result.ok();
    }

    @HwLogin
    @Operation(summary = "获取登录信息")
    @GetMapping("getUserInfo")
    public Result getUserInfo(){
        //获取当前登录用户id
        Long userId = AuthContextHolder.getUserId();
        //根据userid查询用户信息，返回
        //        UserInfo userInfo = userInfoService.getById(userId);
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);

        return Result.ok(userInfoVo);
    }

    @Operation(summary = "小程序授权登录")
    @GetMapping("/wxLogin/{code}")
    public Result wxLogin(@PathVariable String code) throws Exception {
        //1 获取前端传递code值
        //2 拿着code + 小程序id + 秘钥  请求微信服务器接口，得到openid
        // 底层使用httpClient
        WxMaJscode2SessionResult sessionInfo =
                wxMaService.getUserService().getSessionInfo(code);
        String openid = sessionInfo.getOpenid();

        //3 根据openid判断是否第一次登录
        //如果第一次登录，添加用户信息，初始化账户
        //根据openid查询user_info表，如果查询不到数据，第一次登录
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getWxOpenId, openid);
        UserInfo userInfo = userInfoService.getOne(wrapper);
        if (userInfo == null) { //第一次登录
            //添加用户信息
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openid);
            userInfo.setNickname("听友" + System.currentTimeMillis());
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            userInfoService.save(userInfo);

            //TODO 分布式事务，后面讲到
            //初始化账户
            //方法一：远程调用 在service-account创建初始化接口
            // 在service-user进行远程调用实现
            //userAccountFeignClient.initUserAccount(userInfo.getId());

            //方法二：异步方式，消息队列实现
            rabbitService.sendMessage(MqConst.EXCHANGE_USER,
                    MqConst.ROUTING_USER_REGISTER,
                    userInfo.getId());
        }

            //4 生成token，把用户信息存储到redis里面
            //设置redis数据过期时间
            //redis的key是token，value是用户信息
            String token = UUID.randomUUID().toString().replaceAll("-", "");
            //把用户信息存储到redis里面
            redisTemplate.opsForValue().set(token,
                    userInfo,
                    RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                    TimeUnit.SECONDS);

            //5 返回token
            HashMap<String, Object> map = new HashMap<>();
            map.put("token", token);
            return Result.ok(map);
        }

    }




