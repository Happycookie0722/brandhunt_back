package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.Service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularList() {
        return ResponseEntity.ok(searchService.getPopularKeyword());
    }

}
