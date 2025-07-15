package com.redmath.newsapp.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsRequest {
    private String title;
    private String content;
    private Long categoryId;
}
