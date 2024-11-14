package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;


	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;



	@Override
	public List<JSONObject> getBaseCategoryList() {
		// 创建集合对象，放最终数据
		List<JSONObject> finalList = new ArrayList<>();

		//1 查询所有分类（包含一级、二级、三级，使用view查询），返回list集合
		List<BaseCategoryView> list = baseCategoryViewMapper.selectList(null);

		//2 把查询所有分类list集合封装成要求的数据格式
		//封装一级分类，根据一级分类id进行分组操作
		// map 的 key是分组字段 一级分类id
		// map 的 value是 每组数据集合
		Map<Long, List<BaseCategoryView>> map =
				list.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

		//遍历map集合，封装一级分类数据
		//基础方法
//		Set<Long> keys = map.keySet();
//		for (Long key:keys){
//			List<BaseCategoryView> value = map.get(key);
//		}

		//方法二 遍历map集合
		for (Map.Entry<Long,List<BaseCategoryView>> entry : map.entrySet()){
			Long category1Id = entry.getKey();
			List<BaseCategoryView> category1List = entry.getValue();
			//封装每个一级分类
			JSONObject category1 = new JSONObject();
			category1.put("categoryId",category1Id);
			category1.put("categoryName",category1List.get(0).getCategory1Name());

			//找到每个一级分类里面所有二级分类，进行封装
			//根据二级分类id进行分组
			//key : 二级分类id
			// value: 每个二级里面所有数据集合
			Map<Long, List<BaseCategoryView>> map2 =
					category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

			//每个一级所有二级集合
			List<JSONObject> category2Child = new ArrayList<>();

			//遍历map集合
			for (Map.Entry<Long,List<BaseCategoryView>> entry2 : map2.entrySet()){
				Long category2Id = entry2.getKey();
				List<BaseCategoryView> category2List = entry2.getValue();

				JSONObject category2 = new JSONObject();
				category2.put("categoryId",category2Id);
				category2.put("categoryName",category2List.get(0).getCategory2Name());

				//封装每个二级三级数据  category2List
				// List<BaseCategoryView> --  List<JSONObject>

				List<JSONObject> category3List = category2List.stream().map(baseCategoryView -> {
					JSONObject category3 = new JSONObject();
					category3.put("categoryId",baseCategoryView.getCategory3Id());
					category3.put("categoryName",baseCategoryView.getCategory3Name());
					return category3;
				}).collect(Collectors.toList());

				category2.put("categoryChild",category3List);


				category2Child.add(category2);

			}

				//把每个二级分类数据放到一级分类里面
				category1.put("categoryChild",category2Child);


			//把每个封装好的一级分类数据放到final集合里的finalList集合中
			finalList.add(category1);
		}


		return finalList;
	}

	//根据三级分类Id 查询分类数据
	@Override
	public BaseCategoryView getCategoryView(Long category3Id) {
		BaseCategoryView baseCategoryView = baseCategoryViewMapper.selectById(category3Id);
		return baseCategoryView;
	}

	@Override
	public List<BaseAttribute> findAttributeByCategory1Id(Long category1Id) {

		return List.of();
	}
	@Override
	public List<BaseCategory3> findTopBaseCategory3ByCategory1Id(Long category1Id) {
		//    # 方法二：根据一级分类id获取下面置顶的前7个三级分类数据
		//# 第一步操作：根据一级分类id查询下面所有二级分类id
		//    select id from base_category2 bc2 where bc2.category1_id=1
		//# 101 102 103
		LambdaQueryWrapper<BaseCategory2> wrapperBaseCategory2 = new LambdaQueryWrapper<>();
		wrapperBaseCategory2.eq(BaseCategory2::getCategory1Id,category1Id);
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(wrapperBaseCategory2);
		//获取baseCategory2List所有二级分类id值
		List<Long> category2IdList =
				baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
		//# 第二步操作：根据查询出来二级分类id查询下面三级分类数据
		//## 条件：is_top=1  前7条数据 limit 7
		//    select id,name from base_category3 bc3
		//    where bc3.category2_id in (101,102,103)
		//    and bc3.is_top=1 limit 7
		LambdaQueryWrapper<BaseCategory3> wrapperBaseCategory3 = new LambdaQueryWrapper<>();
		// (1001,1002,1003)
		wrapperBaseCategory3.in(BaseCategory3::getCategory2Id,category2IdList);
		//is_top=1 置顶
		wrapperBaseCategory3.eq(BaseCategory3::getIsTop,1);
		// limit 7 前7条三级分类数据
		wrapperBaseCategory3.last(" limit 7");
		List<BaseCategory3> category3List = baseCategory3Mapper.selectList(wrapperBaseCategory3);
		return category3List;
	}

	@Override
	public JSONObject getAllCategoryListByCategory1Id(Long category1Id) {
		//1 封装一级分类
		// 根据一级分类id获取一级分类数据，
		BaseCategory1 baseCategory1 = baseCategory1Mapper.selectById(category1Id);
		//封装到JSONObject
		JSONObject category1 = new JSONObject();
		category1.put("categoryId",category1Id);
		category1.put("categoryName",baseCategory1.getName());
		//2 根据一级分类id获取所有二级和三级数据
		LambdaQueryWrapper<BaseCategoryView> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(BaseCategoryView::getCategory1Id,category1Id);
		List<BaseCategoryView> categoryViewList = baseCategoryViewMapper.selectList(wrapper);
		//3 向一级分类里面封装所有二级分类
		//根据二级分类id进行分组，map集合
		//map的key是二级分类id，value每组二级分类数据集合
		Map<Long, List<BaseCategoryView>> map =
				categoryViewList.stream()
						.collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
		//遍历map集合，封装二级分类
		List<JSONObject> category2Child = new ArrayList<>();
		for(Map.Entry<Long, List<BaseCategoryView>> entry2 : map.entrySet()){
			//封装二级分类
			Long category2Id = entry2.getKey();
			List<BaseCategoryView> category2List = entry2.getValue();
			JSONObject category2 = new JSONObject();
			category2.put("categoryId",category2Id);
			category2.put("categoryName",category2List.get(0).getCategory2Name());

			//封装三级分类
			// List<BaseCategoryView> -- List<JSONObject>
			List<JSONObject> category3Child = new ArrayList<>();
			category2List.stream().forEach(category3View -> {
				JSONObject category3 = new JSONObject();
				category3.put("categoryId", category3View.getCategory3Id());
				category3.put("categoryName", category3View.getCategory3Name());
				category3Child.add(category3);
			});

			//把三级分类集合放到每个二级分类里面
			category2.put("categoryChild",category3Child);
			category2Child.add(category2);
		}
		//把二级分类放到一级分类里面
		category1.put("categoryChild",category2Child);

		return category1;
	}
}
