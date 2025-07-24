// src/main/java/com/jobportal/application/service/ApplicationService.java
package com.redmath.jobportal.application.service;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.repository.ApplicationRepository;
import com.redmath.jobportal.auth.services.CustomOAuth2User;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public Application applyToJob(Long jobId, Authentication auth) {
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Application application = Application.builder()
                .job(job)
                .user(user)
                .build();

        return applicationRepository.save(application);
    }

    public List<ApplicationDto> getApplicationsByUser(Authentication auth) {
        User user = getLoggedInUser(auth);
        return applicationRepository.findByUser(user).stream()
                .map(app -> new ApplicationDto(
                        app.getId(),
                        app.getJob().getId(),
                        app.getJob().getTitle()))
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getApplicationsForAllPostedJobs(Authentication auth) {
        User employer = getLoggedInUser(auth);
        List<Job> jobsPosted = jobRepository.findByPostedBy(employer.getUsername());

        return applicationRepository.findAll().stream()
                .filter(app -> jobsPosted.contains(app.getJob()))
                .map(app -> new ApplicationDto(
                        app.getId(),
                        app.getJob().getId(),
                        app.getJob().getTitle()))
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getApplicationsForSpecificPostedJob(Authentication auth, Long jobId) {
        User employer = getLoggedInUser(auth);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPostedBy().equals(employer.getUsername())) {
            throw new RuntimeException("Unauthorized: This job was not posted by you");
        }

        return applicationRepository.findByJob(job).stream()
                .map(app -> new ApplicationDto(
                        app.getId(),
                        job.getId(),
                        job.getTitle()))
                .collect(Collectors.toList());
    }

    private User getLoggedInUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Not authenticated");
        }

        if (auth.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauthUser = (CustomOAuth2User) auth.getPrincipal();
            String email = oauthUser.getEmail();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        // Handle other authentication types if needed
        throw new UnsupportedOperationException("Unsupported authentication type");
    }

//    private User getLoggedInUser(Authentication auth) {
//        String username = auth.getName();
//        return userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//    }
}
