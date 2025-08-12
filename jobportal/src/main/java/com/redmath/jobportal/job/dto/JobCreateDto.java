package com.redmath.jobportal.job.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobCreateDto {
    private String title;
    private String description;
    private String company;
    private boolean remote;
    private double salary;
    // Deliberately excludes id and postedBy fields
}