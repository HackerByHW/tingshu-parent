package com.atguigu.tingshu.user.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.atguigu.tingshu.ServiceUserApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * ClassName: WechatMpConfig
 * Package: com.atguigu.tingshu.user.config
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/4 19:22
 * @Version 1.0
 */
@Component
public class WechatMpConfig {
    @Autowired
    private WechatAccountConfig wechatAccountConfig;

    @Bean
    public WxMaService wxMaService(){
        //  创建对象
        WxMaDefaultConfigImpl wxMaConfig =  new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wechatAccountConfig.getAppId());
        wxMaConfig.setSecret(wechatAccountConfig.getAppSecret());
        wxMaConfig.setMsgDataFormat("JSON");
        //  创建 WxMaService 对象
        WxMaService service = new WxMaServiceImpl();
        //  给 WxMaService 设置配置选项
        service.setWxMaConfig(wxMaConfig);
        return service;
    }

}
