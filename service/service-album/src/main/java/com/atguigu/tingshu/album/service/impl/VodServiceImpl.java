package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    //上传声音
    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {

        VodUploadClient client =
                new VodUploadClient(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        //创建request，设置上传文件
        VodUploadRequest request = new VodUploadRequest();
        /*
         *  方法参数：本地音频文件路径
         *
         * 把本地上传文件，放到服务器某个目录里面，作为临时目录
         */
        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
        request.setMediaFilePath(tempPath);

        request.setProcedure(vodConstantProperties.getProcedure());
        try {
            VodUploadResponse response = client.upload("ap-guangzhou", request);
            //腾讯云生成声音播放地址
            String mediaUrl = response.getMediaUrl();
            //文件在腾讯云存储唯一标识
            String fileId = response.getFileId();

            HashMap<String, Object> map = new HashMap<>();
            map.put("mediaFileId",fileId);
            map.put("mediaUrl",mediaUrl);
            //  返回map 数据
            return map;

        } catch (Exception e) {
            // 业务方进行异常处理

        }
        return null;
    }
    @SneakyThrows
    @Override
    public TrackMediaInfoVo getTrackInfoTenx(String mediaFileId) {
        //  初始化认证对象
        Credential cred =
                new Credential(vodConstantProperties.getSecretId(),
                        vodConstantProperties.getSecretKey());
        // 实例化要请求产品的client对象,clientProfile是可选的
        VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
        // 实例化一个请求对象,每个接口都会对应一个request对象
        DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
        //  设置当前fileIds
        req.setFileIds(new String[]{mediaFileId});
        // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
        DescribeMediaInfosResponse response = client.DescribeMediaInfos(req);
        //  判断对象不为空
        if (response.getMediaInfoSet().length>0){
            //  获取到
            MediaInfo mediaInfo = response.getMediaInfoSet()[0];
            //  创建流媒体信息对象
            TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
            trackMediaInfoVo.setDuration(mediaInfo.getMetaData().getDuration());
            trackMediaInfoVo.setSize(mediaInfo.getMetaData().getSize());
            trackMediaInfoVo.setMediaUrl(mediaInfo.getBasicInfo().getMediaUrl());
            trackMediaInfoVo.setType(mediaInfo.getBasicInfo().getType());
            //  返回数据
            return trackMediaInfoVo;
        }
        return null;
    }

    @SneakyThrows
    @Override
    public void removeTrackInfo(String mediaFileId) {
        Credential cred =
                new Credential(vodConstantProperties.getSecretId(),
                        vodConstantProperties.getSecretKey());
        // 实例化要请求产品的client对象,clientProfile是可选的
        VodClient client = new VodClient(cred, "");
        // 实例化一个请求对象,每个接口都会对应一个request对象
        DeleteMediaRequest req = new DeleteMediaRequest();
        req.setFileId(mediaFileId);
        // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
        DeleteMediaResponse response = client.DeleteMedia(req);
        // 输出json格式的字符串回包
        log.info("声音删除返回结课: {}", JSON.toJSONString(response));
    }
}


