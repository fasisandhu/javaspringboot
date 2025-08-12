package com.redmath.jobportal.job.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobDto {
    private Long id;
    private String title;
    private String description;
    private String company;
    private boolean remote;
    private double salary;
    private String postedBy;
}

