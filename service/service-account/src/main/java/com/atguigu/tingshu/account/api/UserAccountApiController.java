package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.HwLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;

	//登录之后，远程调用：进行账户初始化
	@GetMapping("initUserAccount/{userId}")
	public Result initUserAccount(@PathVariable Long userId) {
		userAccountService.initUserAccount(userId);
		return Result.ok();
	}


	/**
	 * 获取账户可用余额
	 * @return
	 */
	@HwLogin
	@Operation(summary = "获取账号可用金额")
	@GetMapping("getAvailableAmount")
	public Result<BigDecimal> getAvailableAmount() {
		//	调用服务层方法
		return Result.ok(userAccountService.getAvailableAmount(AuthContextHolder.getUserId()));
	}

}

