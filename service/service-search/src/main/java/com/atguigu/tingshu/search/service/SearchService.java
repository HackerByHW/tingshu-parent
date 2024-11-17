package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SearchService {


    void upperAlbum(Long albumId);

    /**
     * 下架专辑
     * @param albumId
     */
    void lowerAlbum(Long albumId);


    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    List<Map<String, Object>> channel(Long category1Id);

    List<String> completeSuggest(String keyword);

    void updateLatelyAlbumRanking();

    List<AlbumInfoIndexVo> findRankingList(Long category1Id, String dimension);
}