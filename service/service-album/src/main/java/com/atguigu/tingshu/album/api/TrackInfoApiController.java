package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.login.HwLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"all"})
public class TrackInfoApiController {
	@Autowired
	private TrackInfoService trackInfoService;

	@Autowired
	private VodService vodService;

	/**
	 * 保存修改声音数据
	 * @param id
	 * @param trackInfoVo
	 * @return
	 */
	@Operation(summary = "修改声音")
	@PutMapping("updateTrackInfo/{id}")
	public Result updateById(@PathVariable Long id, @RequestBody @Validated TrackInfoVo trackInfoVo) {
		//	调用服务层方法
		trackInfoService.updateTrackInfo(id, trackInfoVo);
		return Result.ok();
	}

	/**
	 * 根据Id 获取数据
	 * @param id
	 * @return
	 */
	@Operation(summary = "获取声音信息")
	@GetMapping("getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
		//	调用服务层方法
		TrackInfo trackInfo = trackInfoService.getById(id);
		return Result.ok(trackInfo);
	}

	/**
	 * 删除声音
	 * @param id
	 * @return
	 */
	@Operation(summary = "删除声音信息")
	@DeleteMapping("removeTrackInfo/{id}")
	public Result removeTrackInfo(@PathVariable Long id) {
		//	调用服务层方法
		trackInfoService.removeTrackInfo(id);
		return Result.ok();
	}

	@Autowired
	private RedisTemplate redisTemplate;

	@HwLogin(required = true)
	@Operation(summary = "获取当前用户声音分页列表")
	@PostMapping("findUserTrackPage/{page}/{limit}")
	public Result<IPage<TrackListVo>> findUserTrackPage(@PathVariable Long page,
														@PathVariable Long limit,
														@RequestBody TrackInfoQuery trackInfoQuery,
														HttpServletRequest request) {
//		//**第一步 从请求头获取token，token是否存在**
//		String token = request.getHeader("token");
//		if(StringUtils.isEmpty(token)) {//没有登录
//			throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
//		}
//		//**第二步 根据token查询redis，是否可以查询到数据**
//		UserInfo userInfo = (UserInfo)redisTemplate.opsForValue().get(token);
//		if(userInfo == null) {//没有登录
//			throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
//		}

		//TODO
		trackInfoQuery.setUserId(1L);
		//创建page对象
		Page<TrackListVo> trackListVoPage = new Page<>(page,limit);
		//调用service
		IPage<TrackListVo> pageModel = trackInfoService.findUserTrackPage(trackListVoPage,trackInfoQuery);
		return Result.ok(pageModel);
	}
	/**
	 * 保存声音
	 * @param trackInfoVo
	 * @return
	 */
	@Operation(summary = "新增声音")
	@PostMapping("saveTrackInfo")
	public Result saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo) {
		// 调用服务层方法
		trackInfoService.saveTrackInfo(trackInfoVo,1L);
		return Result.ok();
	}

	/**
	 * 上传声音
	 * @param file
	 * @return
	 */
	@Operation(summary = "上传声音")
	@PostMapping("uploadTrack")
	public Result<Map<String,Object>> uploadTrack(MultipartFile file) {
		//	调用服务层方法
		Map<String,Object> map = vodService.uploadTrack(file);
		return Result.ok(map);
	}

	/**
	 * 根据专辑Id获取声音列表
	 * @param albumId
	 * @param page
	 * @param limit
	 * @return
	 */
	@HwLogin(required = false)
	@Operation(summary = "获取专辑声音分页列表")
	@GetMapping("findAlbumTrackPage/{albumId}/{page}/{limit}")
	public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(@PathVariable Long albumId , @PathVariable Long page, @PathVariable Long limit){
		//获取userId
		Long userId = AuthContextHolder.getUserId();
		//创建page对象，设置当前页和每页记录数
		Page<AlbumTrackListVo> pageParam = new Page<>(page,limit);
		IPage<AlbumTrackListVo> pageModel1 = trackInfoService.findAlbumTrackPage(pageParam,albumId,userId);
		return Result.ok(pageModel1);
	}
}
