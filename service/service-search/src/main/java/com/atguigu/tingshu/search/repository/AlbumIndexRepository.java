package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ClassName: AlbumIndexRepository
 * Package: com.atguigu.tingshu.search.repository
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/6 11:52
 * @Version 1.0
 */
public interface AlbumIndexRepository extends ElasticsearchRepository<AlbumInfoIndex, Long> {


}
