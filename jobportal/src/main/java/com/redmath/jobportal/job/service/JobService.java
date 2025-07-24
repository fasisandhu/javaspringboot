package com.redmath.jobportal.job.service;


import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.auth.services.CustomOAuth2User;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;


    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public Job createJob(Job job, Authentication authentication) {
        job.setPostedBy(getLoggedInUser(authentication).getUsername());
        return jobRepository.save(job);
    }

    public Optional<Job> updateJob(Long id, Job updatedJob, Authentication authentication) {
        return jobRepository.findById(id).map(existing -> {
            if (!existing.getPostedBy().equals(getLoggedInUser(authentication).getUsername())) return null;
            existing.setTitle(updatedJob.getTitle());
            existing.setDescription(updatedJob.getDescription());
            existing.setCompany(updatedJob.getCompany());
            existing.setRemote(updatedJob.isRemote());
            existing.setSalary(updatedJob.getSalary());
            return jobRepository.save(existing);
        });
    }

    public boolean deleteJob(Long id, Authentication authentication) {
        return jobRepository.findById(id).map(job -> {
            if (!job.getPostedBy().equals(authentication.getName())) return false;
            jobRepository.deleteById(id);
            return true;
        }).orElse(false);
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
}
