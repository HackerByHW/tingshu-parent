package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.HwLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;

	//添加专辑
	@Operation(summary = "新增专辑")
	@PostMapping("saveAlbumInfo")
	private Result save(@RequestBody @Validated AlbumInfoVo albumInfoVo){
		//TODO 需要用户id，临时添加固定值，后续完善
		Long userId = 1L;
		albumInfoService.saveAlbumInfo(albumInfoVo, userId);
		return Result.ok();
	}

	/**
	 * 获取当前用户专辑列表
	 * @return
	 */
	@HwLogin
	@Operation(summary = "获取当前用户全部专辑列表")
	@GetMapping("findUserAllAlbumList")
	public Result findUserAllAlbumList() {
		//TODO 需要用户id，临时添加固定值，后续完善
		Long userId = 1L;
		List<AlbumInfo> list = albumInfoService.findUserAllAlbumList(userId);
		return Result.ok(list);
	}

	/**
	 * 根据条件查询专辑列表
	 * @param page
	 * @param limit
	 * @param albumInfoQuery
	 * @return
	 */
	@Operation(summary = "获取当前用户专辑分页列表")
	@PostMapping("findUserAlbumPage/{page}/{limit}")
	public Result findUserAlbumPage(@PathVariable Long page,
									@PathVariable Long limit,
									@RequestBody AlbumInfoQuery albumInfoQuery) {
		//TODO userId
		albumInfoQuery.setUserId(1L);
		//创建page对象，设置当前页和每页记录数
		Page<AlbumListVo> pageModel = new Page<>(page,limit);
		//调用service方法
		IPage<AlbumListVo> pageData = albumInfoService.findUserAlbumPage(pageModel,albumInfoQuery);
		return Result.ok(pageData);
	}

	/**
	 * 根据专辑id删除专辑数据
	 * @param id
	 * @return
	 */
	@Operation(summary = "删除专辑信息")
	@DeleteMapping("removeAlbumInfo/{id}")
	public Result removeAlbumInfoById(@PathVariable Long id) {
		albumInfoService.removeAlbumInfoById(id);
		return Result.ok();
	}

	//修改- 根据专辑id查询专辑信息
	@Operation(summary = "获取专辑信息")
	@GetMapping("getAlbumInfo/{id}")
	public Result<AlbumInfo> getAlbumInfoById(@PathVariable Long id) {
		// 调用服务层方法
		AlbumInfo albumInfo = albumInfoService.getAlbumInfoById(id);
		return Result.ok(albumInfo);
	}


	/**
	 * 修改专辑信息
	 * @param id
	 * @param albumInfoVo
	 * @return
	 */  //api/album/albumInfo/updateAlbumInfo/1576
	@Operation(summary = "修改专辑")
	@PutMapping("/updateAlbumInfo/{id}")
	public Result updateById(@PathVariable Long id,
							 @RequestBody @Validated AlbumInfoVo albumInfoVo){
		//	调用服务层方法
		albumInfoService.updateAlbumInfo(id,albumInfoVo);
		return Result.ok();
	}

	/**
	 * 根据专辑Id 获取到专辑属性列表
	 * @param albumId
	 * @return
	 */
	@Operation(summary = "获取专辑属性值列表")
	@GetMapping("findAlbumAttributeValue/{albumId}")
	public Result <List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable Long albumId){
		// 获取到专辑属性集合
		List<AlbumAttributeValue> albumAttributeValueList =
				albumInfoService.findAlbumAttributeValueByAlbumId(albumId);
				return Result.ok(albumAttributeValueList);
	}

	//根据专辑id获取四个统计数据
	@GetMapping("getAlbumStat/{albumId}")
	public Result getAlbumStat(@PathVariable Long albumId){
		Map<String,Object> map =albumInfoService.getAlbumStat(albumId);
		return Result.ok(map);
	}

	/**
	 * 根据专辑Id 获取到统计信息
	 * @param albumId
	 * @return
	 */
	@Operation(summary = "获取到专辑统计信息")
	@GetMapping("/getAlbumStatVo/{albumId}")
	public Result getAlbumStatVo(@PathVariable Long albumId){
		//	获取服务层方法
		AlbumStatVo albumStatVo = this.albumInfoService.getAlbumStatVoByAlbumId(albumId);
		return Result.ok(albumStatVo);
	}

}

