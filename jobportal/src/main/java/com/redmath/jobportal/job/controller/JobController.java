package com.redmath.jobportal.job.controller;

import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id); // Service throws exception if not found
        return ResponseEntity.ok(job);
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Job> createJob(@RequestBody Job job, Authentication authentication){
        return ResponseEntity.ok(jobService.createJob(job, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job, Authentication authentication) {
        Job updatedJob = jobService.updateJob(id, job, authentication);
        return ResponseEntity.ok(updatedJob);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id, Authentication authentication) {
        jobService.deleteJob(id, authentication);
        return ResponseEntity.noContent().build();
    }



}
