package com.redmath.jobportal.job.controller;

import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;


    @GetMapping
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PostMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id){
        return jobService.getJobById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Job> createJob(@RequestBody Job job, Authentication authentication){
        return ResponseEntity.ok(jobService.createJob(job, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id,@RequestBody Job job,Authentication authentication){
        return jobService.updateJob(id,job,authentication).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id, Authentication authentication) {
        boolean deleted = jobService.deleteJob(id, authentication);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.status(403).build();
    }



}
