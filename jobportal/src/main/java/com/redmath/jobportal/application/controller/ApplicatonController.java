package com.redmath.jobportal.application.controller;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/application")
@RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth")
public class ApplicatonController {

    private final ApplicationService applicationService;

    @PostMapping("/{jobId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public Application apply(@PathVariable Long jobId, Authentication auth) {
        return applicationService.applyToJob(jobId, auth);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    public List<ApplicationDto> myApplications(Authentication auth) {
        return applicationService.getApplicationsByUser(auth);
    }

    @GetMapping("/recruiter/all")
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<ApplicationDto> getApplicationsForAllJobsByRecruiter(Authentication auth) {
        return applicationService.getApplicationsForAllPostedJobs(auth);
    }

    @GetMapping("/recruiter/job/{jobId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<ApplicationDto> getApplicationsForJobByRecruiter(@PathVariable Long jobId, Authentication auth) {
        return applicationService.getApplicationsForSpecificPostedJob(auth, jobId);
    }


}
