package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;


    @Override
    public UserInfoVo getUserInfo(Long userId) {

        UserInfo userInfo = userInfoMapper.selectById(userId);
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return userInfoVo;
    }

    //判断用户是否购买声音列表
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> trackIdList) {
        LambdaQueryWrapper<UserPaidAlbum> wrapperUserPaidAlbum = new LambdaQueryWrapper<>();
        wrapperUserPaidAlbum.eq(UserPaidAlbum::getUserId, userId);
        wrapperUserPaidAlbum.eq(UserPaidAlbum::getAlbumId, albumId);
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapperUserPaidAlbum);
        //判断userPaidAlbum不为空，用户购买这个专辑
        if (userPaidAlbum != null) {
            Map<Long, Integer> map = new HashMap<>();
            //trackIdList 遍历
            trackIdList.forEach(trackId -> {
                map.put(trackId, 1);
            });
            return map;
        } else {//没有购买过专辑
            //查询用户购买过哪些声音
            //SELECT * FROM user_paid_track t WHERE t.user_id=1 AND track_id IN (100,101,102)
            LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getUserId, userId)
                    .in(UserPaidTrack::getTrackId, trackIdList);
            List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackLambdaQueryWrapper);
            // List<Long> trackIdList 所有声音   1 2 3 4 5
            // userPaidTrackList 用户购买过声音   1 2 3
            Map<Long, Integer> map = new HashMap<>();
            trackIdList.forEach(trackId -> {
                if (userPaidTrackList.contains(trackId)) {
                    map.put(trackId, 1);
                } else {
                    map.put(trackId, 0);
                }

            });
            return map;
        }
    }

    @Override
    public Boolean isPaidAlbum(Long albumId, Long userId) {
        // 根据用户Id 与专辑Id 查询是否有记录
        Long count = userPaidAlbumMapper.selectCount(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId, userId).eq(UserPaidAlbum::getAlbumId, albumId));
        if (count > 0){
            return true;
        }
        return false;
    }
}