package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;

	@Override
	public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
		Query query = Query.query(Criteria.where("userId").is(userId).and("trackId") .is(trackId));
		String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,userId);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,collectionName);
		//判断
		if(userListenProcess != null){
			return userListenProcess.getBreakSecond();
		}

		return new BigDecimal("0");
	}

	//保存声音播放进度
	@Override
	public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
		//1 根据userId + trackId查询
		Query query = Query.query(Criteria.where("userId").is(userId).and("trackId") .is(userListenProcessVo.getTrackId()));
		String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,userId);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,collectionName);
		//2 如果可以查询到数据，有播放进度，进行更新
		if (userListenProcess != null){//更新
			userListenProcess.setUpdateTime(new Date());
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			mongoTemplate.save(userListenProcess,collectionName);

		}else {
			//3 如果不能查询到数据，第一次播放，进行保存
			userListenProcess = new UserListenProcess();

			BeanUtils.copyProperties(userListenProcessVo,userListenProcess);
			//	设置Id
			userListenProcess.setId(ObjectId.get().toString());
			// 设置用户Id
			userListenProcess.setUserId(userId);
			// 设置是否显示
			userListenProcess.setIsShow(1);
			// 创建时间
			userListenProcess.setCreateTime(new Date());
			// 更新时间
			userListenProcess.setUpdateTime(new Date());
			mongoTemplate.save(userListenProcess,collectionName);
		}

		//4 更新声音播放量
		// 同一个用户对同一个声音，24小时以内计算一次播放量
		// Redis
		// setnx + 过期时间
		// redis的key是用户id+声音id
		String key = "playnum_" + userId + userListenProcessVo.getTrackId();
		Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(key,"1",24, TimeUnit.HOURS);


		if (ifAbsent){//第一次
			//更新播放量
			// RabbitMQ 异步方式更新播放量
			//封装消息内容到实体类
			TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
			//当前消息标识
			trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-",""));
			trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
			trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
			trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
			trackStatMqVo.setCount(1);
			//发送消息
			rabbitService.sendMessage(MqConst.EXCHANGE_TRACK,MqConst.ROUTING_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));

		}
	}

}
