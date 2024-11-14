package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Mapper
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {
    //根据一级分类Id 查询分类属性数据
    List<BaseAttribute> findAttributeByCategory1Id(@Param("category1Id") Long category1Id);

}
