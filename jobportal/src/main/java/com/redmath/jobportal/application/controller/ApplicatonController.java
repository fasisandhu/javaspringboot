package com.redmath.jobportal.application.controller;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.dto.ApplicationRecruiterDto;
import com.redmath.jobportal.application.dto.ApplicationUserDto;
import com.redmath.jobportal.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    //for testing
//    @GetMapping("/whoami")
//    public Map<String, Object> whoAmI(Authentication auth) {
//        Map<String, Object> result = new HashMap<>();
//        result.put("authenticationType", auth.getClass().getName());
//        result.put("principalType", auth.getPrincipal().getClass().getName());
//
//        if (auth.getPrincipal() instanceof CustomOAuth2User) {
//            CustomOAuth2User user = (CustomOAuth2User) auth.getPrincipal();
//            result.put("email", user.getEmail());
//            result.put("name", user.getName());
//            result.put("role", user.getRole());
//        }
//
//        result.put("authorities", auth.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .collect(Collectors.toList()));
//
//        return result;
//    }

    @PostMapping("/{jobId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ApplicationDto apply(@PathVariable Long jobId, Authentication auth) {
        return applicationService.applyToJob(jobId, auth);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    public List<ApplicationUserDto> myApplications(Authentication auth) {
        return applicationService.getApplicationsByUser(auth);
    }

    @GetMapping("/recruiter/all")
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<ApplicationRecruiterDto> getApplicationsForAllJobsByRecruiter(Authentication auth) {
        return applicationService.getApplicationsForAllPostedJobs(auth);
    }

    @GetMapping("/recruiter/job/{jobId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<ApplicationRecruiterDto> getApplicationsForJobByRecruiter(@PathVariable Long jobId, Authentication auth) {
        return applicationService.getApplicationsForSpecificPostedJob(auth, jobId);
    }


}
