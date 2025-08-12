package com.redmath.jobportal;

import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.exceptions.UnauthorizedJobAccessException;
import com.redmath.jobportal.job.dto.JobCreateDto;
import com.redmath.jobportal.job.dto.JobDto;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import com.redmath.jobportal.job.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JobService jobService;

    private Job sampleJob;
    private JobDto sampleJobDto;
    private User employerUser;
    private User anotherEmployerUser;

    @BeforeEach
    void setUp() {
        sampleJob = Job.builder()
                .id(1L)
                .title("Java Developer")
                .description("Spring Boot Developer")
                .company("Redmath")
                .remote(true)
                .salary(100000)
                .postedBy("employer@example.com")
                .build();

        sampleJobDto = JobDto.builder()
                .id(1L)
                .title("Java Developer")
                .description("Spring Boot Developer")
                .company("Redmath")
                .remote(true)
                .salary(100000)
                .postedBy("employer@example.com")
                .build();

        employerUser = User.builder()
                .id(1L)
                .email("employer@example.com")
                .name("John Employer")
                .role(Role.EMPLOYER)
                .build();

        anotherEmployerUser = User.builder()
                .id(2L)
                .email("another@example.com")
                .name("Jane Employer")
                .role(Role.EMPLOYER)
                .build();
    }

    @Test
    void testGetAllJobs() {
        when(jobRepository.findAll()).thenReturn(List.of(sampleJob));

        List<JobDto> result = jobService.getAllJobs();

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        verify(jobRepository).findAll();
    }

    @Test
    void testGetAllJobs_EmptyList() {
        when(jobRepository.findAll()).thenReturn(Collections.emptyList());

        List<JobDto> result = jobService.getAllJobs();

        assertTrue(result.isEmpty());
        verify(jobRepository).findAll();
    }

    @Test
    void testGetJobById_Found() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        JobDto result = jobService.getJobById(1L);

        assertEquals(sampleJob.getId(), result.getId());
        assertEquals(sampleJob.getTitle(), result.getTitle());
        verify(jobRepository).findById(1L);
    }

    @Test
    void testGetJobById_NotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        JobNotFoundException exception = assertThrows(JobNotFoundException.class,
                () -> jobService.getJobById(1L));

        assertEquals("Job with ID 1 not found", exception.getMessage());
        verify(jobRepository).findById(1L);
    }

    @Test
    void testCreateJob_Success() {
        JobCreateDto newJobDto = JobCreateDto.builder()
                .title("Backend Developer")
                .description("Node.js Developer")
                .company("TechCorp")
                .remote(false)
                .salary(90000)
                .build();

        Job savedJob = Job.builder()
                .id(2L)
                .title("Backend Developer")
                .description("Node.js Developer")
                .company("TechCorp")
                .remote(false)
                .salary(90000)
                .postedBy("employer@example.com")
                .build();

        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        JobDto result = jobService.createJob(newJobDto, authentication);

        assertEquals("Backend Developer", result.getTitle());
        assertEquals("employer@example.com", result.getPostedBy());
        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testCreateJob_UserNotFound() {
        JobCreateDto newJobDto = JobCreateDto.builder()
                .title("Backend Developer")
                .description("Node.js Developer")
                .company("TechCorp")
                .remote(false)
                .salary(90000)
                .build();

        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> jobService.createJob(newJobDto, authentication));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testUpdateJob_Success() {
        JobCreateDto updatedJobDto = JobCreateDto.builder()
                .title("Senior Java Developer")
                .description("Senior Spring Boot Developer")
                .company("Redmath Inc")
                .remote(false)
                .salary(120000)
                .build();

        Job updatedJob = Job.builder()
                .id(1L)
                .title("Senior Java Developer")
                .description("Senior Spring Boot Developer")
                .company("Redmath Inc")
                .remote(false)
                .salary(120000)
                .postedBy("employer@example.com")
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.save(any(Job.class))).thenReturn(updatedJob);

        JobDto result = jobService.updateJob(1L, updatedJobDto, authentication);

        assertEquals("Senior Java Developer", result.getTitle());
        assertEquals("Redmath Inc", result.getCompany());
        assertEquals(120000, result.getSalary());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testUpdateJob_JobNotFound() {
        JobCreateDto updatedJobDto = JobCreateDto.builder()
                .title("Senior Java Developer")
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        JobNotFoundException exception = assertThrows(JobNotFoundException.class,
                () -> jobService.updateJob(1L, updatedJobDto, authentication));

        assertEquals("Job with ID 1 not found", exception.getMessage());
        verify(jobRepository).findById(1L);
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testUpdateJob_UnauthorizedUser() {
        JobCreateDto updatedJobDto = JobCreateDto.builder()
                .title("Senior Java Developer")
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(authentication.getName()).thenReturn("another@example.com");
        when(userRepository.findByEmail("another@example.com")).thenReturn(Optional.of(anotherEmployerUser));

        UnauthorizedJobAccessException exception = assertThrows(UnauthorizedJobAccessException.class,
                () -> jobService.updateJob(1L, updatedJobDto, authentication));

        assertEquals("You are not authorized to update this job", exception.getMessage());
        verify(jobRepository).findById(1L);
        verify(userRepository, times(1)).findByEmail("another@example.com");
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testUpdateJob_AuthenticatedUserNotFound() {
        JobCreateDto updatedJobDto = JobCreateDto.builder()
                .title("Senior Java Developer")
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> jobService.updateJob(1L, updatedJobDto, authentication));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testDeleteJob_Success() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        doNothing().when(jobRepository).deleteById(1L);

        assertDoesNotThrow(() -> jobService.deleteJob(1L, authentication));

        verify(jobRepository).findById(1L);
        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).deleteById(1L);
    }

    @Test
    void testDeleteJob_JobNotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        JobNotFoundException exception = assertThrows(JobNotFoundException.class,
                () -> jobService.deleteJob(1L, authentication));

        assertEquals("Job with ID 1 not found", exception.getMessage());
        verify(jobRepository).findById(1L);
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteJob_UnauthorizedUser() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(authentication.getName()).thenReturn("another@example.com");
        when(userRepository.findByEmail("another@example.com")).thenReturn(Optional.of(anotherEmployerUser));

        UnauthorizedJobAccessException exception = assertThrows(UnauthorizedJobAccessException.class,
                () -> jobService.deleteJob(1L, authentication));

        assertEquals("You are not authorized to delete this job", exception.getMessage());
        verify(userRepository).findByEmail("another@example.com");
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteJob_AuthenticatedUserNotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> jobService.deleteJob(1L, authentication));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jobRepository, never()).deleteById(any());
    }
}
