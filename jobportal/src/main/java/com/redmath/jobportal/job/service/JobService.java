package com.redmath.jobportal.job.service;


import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.exceptions.UnauthorizedJobAccessException;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;


    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " not found"));
    }

    public Job createJob(Job job, Authentication authentication) {
        job.setPostedBy(getLoggedInUser(authentication).getEmail());
        return jobRepository.save(job);
    }

    public Job updateJob(Long id, Job updatedJob, Authentication authentication) {
        Job existing = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " not found"));

        if (!existing.getPostedBy().equals(getLoggedInUser(authentication).getEmail())) {
            log.info("job.getPostedBy() = {}", existing.getPostedBy());
            log.info("Logged in user email = {}", getLoggedInUser(authentication).getEmail());
            throw new UnauthorizedJobAccessException("You are not authorized to update this job");
        }

        existing.setTitle(updatedJob.getTitle());
        existing.setDescription(updatedJob.getDescription());
        existing.setCompany(updatedJob.getCompany());
        existing.setRemote(updatedJob.isRemote());
        existing.setSalary(updatedJob.getSalary());

        return jobRepository.save(existing);
    }

    public void deleteJob(Long id, Authentication authentication) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " not found"));

        if (!job.getPostedBy().equals(getLoggedInUser(authentication).getEmail())) {
            log.info("job.getPostedBy() = {}", job.getPostedBy());
            log.info("Logged in user email = {}", getLoggedInUser(authentication).getEmail());
            throw new UnauthorizedJobAccessException("You are not authorized to delete this job");
        }

        jobRepository.deleteById(id);
    }

//    private User getLoggedInUser(Authentication auth) {
//        if (auth == null || !auth.isAuthenticated()) {
//            throw new SecurityException("Not authenticated");
//        }
//
//        if (auth.getPrincipal() instanceof CustomOAuth2User) {
//            CustomOAuth2User oauthUser = (CustomOAuth2User) auth.getPrincipal();
//            String email = oauthUser.getEmail();
//            return userRepository.findByEmail(email)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//        }
//        // Handle other authentication types if needed
//        throw new UnsupportedOperationException("Unsupported authentication type");
//    }

    private User getLoggedInUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
