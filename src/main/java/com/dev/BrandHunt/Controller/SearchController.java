package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.Service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequiredArgsConstructor
@RestController(value = "/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularList() {
        return ResponseEntity.ok(searchService.getPopularKeyword());
    }

}
