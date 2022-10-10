package com.atguigu.gulimail.search.service;

import com.atguigu.gulimail.search.vo.SearchParam;
import com.atguigu.gulimail.search.vo.SearchResult;
import org.springframework.context.annotation.Bean;

public interface MallSearchService {

//    @Bean
    SearchResult search(SearchParam param);
}
