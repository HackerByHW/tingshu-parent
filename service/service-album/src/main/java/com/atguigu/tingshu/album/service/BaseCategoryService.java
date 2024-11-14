package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {

    List<JSONObject> getBaseCategoryList();

    List<BaseAttribute> findAttributeByCategory1Id(Long category1Id);

    BaseCategoryView getCategoryView(Long category3Id);


    List<BaseCategory3> findTopBaseCategory3ByCategory1Id(Long category1Id);

    JSONObject getAllCategoryListByCategory1Id(Long category1Id);
}
