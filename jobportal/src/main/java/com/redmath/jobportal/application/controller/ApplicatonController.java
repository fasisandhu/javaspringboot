package com.redmath.jobportal.application.controller;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.service.ApplicationService;
import com.redmath.jobportal.auth.services.CustomOAuth2User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/application")
@RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth")
public class ApplicatonController {

    private final ApplicationService applicationService;

    //for testing
    @GetMapping("/whoami")
    public Map<String, Object> whoAmI(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        result.put("authenticationType", auth.getClass().getName());
        result.put("principalType", auth.getPrincipal().getClass().getName());

        if (auth.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User user = (CustomOAuth2User) auth.getPrincipal();
            result.put("email", user.getEmail());
            result.put("name", user.getName());
            result.put("role", user.getRole());
        }

        result.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return result;
    }

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
