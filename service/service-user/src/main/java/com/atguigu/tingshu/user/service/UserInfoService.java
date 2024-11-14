package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    UserInfoVo getUserInfo(Long userId);

    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> trackIdList);

    Boolean isPaidAlbum(Long albumId, Long userId);
}
