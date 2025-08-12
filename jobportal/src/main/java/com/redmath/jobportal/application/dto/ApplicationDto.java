package com.redmath.jobportal.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationDto {
    private Long id;
    private Long jobId;
    private String message;
}