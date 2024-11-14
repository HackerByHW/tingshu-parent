package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private VodService vodService;

	@Autowired
	private AlbumInfoService albumInfoService;
	@Autowired
	private UserInfoFeignClient userInfoFeignClient;



	//保存声音
	@Transactional
	@Override
	public void saveTrackInfo(TrackInfoVo trackInfoVo, long userId) {
		//#1 添加声音基本信息 track_info
		//## 第一部分 -- 声音基本信息：专辑id、用户id、标题、简介、封面等等
		TrackInfo trackInfo = new TrackInfo();
		// trackInfoVo -- trackInfo
		BeanUtils.copyProperties(trackInfoVo,trackInfo);
		//其他值
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
		trackInfo.setUserId(userId);

		//## 第二部分数据 -- 到腾讯云找到，声音文件信息，大小、播放时长、类型
		//声音文件在腾讯云存储唯一标识
		String mediaFileId = trackInfoVo.getMediaFileId();
		//根据mediaFileId调用腾讯云对应接口获取声音文件信息
		TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackInfoTenx(mediaFileId);
		// 赋值声音
		trackInfo.setMediaSize(trackMediaInfoVo.getSize());
		trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
		trackInfo.setMediaDuration(trackMediaInfoVo.getDuration());
		trackInfo.setMediaType(trackMediaInfoVo.getType());

		//## 第三部分数据 -- order_num，声音排序字段，每次添加声音，值+1
		//		# track_info 根据专辑id获取专辑里面声音最大order_num
		//		select order_num from track_info info where info.album_id=1
		//		order by order_num desc limit 1
		LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(TrackInfo::getAlbumId,trackInfoVo.getAlbumId());
		wrapper.orderByDesc(TrackInfo::getOrderNum);
		wrapper.last(" limit 1");
		TrackInfo preTrackInfo = trackInfoMapper.selectOne(wrapper);
		Integer newOrder_num = 1;
		if(preTrackInfo != null) {
			newOrder_num = preTrackInfo.getOrderNum()+1;
		}
		trackInfo.setOrderNum(newOrder_num);
		//调用方法添加 track_info
		trackInfoMapper.insert(trackInfo);

		//#2 track_stat
		//## -- 每个声音有四个统计数据，四个统计数据初始化0
		this.saveTrackStat(trackInfo.getId(),SystemConstant.TRACK_STAT_PLAY);
		this.saveTrackStat(trackInfo.getId(),"0702");
		this.saveTrackStat(trackInfo.getId(),"0703");
		this.saveTrackStat(trackInfo.getId(),"0704");

		//#3 album_info
		//## -- 修改操作，把声音所在专辑，include_track_count包含声音数量+1
		//根据专辑id查询专辑数据
		//获取声音数量值 + 1
		//调用方法修改
		AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
		int includeTrackCount = albumInfo.getIncludeTrackCount() + 1;
		albumInfo.setIncludeTrackCount(includeTrackCount);
		albumInfoService.updateById(albumInfo);
	}
	@Autowired
	private TrackStatMapper trackStatMapper;
	/**
	 * 初始化统计数量
	 * @param trackId
	 * @param trackType
	 */
	private void saveTrackStat (Long trackId, String trackType){
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(trackType);
		trackStat.setStatNum(0);
		this.trackStatMapper.insert(trackStat);
	}

	@Override
	public IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.findUserTrackPage(trackListVoPage,trackInfoQuery);
	}

	//删除声音
	@Transactional
	@Override
	public void removeTrackInfo(Long id) {
		//# 1 把声音所在的专辑里面包含声音数量值-1 album_info
		// 根据专辑id查询声音所在的专辑数据
		// 根据声音id获取专辑id
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		Long albumId = trackInfo.getAlbumId();

		// 根据专辑id获取专辑数据
		AlbumInfo albumInfo = albumInfoService.getById(albumId);
		// 获取包含声音数量 -1
		Integer newInclueCount = albumInfo.getIncludeTrackCount()-1;
		albumInfo.setIncludeTrackCount(newInclueCount);

		//调用方法修改
		albumInfoService.updateById(albumInfo);

		//# 2 根据声音id删除track_info表数据
		trackInfoMapper.deleteById(id);

		//# 3 根据声音id删除track_stat表数据
		LambdaQueryWrapper<TrackStat> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(TrackStat::getTrackId,id);
		trackStatMapper.delete(wrapper);

		//# 4 删除腾讯云声音文件
		// 调用腾讯云接口实现
		vodService.removeTrackInfo(trackInfo.getMediaFileId());
	}

	@Override
	public IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageParam, Long albumId, Long userId) {
		IPage<AlbumTrackListVo> pageModel = trackInfoMapper.findAlbumTrackPage(pageParam,albumId,userId);
		List<AlbumTrackListVo> trackListVoList = pageModel.getRecords();
		//根据专辑id获取专辑数据
		AlbumInfo albumInfo = albumInfoService.getById(albumId);
		Assert.notNull(albumInfo,"专辑对象不能为空");
		//0101-免费、0102-vip免费、0103-付费
		String payType = albumInfo.getPayType();
		//2判断用户是否登录
		//3 如果用户没有登录，看免费专辑、 vip免费和付费可以观看试看声音
		if(userId == null){
			//免费专辑不需要处理
			// vip免费和付费找到不是试看部分 0102-vip免费、0103-付费
			if(!"0101".equals(payType)){
				//找到不是试看部分
				//获取专辑试看集数
				Integer tracksForFree = albumInfo.getTracksForFree();
				// 条件 order_num > 试看集数
				List<AlbumTrackListVo> albumTrackListVoList = trackListVoList.stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > tracksForFree).collect(Collectors.toList());
				//albumTrackListVoList里面每个对象isShowPaidMark=true
				if (!CollectionUtils.isEmpty(albumTrackListVoList)){
					albumTrackListVoList.forEach(albumTrackListVo -> {
						// 显示付费通知
						albumTrackListVo.setIsShowPaidMark(true);
					});
				}
			}
		} else {
			//4 如果用户登录状态，判断用户是否是vip，如果是vip，vip免费可以观看
			//付费专辑：用户登录了付费，用户vip也要付费
			// 如果用户购买专辑或者声音，可以观看
			// -- 如果用户购买整个专辑，专辑里面所有声音都可以观看
			// -- 如果用户购买专辑里面某些声音，购买的声音可以观看

			// 声明变量是否需要付费，默认不需要付费
			boolean isNeedPaid = false;
			// 如果专辑是vip免费的
			if("0102".equals(payType)) {
				//判断用户是否是vip，如果是vip，vip免费可以观看
				//远程调用：根据用户id获取用户信息
				Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
				UserInfoVo userInfoVo = userInfoVoResult.getData();
				Assert.notNull(userInfoVo,"用户信息为空");
				//获取是否是vip
				Integer isVip = userInfoVo.getIsVip();
				//如果用户不是vip，付费
				if(isVip.intValue() == 0) {
					isNeedPaid = true;
				}

				Date vipExpireTime = userInfoVo.getVipExpireTime();
				// 2024-10-11
				// 当前：2024-11-09

				//如果用户是vip，但是过期
				if (isVip.intValue() == 1 && vipExpireTime.before(new Date())){
					isNeedPaid = true;
				}
			} else if("0103".equals(payType)) {//付费
				isNeedPaid = true;
			}
			// 如果专辑付费,统一处理
			if (isNeedPaid){
				//用户是否购买专辑或者声音
				//获取需要付费的声音列表（去除试看声音）
				List<AlbumTrackListVo> albumTrackListVoList = trackListVoList.stream()
						.filter(albumTrackListVo -> albumTrackListVo.getOrderNum().intValue() > albumInfo.getTracksForFree())
						.collect(Collectors.toList());
				//albumTrackListVoList里面声音，查询判断是否购买过
				if (!CollectionUtils.isEmpty(albumTrackListVoList)){
					//声音id列表获取到
					List<Long> trackIdList =
							albumTrackListVoList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
					//远程调用：根据userid 、专辑id、声音id查询，用户是否购买专辑 或者是否购买声音
					Result<Map<Long, Integer>> result = userInfoFeignClient.userIsPaidTrack(userId, albumId, trackIdList);
					// 返回map集合
					// map的key是声音id  value：是否购买过 1买过，0 没有购买
					Map<Long, Integer> map = result.getData();
					albumTrackListVoList.forEach(albumTrackListVo -> {
						boolean isBuy =map.get(albumTrackListVo.getTrackId()) == 1 ? false : true ;
						albumTrackListVo.setIsShowPaidMark(isBuy);
					});
				}

			}

			}
		return pageModel;
	}

	//修改声音
	@Override
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
		//1 根据声音id获取声音数据
		TrackInfo trackInfo = trackInfoMapper.selectById(id);

		//获取声音文件fieldid
		String mediaFileId_database = trackInfo.getMediaFileId();

		BeanUtils.copyProperties(trackInfoVo,trackInfo);

		//比较数据库里面mediaFileId 和 页面传递 mediaFileId是否相同
		String mediaFileId_page = trackInfoVo.getMediaFileId();
		if(!mediaFileId_database.equals(mediaFileId_page)) {
			//获取最新声音文件数据
			TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackInfoTenx(mediaFileId_page);

			trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
			trackInfo.setMediaType(trackMediaInfoVo.getType());
			trackInfo.setMediaDuration(trackMediaInfoVo.getDuration());
			trackInfo.setMediaSize(trackMediaInfoVo.getSize());

			// 删除云点播声音
			vodService.removeTrackInfo(mediaFileId_database);
		}

		trackInfoMapper.updateById(trackInfo);
	}


}
