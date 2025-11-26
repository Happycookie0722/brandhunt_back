package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Entity.Category;
import com.dev.BrandHunt.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    @Autowired
    CategoryRepository categoryRepository;

    public List<Category> getCategoryInfo() {
        return categoryRepository.findAll();
    }

    public Category matchCategory(String url, List<Category> list) {
        url = url.toLowerCase();

        for (Category category : list) {
            if (url.contains(category.getName().toLowerCase())) {
                return category;
            }
        }

        // 매칭되는 것이 없으면 기타 카테고리 반환 (없으면 null 반환)
        return list.stream()
                .filter(c -> c.getName().equalsIgnoreCase("others"))
                .findFirst()
                .orElse(null);
    }
}
