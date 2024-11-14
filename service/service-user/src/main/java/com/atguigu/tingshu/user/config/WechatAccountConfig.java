package com.atguigu.tingshu.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * ClassName: WechatAccountConfig
 * Package: com.atguigu.tingshu.user.config
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/4 19:19
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.login")
@RefreshScope
public class WechatAccountConfig {
    //  公众平台的appId
    private String appId;
    //  小程序微信公众平台秘钥
    private String appSecret;
}
