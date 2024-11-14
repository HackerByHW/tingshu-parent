package com.atguigu.tingshu.account.client.impl;


import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import org.springframework.stereotype.Component;

@Component
public class UserAccountDegradeFeignClient implements UserAccountFeignClient {
    @Override
    public Result initUserAccount(Long userId) {
        return null;
    }
}
