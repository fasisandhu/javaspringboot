package com.redmath.jobportal.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationRecruiterDto {
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String applicantName;
    private String applicantEmail;
}
