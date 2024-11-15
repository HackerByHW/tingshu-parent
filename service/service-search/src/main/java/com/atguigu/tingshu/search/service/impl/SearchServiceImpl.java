package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private AlbumIndexRepository albumIndexRepository;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    @Autowired
    private RedisTemplate redisTemplate;


    //专辑上架
    @Override
    public void upperAlbum(Long albumId) {
        //远程调用获取上架专辑需要所有数据，添加到es里面
        //1 远程调用获取数据，封装到albumInfoIndex
        // 根据专辑id获取专辑基本信息
        Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
        AlbumInfo albumInfo = albumInfoResult.getData();
        Assert.notNull(albumInfo, "专辑不能为空");

        // 根据用户id获取用户信息
        Long userId = albumInfo.getUserId();
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Assert.notNull(userInfoVo, "用户信息不能为空");

        // 根据三级分类id获取对应一级和二级
        Long category3Id = albumInfo.getCategory3Id();
        Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(category3Id);
        BaseCategoryView baseCategoryView = categoryViewResult.getData();
        Assert.notNull(baseCategoryView, "分类数据为空");

        //根据专辑id获取四个统计数据
        Result albumStatResult = albumInfoFeignClient.getAlbumStat(albumId);
        Map<String, Object> map = (Map<String, Object>) albumStatResult.getData();

        //根据专辑id获取属性数据
        Result<List<AlbumAttributeValue>> albumAttributeValueResult = albumInfoFeignClient.findAlbumAttributeValue(albumId);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueResult.getData();

        // List<AttributeValueIndex>    List<AlbumAttributeValue>
        //2 创建实体类对象,封装需要数据
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        BeanUtils.copyProperties(albumInfo, albumInfoIndex);

        //  赋值属性值信息
        if (!CollectionUtils.isEmpty(albumAttributeValueList)) {
            List<AttributeValueIndex> attributeValueIndexList =
                    albumAttributeValueList.stream().map(albumAttributeValue -> {
                        AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                        BeanUtils.copyProperties(albumAttributeValue, attributeValueIndex);
                        return attributeValueIndex;
                    }).collect(Collectors.toList());
            //  保存数据
            albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
        }
        //  赋值分类数据
        albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        albumInfoIndex.setCategory3Id(baseCategoryView.getCategory3Id());
        //  赋值主播名称
        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        //TODO 更新统计量与得分，默认随机，方便测试
        int num1 = new Random().nextInt(1000);
        int num2 = new Random().nextInt(100);
        int num3 = new Random().nextInt(50);
        int num4 = new Random().nextInt(300);

        albumInfoIndex.setPlayStatNum(num1);
        albumInfoIndex.setSubscribeStatNum(num2);
        albumInfoIndex.setBuyStatNum(num3);
        albumInfoIndex.setCommentStatNum(num4);
        double hotScore = num1 * 0.2 + num2 * 0.3 + num3 * 0.4 + num4 * 0.1;

        //  设置热度排名
        albumInfoIndex.setHotScore(hotScore);
        //3 调用方法实现添加到es
        albumIndexRepository.save(albumInfoIndex);

        // 初始化自动补全数据
//创建索引库，类型completion
        SuggestIndex suggestIndex = new SuggestIndex();
//id
        suggestIndex.setId(UUID.randomUUID().toString().replaceAll("-", ""));
//专辑title
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
//关键字中文
        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
//拼音
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));
//拼音简写
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())}));
        suggestIndexRepository.save(suggestIndex);
    }


    @Override
    public void lowerAlbum(Long albumId) {
        albumIndexRepository.deleteById(albumId);
    }


    /**
     * 根据关键词检索
     *
     * @param albumIndexQuery
     * @return
     */
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        //1 调用方法构建dsl语句
        SearchRequest searchRequest = this.buildQueryDsl(albumIndexQuery);
        SearchResponse<AlbumInfoIndex> response = null;
        try {
            //调用elasticsearchClient里面search实现es搜索
            response = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

            //把es查询出来结果封装成AlbumSearchResponseVo对象
            AlbumSearchResponseVo albumSearchResponseVo = this.parseSearchResult(response);

            //设置分页数据
            //每页记录数
            albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
            //当前页
            albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
            // 获取总页数  (总记录数 + 每页显示记录 -1) / 每页显示记录数
            Long totalpages = (albumSearchResponseVo.getTotal() + albumSearchResponseVo.getPageSize() - 1) / albumSearchResponseVo.getPageSize();

            //10条 每页5 2
//            Long total = albumSearchResponseVo.getTotal();
//            Integer pageSize = albumSearchResponseVo.getPageSize();
//            if(total % pageSize == 0){
//                Long totalPages = total / pageSize;
//            }
//            else {
//                Long totalPages = total / pageSize + 1;
//            }
            //返回
            return albumSearchResponseVo;
        } catch (IOException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    /**
     * 处理聚合结果集
     * @param response
     * @param suggestName
     * @return
     */
    private List<String> parseSearchResult(SearchResponse<SuggestIndex> response, String suggestName) {
        //  创建集合
        List<String> suggestList = new ArrayList<>();
        Map<String, List<Suggestion<SuggestIndex>>> groupBySuggestionListAggMap = response.suggest();
        groupBySuggestionListAggMap.get(suggestName).forEach(item -> {
            CompletionSuggest<SuggestIndex> completionSuggest =  item.completion();
            completionSuggest.options().forEach(it -> {
                SuggestIndex suggestIndex = it.source();
                suggestList.add(suggestIndex.getTitle());
            });
        });
        //  返回集合列表
        return suggestList;
    }

    //根据查询条件构建dsl语句
    private SearchRequest buildQueryDsl(AlbumIndexQuery albumIndexQuery) {
        //创建SearchRequest
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();

        //query -- bool
        //创建boolQuery
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // bool -- should -- match -- albumTitle : 小说
        // bool -- should -- match -- albumIntro : 小说
        //获取搜索关键字
        String keyword = albumIndexQuery.getKeyword();
        if (StringUtils.hasText(keyword)) {
            boolQuery.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            boolQuery.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));

            //高亮显示
            // query     ----
            // highlight -- fields
            // highlight -- pre_tags  post_tags
            requestBuilder.highlight(h -> h.fields("albumTitle", f -> f.preTags("<font color='red'>").postTags("</font>")));

        }

        // bool -- filter -- term -- category3Id : 12
        Long category1Id = albumIndexQuery.getCategory1Id();
        if (!StringUtils.isEmpty(category1Id)) {
            boolQuery.filter(f -> f.term(s -> s.field("category1Id").value(category1Id)));
        }
        //  二级分类Id
        Long category2Id = albumIndexQuery.getCategory2Id();
        if (!StringUtils.isEmpty(category2Id)) {
            boolQuery.filter(f -> f.term(s -> s.field("category2Id").value(category2Id)));
        }
        //  三级分类Id
        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }

        //排序
        String orderField = "";
        String sort = "";
        //排序字段根据前端传递的字段和排序方式进行处理
        // 1:desc   1代表排序字段   desc代表排序方式
        String order = albumIndexQuery.getOrder();
        // order == null
        if (!StringUtils.isEmpty(order)){


        //根据：分割，得到数组
        String[] split = order.split(":");
        //判断
        if (split != null && split.length == 2) {
            switch (split[0]) {
                case "1":
                    orderField = "hotScore";
                    break;
                case "2":
                    orderField = "playStatNum";
                    break;
                case "3":
                    orderField = "createTime";
                    break;
            }
            //排序方式 asc  desc
            sort = split[1];
        }

//        requestBuilder.sort(s->s.field(ss->ss.field("PlayStatnum").order(SortOrder.Desc)));
        String finalSort = sort;
        String finalOrderField = orderField;
        requestBuilder.sort(s -> s.field(ss -> ss.field(finalOrderField)
                .order("asc".equals(finalSort) ? SortOrder.Asc : SortOrder.Desc)));
        }else{
            //默认排序方式
            requestBuilder.sort(f->f.field(o->o.field("_score").order(SortOrder.Desc)));
        }
        //分页
        // from  size
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        Integer from = (pageNo - 1) * pageSize;
        //开始位置
        requestBuilder.from(from);
        //每页记录数
        requestBuilder.size(pageSize);

        // albuminfo -- query -- bool
        requestBuilder.index("albuminfo").query(q -> q.bool(boolQuery.build()));
        SearchRequest searchRequest = requestBuilder.build();
        return searchRequest;
    }

    //根据查询结果,封装成AlbumSearchResponseVo
    private AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> searchResponse) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        //总记录数
        long total = searchResponse.hits().total().value();
        albumSearchResponseVo.setTotal(total);
        //List<AlbumInfoIndexVo>
        List<Hit<AlbumInfoIndex>> hits = searchResponse.hits().hits();
        if (!CollectionUtils.isEmpty(hits)) {
            List<AlbumInfoIndexVo> albumInfoIndexVoList = hits.stream().map(albumInfoIndexHit -> {
                AlbumInfoIndex albumInfoIndex = albumInfoIndexHit.source();
                AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
                BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
                //  判断用户是否根据关键词进行检索.
                if (null != albumInfoIndexHit.highlight().get("albumTitle")) {
                    //  获取高亮数据
                    String albumTitle = albumInfoIndexHit.highlight().get("albumTitle").get(0);
                    //  赋值高亮数据
                    albumInfoIndexVo.setAlbumTitle(albumTitle);
                }
                return albumInfoIndexVo;
            }).collect(Collectors.toList());

            albumSearchResponseVo.setList(albumInfoIndexVoList);
        }
        return albumSearchResponseVo;
    }


    //根据一级分类Id获取数据
    @Override
    public List<Map<String, Object>> channel(Long category1Id) {
        //1 远程调用：根据一级分类id获取下面前7个三级分类数据
        Result<List<BaseCategory3>> baseCategory3ListResult =
                categoryFeignClient.findTopBaseCategory3(category1Id);
        List<BaseCategory3> category3List = baseCategory3ListResult.getData();

        //category3List转换map map的key三级分类id  value对应数据（包含三级名称）
        Map<Long, BaseCategory3> category3Map =
                category3List.stream().collect(Collectors.toMap(BaseCategory3::getId,
                        BaseCategory3 -> BaseCategory3));
        //2 从查询所有三级分类数据里面获取所有三级分类id 集合
        List<Long> idList =
                category3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());

        //3 构建DSL语句，查询es得到专辑数据
        // 根据三级分类id集合进行查询，根据三级分类id进行分组，每组获取热门前6个专辑
        SearchRequest.Builder request = new SearchRequest.Builder();

        //  List<Long>  --  List<FieldValue>
        List<FieldValue> valueList =
                idList.stream().map(id -> FieldValue.of(id)).collect(Collectors.toList());

        request.index("albuminfo")
                .query(q->q.terms(f->f.field("category3Id")
                        .terms(new TermsQueryField.Builder().value(valueList).build())));

        request.aggregations("groupByCategory3IdAgg",
                a->a.terms(t->t.field("category3Id").size(10))
                        .aggregations("topTenHotScoreAgg",a1->a1.topHits(s->s.size(6)
                                .sort(s1->s1.field(f->f.field("hotScore")
                                        .order(SortOrder.Desc))))));
        try {
            SearchResponse<AlbumInfoIndex> response =
                    elasticsearchClient.search(request.build(), AlbumInfoIndex.class);

            //  声明集合
            List<Map<String, Object>> result = new ArrayList<>();
            //  从聚合中获取数据
            Aggregate groupByCategory3IdAgg =
                    response.aggregations().get("groupByCategory3IdAgg");
            groupByCategory3IdAgg.lterms().buckets().array().forEach(item ->{
                //  创建集合数据
                List<AlbumInfoIndex> albumInfoIndexList = new ArrayList<>();
                //  获取三级分类Id 对象
                long category3Id = item.key();
                //  获取要置顶的集合数据
                Aggregate topTenHotScoreAgg = item.aggregations().get("topTenHotScoreAgg");
                //  循环遍历获取聚合中的数据
                topTenHotScoreAgg.topHits().hits().hits().forEach(hit->{
                    //  获取到source 的json 字符串数据
                    String json = hit.source().toString();
                    //  将json 字符串转换为AlbumInfoIndex 对象
                    AlbumInfoIndex albumInfoIndex = JSON.parseObject(json, AlbumInfoIndex.class);
                    //  将对象添加到集合中
                    albumInfoIndexList.add(albumInfoIndex);
                });
                //  声明一个map 集合数据
                Map<String, Object> map = new HashMap<>();

                //  存储根据三级分类Id要找到的三级分类
                map.put("baseCategory3",category3Map.get(category3Id));

                //  存储所有的专辑集合数据
                map.put("list",albumInfoIndexList);
                //  将map 添加到集合中
                //4 得到结果，按照要求进行封装，返回
                result.add(map);
            });
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    //自动补全功能
    @Override
    public List<String> completeSuggest(String keyword) {
        //        GET test/_search
        //        {
        //            "suggest": {
        //            "completer": {
        //                "prefix": "foe",
        //                        "completion": {
        //                    "field": "suggest",
        //                            "skip_duplicates": true,
        //                            "fuzzy": {
        //                        "fuzziness": "auto"
        //                    }
        //                }
        //            }
        //        }
        //        }
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        searchRequest.index("suggestinfo")
                .suggest(s->s.suggesters("suggestionKeyword",
                                f->f.prefix(keyword)
                                        .completion(
                                                c->c.field("keyword")
                                                        .skipDuplicates(true)
                                                        .size(10)
                                                        .fuzzy(
                                                                z->z.fuzziness("auto")
                                                        )
                                        ))
                        .suggesters("suggestionkeywordPinyin",f->f.prefix(keyword).completion(
                                c->c.field("keywordPinyin").skipDuplicates(true).size(10)
                                        .fuzzy(z->z.fuzziness("auto"))
                        ))
                        .suggesters("suggestionkeywordSequence",f->f.prefix(keyword).completion(
                                c->c.field("keywordSequence").skipDuplicates(true).size(10)
                                        .fuzzy(z->z.fuzziness("auto"))
                        )));

        try {
            SearchResponse<SuggestIndex> searchResponse =
                    elasticsearchClient.search(searchRequest.build(), SuggestIndex.class);

            HashSet<String> titleSet = new HashSet<>();
            titleSet.addAll(this.parseSearchResult(searchResponse,"suggestionKeyword"));
            titleSet.addAll(this.parseSearchResult(searchResponse,"suggestionkeywordPinyin"));
            titleSet.addAll(this.parseSearchResult(searchResponse,"suggestionkeywordSequence"));

            //  判断：
            if (titleSet.size()<10){
                //  使用查询数据的方式来填充集合数据，让这个提示信息够10条数据.
                SearchResponse<SuggestIndex> response = null;
                try {
                    response = elasticsearchClient.search(s -> s.index("suggestinfo")
                                    .query(f -> f.match(m -> m.field("title").query(keyword)))
                            , SuggestIndex.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //  从查询结果集中获取数据
                for (Hit<SuggestIndex> hit : response.hits().hits()) {
                    //  获取数据结果
                    SuggestIndex suggestIndex = hit.source();
                    //  获取titile
                    titleSet.add(suggestIndex.getTitle());
                    //  判断当前这个结合的长度.
                    if (titleSet.size()==10){
                        break;
                    }
                }
            }
            return new ArrayList<>(titleSet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //更新排行榜
    @SneakyThrows
    @Override
    public void updateLatelyAlbumRanking() {
        //1 远程调用：查询所有一级分类，得到list集合
        Result<List<BaseCategory1>> category1Result = categoryFeignClient.getCategory1();
        List<BaseCategory1> category1List = category1Result.getData();
        //2 遍历所有一级分类集合，得到每个一级分类
        for (BaseCategory1 baseCategory1 : category1List){
            Long category1Id = baseCategory1.getId();
            //创建数组，存储五个统计数据
            String[] rankingDimensionArray = new String[]{"hotScore",
                    "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
            //遍历数组，得到每个统计指标
            for (String ranging :  rankingDimensionArray) {
                //3 拿着每个一级分类id + 统计指标（播放量、评论量）查询es
                //得到排序之后的数据
                SearchRequest.Builder searchRequest = new SearchRequest.Builder();
                /*
                GET /albuminfo/_search
                            {
                              "query": {
                                "term": {
                                  "category1Id": {
                                    "value": "1"
                                  }
                                }
                              }
                              ,"sort": [
                                {
                                  "playStatNum": {
                                    "order": "desc"
                                  }
                                }
                              ],
                              "size": 10
                            }
                 */
                searchRequest.index("albuminfo").query(q->q.term(t->t.field("category1Id").value(category1Id)))
                        .sort(s->s.field(d->d.field(ranging).order(SortOrder.Desc))).size(10);
                SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(searchRequest.build(), AlbumInfoIndex.class);
                List<AlbumInfoIndex> albumInfoIndexList = response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
                //4 把每个一级分类里面根据不同指标排序之后数据放到redis里面
                //使用redis里面hash类型存储数据
                //redis的key是一级分类id
                //redis的field是不同指标（播放量、评论量）
                //redis的value对应的数据
                String rangKey = RedisConstant.RANKING_KEY_PREFIX+baseCategory1.getId();
                redisTemplate.opsForHash().put(rangKey,ranging,albumInfoIndexList);
            }
        }

    }
}