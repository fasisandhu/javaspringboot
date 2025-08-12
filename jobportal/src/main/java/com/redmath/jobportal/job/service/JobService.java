package com.redmath.jobportal.job.service;


import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.exceptions.UnauthorizedJobAccessException;
import com.redmath.jobportal.job.dto.JobCreateDto;
import com.redmath.jobportal.job.dto.JobDto;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
//@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;


    public List<JobDto> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(JobService::mapToDto)
                .collect(Collectors.toList());
    }

    public JobDto getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " not found"));
        return mapToDto(job);
    }


    public JobDto createJob(JobCreateDto jobDto, Authentication authentication) {
        Job job = Job.builder()
                .title(jobDto.getTitle())
                .description(jobDto.getDescription())
                .company(jobDto.getCompany())
                .remote(jobDto.isRemote())
                .salary(jobDto.getSalary())
                .postedBy(getLoggedInUser(authentication).getEmail())
                .build();

        return mapToDto(jobRepository.save(job));
    }

    public JobDto updateJob(Long id, JobCreateDto updatedJob, Authentication authentication) {
        Job existing = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " not found"));

        if (!existing.getPostedBy().equals(getLoggedInUser(authentication).getEmail())) {
            throw new UnauthorizedJobAccessException("You are not authorized to update this job");
        }

        existing.setTitle(updatedJob.getTitle());
        existing.setDescription(updatedJob.getDescription());
        existing.setCompany(updatedJob.getCompany());
        existing.setRemote(updatedJob.isRemote());
        existing.setSalary(updatedJob.getSalary());

        return mapToDto(jobRepository.save(existing));
    }

    public void deleteJob(Long id, Authentication authentication) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " not found"));

        if (!job.getPostedBy().equals(getLoggedInUser(authentication).getEmail())) {
//            log.info("job.getPostedBy() = {}", job.getPostedBy());
//            log.info("Logged in user email = {}", getLoggedInUser(authentication).getEmail());
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

    public static JobDto mapToDto(Job job) {
        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .company(job.getCompany())
                .remote(job.isRemote())
                .salary(job.getSalary())
                .postedBy(job.getPostedBy())
                .build();
    }

}
