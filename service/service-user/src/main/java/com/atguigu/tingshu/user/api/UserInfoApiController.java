package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.HwLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	//根据用户id获取用户信息
	@Operation(summary = "根据用户id获取用户信息")
	@GetMapping("getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId){
		UserInfoVo userInfo = userInfoService.getUserInfo(userId);
		return Result.ok(userInfo);
	}

	/**
	 * 判断用户是否购买声音列表
	 * @param albumId
	 * @param trackIdList
	 * @return
	 */
	@HwLogin(required = false)
	@Operation(summary = "判断用户是否购买声音列表")
	@PostMapping("userIsPaidTrack/{userId}/{albumId}")
	Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable("userId") Long userId,
											   @PathVariable("albumId") Long albumId,
											   @RequestBody List<Long> trackIdList) {
		Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, trackIdList);
		//	返回map 集合数据
		return Result.ok(map);
	}

	/**
	 * 判断用户是否购买过专辑
	 * @param albumId
	 * @return
	 */
	@HwLogin
	@Operation(summary = "判断用户是否购买过专辑")
	@GetMapping("isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum (@PathVariable Long albumId){
		Long userId = AuthContextHolder.getUserId();
		Boolean flag = userInfoService.isPaidAlbum(albumId,userId);
		return Result.ok(flag);
	}
}

