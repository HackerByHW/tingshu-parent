package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.cache.HwCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.sound.midi.Track;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //添加专辑
    @Transactional
    @Override
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        //1 album_info表添加专辑基本信息
        AlbumInfo albumInfo = new AlbumInfo();
        // albumInfoVo -- albumInfo
        BeanUtils.copyProperties(albumInfoVo, albumInfo);

        //用户id
        albumInfo.setUserId(userId);
        //	设置专辑审核状态为：通过
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        //不是免费的专辑，可以试看前5集
        if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())) {
            albumInfo.setTracksForFree(5);
        }
        //调用mapper方法添加专辑基本信息
        albumInfoMapper.insert(albumInfo);

        //2 album_attribute_value添加专辑选择属性数据
        //从前端传递albumInfoVo获取属性数据
        List<AlbumAttributeValueVo> albumAttributeValueVoList =
                albumInfoVo.getAlbumAttributeValueVoList();
        //把属性集合遍历
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
            albumAttributeValueVoList.stream().forEach(albumAttributeValueVo -> {
                //得到每个属性数据，添加表
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                // albumAttributeValueVo -- albumAttributeValue
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                //设置专辑id
                albumAttributeValue.setAlbumId(albumInfo.getId());

                albumAttributeValueMapper.insert(albumAttributeValue);
            });
        }

        //3 album_stat添加四条统计数据，初始值0
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY);
        this.saveAlbumStat(albumInfo.getId(), "0402");
        this.saveAlbumStat(albumInfo.getId(), "0403");
        this.saveAlbumStat(albumInfo.getId(), "0404");

        //4 专辑上架，发送mq消息
        //判断不是私密专辑
        String isOpen = albumInfo.getIsOpen();
        if ("1".equals(isOpen)) {
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfo.getUserId());
        }
    }

    //根据条件查询专辑列表
    @Override
    public IPage<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> pageModel, AlbumInfoQuery albumInfoQuery) {
        return albumInfoMapper.selectUserAlbumPage(pageModel, albumInfoQuery);
    }

    //删除专辑
    @Transactional
    @Override
    public void removeAlbumInfoById(Long id) {
        //1 判断：如果专辑里面存在声音，不能删除
        //根据专辑id查询声音表track_info
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, id);
        //SELECT COUNT(*) FROM track_info WHERE album_id=1603
        Long count = trackInfoMapper.selectCount(wrapper);
        if (count > 0) {//专辑有声音
            throw new GuiguException(400, "该专辑下存在未删除声音！");
        }

        //2 根据专辑id，删除album_info数据
        albumInfoMapper.deleteById(id);

        //3 根据专辑id，删除album_attribute_value数据
        LambdaQueryWrapper<AlbumAttributeValue> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(AlbumAttributeValue::getAlbumId, id);
        albumAttributeValueMapper.delete(wrapper1);

        //4 根据专辑id，删除album_stat数据
        LambdaQueryWrapper<AlbumStat> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(AlbumStat::getAlbumId, id);
        albumStatMapper.delete(wrapper2);

        //5 发送mq消息，专辑下架
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
    }

    //////////////////////////////////


//	private AlbumInfo getAlbumInfoData(Long id) {
//		//根据专辑id查询
//		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
//		//获取专辑属性信息
//		LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
//		wrapper.eq(AlbumAttributeValue::getAlbumId,id);
//		List<AlbumAttributeValue> attributeValueList = albumAttributeValueMapper.selectList(wrapper);
//		albumInfo.setAlbumAttributeValueVoList(attributeValueList);
//		return albumInfo;
//	}

    //修改- 根据专辑id查询专辑信息
