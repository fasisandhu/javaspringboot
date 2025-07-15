package com.redmath.newsapp.news;

import com.redmath.newsapp.category.Category;
import com.redmath.newsapp.category.CategoryRepository;
import com.redmath.newsapp.dto.NewsRequest;
import com.redmath.newsapp.dto.NewsResponse;
import com.redmath.newsapp.user.CurrentUser;
import com.redmath.newsapp.user.Role;
import com.redmath.newsapp.user.User;
import com.redmath.newsapp.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class NewsService {
    @PersistenceContext
    private EntityManager entityManager;

    private final NewsRepository newsRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public NewsResponse create(NewsRequest request) {
        User editor = CurrentUser.get();

        assert editor != null;
        if (editor.getRole() != Role.EDITOR) throw new RuntimeException("Only EDITORS can post news.");

        var category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new RuntimeException("Category not found"));

        News news = News.builder().title(request.getTitle()).content(request.getContent()).category(category).postedBy(editor).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        newsRepository.save(news);
        return toDto(news);
    }

    public List<NewsResponse> getMyNews() {
        User editor = CurrentUser.get();
        return newsRepository.findByPostedBy(editor).stream().map(this::toDto).toList();
    }

    @Transactional
    public void deleteNews(Long id) {
        News news = newsRepository.findById(id).orElseThrow(() -> new RuntimeException("News not found"));
        if (news.getPostedBy().getId() != CurrentUser.get().getId())
            throw new RuntimeException("Not your news article");

        newsRepository.delete(news);
    }

    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request) {
        News news = newsRepository.findById(id).orElseThrow(() -> new RuntimeException("News not found"));

        if (news.getPostedBy().getId() != CurrentUser.get().getId())
            throw new RuntimeException("Not your news article");

        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setUpdatedAt(LocalDateTime.now());

        if (request.getCategoryId() != null) {
            var category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new RuntimeException("Category not found"));
            news.setCategory(category);
        }

        return toDto(news);
    }

    private NewsResponse toDto(News news) {
        return NewsResponse.builder().id(news.getId()).title(news.getTitle()).content(news.getContent()).categoryName(news.getCategory().getName()).editorName(news.getPostedBy().getName()).createdAt(news.getCreatedAt().toString()).updatedAt(news.getUpdatedAt().toString()).build();
    }

    public List<NewsResponse> getAllNews() {
        return newsRepository.findAll(Sort.by("createdAt").descending())
                .stream().map(this::toDto).toList();
    }

    public List<NewsResponse> getNewsByCategory(Long categoryId) {
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return newsRepository.findByCategory(category, Sort.by("createdAt").descending())
                .stream().map(this::toDto).toList();
    }

    public List<NewsResponse> getNewsByEditor(Long editorId) {
        var editor = userRepository.findById(editorId)
                .orElseThrow(() -> new RuntimeException("Editor not found"));

        return newsRepository.findByPostedBy(editor, Sort.by("createdAt").descending())
                .stream().map(this::toDto).toList();
    }

    public List<NewsResponse> searchNews(String keyword) {
        return newsRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword)
                .stream().map(this::toDto).toList();
    }
}
