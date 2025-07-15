package com.redmath.newsapp.news;

import com.redmath.newsapp.dto.NewsRequest;
import com.redmath.newsapp.dto.NewsResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('EDITOR')")
    @PostMapping
    public NewsResponse create(@RequestBody NewsRequest request) {
        return newsService.create(request);
    }

    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('EDITOR')")
    @GetMapping("/mine")
    public List<NewsResponse> myNews() {
        return newsService.getMyNews();
    }

    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('EDITOR')")
    @PutMapping("/{id}")
    public NewsResponse update(@PathVariable Long id, @RequestBody NewsRequest request) {
        return newsService.updateNews(id, request);
    }

    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('EDITOR')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        newsService.deleteNews(id);
    }

    @GetMapping
    public List<NewsResponse> allNews() {
        return newsService.getAllNews();
    }

    @GetMapping("/category/{id}")
    public List<NewsResponse> byCategory(@PathVariable Long id) {
        return newsService.getNewsByCategory(id);
    }

    @GetMapping("/editor/{id}")
    public List<NewsResponse> byEditor(@PathVariable Long id) {
        return newsService.getNewsByEditor(id);
    }

    @GetMapping("/search")
    public List<NewsResponse> search(@RequestParam String keyword) {
        return newsService.searchNews(keyword);
    }

}
