package com.redmath.jobportal.application.service;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.repository.ApplicationRepository;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.*;
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
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        // Check for duplicate application
        boolean alreadyApplied = applicationRepository.existsByUserAndJob(user, job);
        if (alreadyApplied) {
            throw new DuplicateApplicationException("You have already applied for this job");
        }

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
        List<Job> jobsPosted = jobRepository.findByPostedBy(employer.getEmail());

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
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        if (!job.getPostedBy().equals(employer.getEmail())) {
            throw new UnauthorizedAccessException("Unauthorized: This job was not posted by you");
        }

        return applicationRepository.findByJob(job).stream()
                .map(app -> new ApplicationDto(
                        app.getId(),
                        job.getId(),
                        job.getTitle()))
                .collect(Collectors.toList());
    }

    private User getLoggedInUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}