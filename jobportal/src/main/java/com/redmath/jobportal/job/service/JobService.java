package com.redmath.jobportal.job.service;


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

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public Job createJob(Job job, Authentication authentication) {
        job.setPostedBy(authentication.getName());
        return jobRepository.save(job);
    }

    public Optional<Job> updateJob(Long id, Job updatedJob, Authentication authentication) {
        return jobRepository.findById(id).map(existing -> {
            if (!existing.getPostedBy().equals(authentication.getName())) return null;
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

}