//    @Override
//public AlbumInfo getAlbumInfoById(Long id) {
//   //生成数据key，包含专辑id
//   String albumKey = RedisConstant.ALBUM_INFO_PREFIX+ id;
//   //1 查询redis缓存
//   AlbumInfo albumInfo = (AlbumInfo)redisTemplate.opsForValue().get(albumKey);
//
//   //2 如果redis查询不到数据，查询mysql，把mysql数据放到redis里面
//   if(albumInfo == null) {
//      // 查询mysql添加分布式锁，使用Redisson实现
//      try {//获取锁对象
//         String albumLockKey = RedisConstant.ALBUM_INFO_PREFIX+id+ ":lock";
//         RLock rLock = redissonClient.getLock(albumLockKey);
//         //加锁
//         // rLock.lock(); 加锁，默认值
//         //rLock.lock(30,TimeUnit.SECONDS); 自己设置过期时间
//         // trylock三个参数：第一个等待时间，第二个参数过期时间
//         boolean tryLock = rLock.tryLock(10, 30, TimeUnit.SECONDS);
//         if(tryLock) { //加锁成功
//            try {
//               //查询mysql，
//               AlbumInfo albumInfoData = this.getAlbumInfoData(id);
//               if(albumInfoData == null) {
//                  albumInfoData = new AlbumInfo();
//                  //把mysql查询数据放到redis里面
//                  redisTemplate.opsForValue().set(albumKey,albumInfoData,
//                        RedisConstant.ALBUM_TIMEOUT,TimeUnit.SECONDS);
//               }
//               //把mysql查询数据放到redis里面
//               redisTemplate.opsForValue().set(albumKey,albumInfoData,
//                     RedisConstant.ALBUM_TIMEOUT,TimeUnit.SECONDS);
//
//               return albumInfoData;
//
//            }finally {
//               //解锁
//               rLock.unlock();
//            }
//         } else {
//            // 没有获取到锁的线程，自旋
//            return getAlbumInfoById(id);
//         }
//      } catch (InterruptedException e) {
//         throw new RuntimeException(e);
//      }
//   } else {
//      //3 如果redis查询数据，直接返回
//      return albumInfo;
//   }
//}

    //修改- 根据专辑id查询专辑信息
	@HwCache(prefix="album:Info")
	@Override
	public AlbumInfo getAlbumInfoById(Long id) {
		//根据专辑id查询
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		//获取专辑属性信息
		LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AlbumAttributeValue::getAlbumId,id);
		List<AlbumAttributeValue> attributeValueList = albumAttributeValueMapper.selectList(wrapper);
		albumInfo.setAlbumAttributeValueVoList(attributeValueList);
		return albumInfo;
	}


    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    //修改专辑信息
    @Override
    public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
        //1 修改专辑基本信息
        // 根据专辑id查询专辑数据
        AlbumInfo albumInfo = albumInfoMapper.selectById(id);
        // 设置修改值
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        //调用方法修改
        albumInfoMapper.updateById(albumInfo);

        //2 修改专辑属性信息
        // 根据专辑id删除属性数据
        LambdaQueryWrapper<AlbumAttributeValue> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(AlbumAttributeValue::getAlbumId, id);
        albumAttributeValueMapper.delete(wrapper1);

        // 再添加最新属性数据
        List<AlbumAttributeValueVo> albumAttributeValueVoList =
                albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
//			albumAttributeValueVoList.stream().forEach(albumAttributeValueVo -> {
//
//				AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
//				BeanUtils.copyProperties(albumAttributeValueVo,albumAttributeValue);
//				//	赋值专辑属性Id
//				albumAttributeValue.setAlbumId(id);
//				albumAttributeValueMapper.insert(albumAttributeValue);
//			});
            // List<AlbumAttributeValueVo> -- List<AlbumAttributeValue>
            List<AlbumAttributeValue> list =
                    albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                        AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                        BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                        //	赋值专辑属性Id
                        albumAttributeValue.setAlbumId(id);
                        return albumAttributeValue;
                    }).collect(Collectors.toList());
            //批量操作
            albumAttributeValueService.saveBatch(list);
        }
    }

    //获取当前用户专辑列表
    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {

        Page<AlbumInfo> albumInfoPage = new Page<>(1, 100);

        LambdaQueryWrapper<AlbumInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle);
        wrapper.eq(AlbumInfo::getUserId, userId);
        wrapper.orderByDesc(AlbumInfo::getId);

        IPage<AlbumInfo> pageModel = albumInfoMapper.selectPage(albumInfoPage, wrapper);
        List<AlbumInfo> list = pageModel.getRecords();
        //List<AlbumInfo> list = albumInfoMapper.selectList(wrapper);
        return list;
    }

    /**
     * 初始化统计数据
     *
     * @param albumId
     * @param statType
     */
    private void saveAlbumStat(Long albumId, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatMapper.insert(albumStat);
    }

    @Override
    public List<AlbumAttributeValue> findAlbumAttributeValueByAlbumId(Long albumId) {
        LambdaQueryWrapper<AlbumAttributeValue> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper.selectList(Wrapper);
        return albumAttributeValues;

    }

    @Override
    public Map<String, Object> getAlbumStat(Long albumId) {
        Map<String, Object> map = albumStatMapper.getAlbumStat(albumId);
        return map;
    }

    @Override
    public AlbumStatVo getAlbumStatVoByAlbumId(Long albumId) {
        AlbumStatVo albumStatVo = albumStatMapper.getAlbumStatVoByAlbumId(albumId);
        return albumStatVo;
    }
}
