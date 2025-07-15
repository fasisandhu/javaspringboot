package com.redmath.newsapp.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {
    private Long id;
    private String title;
    private String content;
    private String categoryName;
    private String editorName;
    private String createdAt;
    private String updatedAt;
}
