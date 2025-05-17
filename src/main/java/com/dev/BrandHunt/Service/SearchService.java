package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Entity.SearchLog;
import com.dev.BrandHunt.Repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final RedisService redisService;

    public void recordSearch(SearchLog searchLog) {
        searchRepository.save(searchLog);
    }


}
