package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * ClassName: SuggestIndexRepository
 * Package: com.atguigu.tingshu.search.repository
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/11 0:33
 * @Version 1.0
 */
public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex,String> {
}
