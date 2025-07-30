package com.redmath.jobportal.application.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationUserDto {
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String companyName;
}
