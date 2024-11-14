package com.atguigu.tingshu.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.UserListenProcessFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private UserListenProcessFeignClient userListenProcessFeignClient;

    //根据专辑Id 获取详情数据
    @Override
    public Map<String, Object> getItem(Long albumId) {

        // 创建map集合对象
        Map<String, Object> result = new HashMap<>();

        //1 根据专辑id获取专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑不存在");

            result.put("albumInfo", albumInfo);
            return albumInfo;
        });

        //2 根据专辑id获取四个统计数据
        CompletableFuture<Void> albumStatVoCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<AlbumStatVo> statVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
            AlbumStatVo albumStatVo = statVoResult.getData();
            result.put("albumStatVo", albumStatVo);
        });

        //3 根据三级分类id获取一级和二级
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //获取三级分类id
            Long category3Id = albumInfo.getCategory3Id();
            Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(category3Id);
            BaseCategoryView baseCategoryView = categoryViewResult.getData();
            result.put("baseCategoryView", baseCategoryView);
        });

        //4 根据用户id获取用户信息
        CompletableFuture<Void> userInfoVoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //获取用户id
            Long userId = albumInfo.getUserId();
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            result.put("announcer", userInfoVo);
        });

        //5 等待所有线程完成，合并
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                albumStatVoCompletableFuture,
                baseCategoryViewCompletableFuture,
                userInfoVoCompletableFuture
        ).join();

        return result;
    }
}
