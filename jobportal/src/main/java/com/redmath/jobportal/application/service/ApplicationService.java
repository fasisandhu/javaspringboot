package com.redmath.jobportal.application.service;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.dto.ApplicationRecruiterDto;
import com.redmath.jobportal.application.dto.ApplicationUserDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.repository.ApplicationRepository;
import com.redmath.jobportal.exceptions.DuplicateApplicationException;
import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.exceptions.UnauthorizedAccessException;
import com.redmath.jobportal.exceptions.UserNotFoundException;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public ApplicationDto applyToJob(Long jobId, Authentication auth) {
        User user = getLoggedInUser(auth);

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

        Application savedApplication = applicationRepository.save(application);

        return new ApplicationDto(
                savedApplication.getId(),
                savedApplication.getJob().getId(),
                "Application submitted successfully"
        );
    }

    public List<ApplicationUserDto> getApplicationsByUser(Authentication auth) {
        User user = getLoggedInUser(auth);
        return applicationRepository.findByUser(user).stream()
                .map(app -> new ApplicationUserDto(
                        app.getId(),
                        app.getJob().getId(),
                        app.getJob().getTitle(),
                        app.getJob().getCompany()))
                .collect(Collectors.toList());
    }

    public List<ApplicationRecruiterDto> getApplicationsForAllPostedJobs(Authentication auth) {
        User employer = getLoggedInUser(auth);
        List<Job> jobsPosted = jobRepository.findByPostedBy(employer.getEmail());

        // Extract job IDs for efficient comparison
        List<Long> jobIds = jobsPosted.stream()
                .map(Job::getId)
                .collect(Collectors.toList());

        return applicationRepository.findAll().stream()
                .filter(app -> jobIds.contains(app.getJob().getId()))
                .map(app -> new ApplicationRecruiterDto(
                        app.getId(),
                        app.getJob().getId(),
                        app.getJob().getTitle(),
                        app.getJob().getCompany(),
                        app.getUser().getName(),
                        app.getUser().getEmail()))
                .collect(Collectors.toList());
    }

    public List<ApplicationRecruiterDto> getApplicationsForSpecificPostedJob(Authentication auth, Long jobId) {
        User employer = getLoggedInUser(auth);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        if (!job.getPostedBy().equals(employer.getEmail())) {
            throw new UnauthorizedAccessException("Unauthorized: This job was not posted by you");
        }

        return applicationRepository.findByJob(job).stream()
                .map(app -> new ApplicationRecruiterDto(
                        app.getId(),
                        job.getId(),
                        job.getTitle(),
                        job.getCompany(),
                        app.getUser().getName(),
                        app.getUser().getEmail()))
                .collect(Collectors.toList());
    }

    private User getLoggedInUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}