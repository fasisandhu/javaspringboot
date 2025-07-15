package com.redmath.newsapp.news;

import com.redmath.newsapp.category.Category;
import com.redmath.newsapp.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News,Long> {
    List<News> findByPostedBy(User editor);

    List<News> findByCategory(Category category, Sort sort);
    List<News> findByPostedBy(User user, Sort sort);
    List<News> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);

}
