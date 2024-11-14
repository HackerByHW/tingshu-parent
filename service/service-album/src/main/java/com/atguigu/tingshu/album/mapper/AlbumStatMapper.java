package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {

    Map<String,Object> getAlbumStat(@Param("albumId") Long albumId);


    AlbumStatVo getAlbumStatVoByAlbumId(Long albumId);
}
