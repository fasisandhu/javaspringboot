package com.redmath.jobportal;

import com.redmath.jobportal.application.dto.ApplicationDto;
import com.redmath.jobportal.application.dto.ApplicationRecruiterDto;
import com.redmath.jobportal.application.dto.ApplicationUserDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.repository.ApplicationRepository;
import com.redmath.jobportal.application.service.ApplicationService;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ApplicationService applicationService;

    private Job sampleJob;
    private User applicantUser;
    private User employerUser;
    private Application sampleApplication;

    @BeforeEach
    void setUp() {
        applicantUser = User.builder()
                .id(1L)
                .email("applicant@example.com")
                .name("John Applicant")
                .role(Role.APPLICANT)
                .build();

        employerUser = User.builder()
                .id(2L)
                .email("employer@example.com")
                .name("Jane Employer")
                .role(Role.EMPLOYER)
                .build();

        sampleJob = Job.builder()
                .id(101L)
                .title("Java Developer")
                .description("Spring Boot Developer")
                .company("Tech Corp")
                .remote(true)
                .salary(100000)
                .postedBy("employer@example.com")
                .build();

        sampleApplication = Application.builder()
                .id(1L)
                .job(sampleJob)
                .user(applicantUser)
                .build();
    }

    @Test
    void testApplyToJob_Success() {
        when(authentication.getName()).thenReturn("applicant@example.com");
        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicantUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.existsByUserAndJob(applicantUser, sampleJob)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(sampleApplication);

        ApplicationDto result = applicationService.applyToJob(101L, authentication);

        assertEquals(1L, result.getId());
        assertEquals(101L, result.getJobId());
        assertEquals("Application submitted successfully", result.getMessage());
        verify(userRepository).findByEmail("applicant@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository).existsByUserAndJob(applicantUser, sampleJob);
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void testApplyToJob_JobNotFound() {
        when(authentication.getName()).thenReturn("applicant@example.com");
        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicantUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());

        JobNotFoundException exception = assertThrows(JobNotFoundException.class,
                () -> applicationService.applyToJob(101L, authentication));

        assertEquals("Job not found with id: 101", exception.getMessage());
        verify(userRepository).findByEmail("applicant@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void testApplyToJob_UserNotFound() {
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyToJob(101L, authentication));

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void testApplyToJob_AlreadyApplied() {
        when(authentication.getName()).thenReturn("applicant@example.com");
        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicantUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.existsByUserAndJob(applicantUser, sampleJob)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyToJob(101L, authentication));

        assertEquals("You have already applied for this job", exception.getMessage());
        verify(userRepository).findByEmail("applicant@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository).existsByUserAndJob(applicantUser, sampleJob);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void testGetApplicationsByUser_Success() {
        List<Application> applications = Arrays.asList(sampleApplication);
        when(authentication.getName()).thenReturn("applicant@example.com");
        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicantUser));
        when(applicationRepository.findByUser(applicantUser)).thenReturn(applications);

        List<ApplicationUserDto> result = applicationService.getApplicationsByUser(authentication);

        assertEquals(1, result.size());
        ApplicationUserDto dto = result.get(0);
        assertEquals(1L, dto.getApplicationId());
        assertEquals(101L, dto.getJobId());
        assertEquals("Java Developer", dto.getJobTitle());
        assertEquals("Tech Corp", dto.getCompanyName());

        verify(userRepository).findByEmail("applicant@example.com");
        verify(applicationRepository).findByUser(applicantUser);
    }

    @Test
    void testGetApplicationsByUser_UserNotFound() {
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsByUser(authentication));

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(applicationRepository, never()).findByUser(any());
    }

    @Test
    void testGetApplicationsByUser_EmptyList() {
        when(authentication.getName()).thenReturn("applicant@example.com");
        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicantUser));
        when(applicationRepository.findByUser(applicantUser)).thenReturn(Arrays.asList());

        List<ApplicationUserDto> result = applicationService.getApplicationsByUser(authentication);

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("applicant@example.com");
        verify(applicationRepository).findByUser(applicantUser);
    }

    @Test
    void testGetApplicationsForAllPostedJobs_Success() {
        Job anotherJob = Job.builder()
                .id(102L)
                .title("Frontend Developer")
                .description("React Developer")
                .company("Web Corp")
                .remote(false)
                .salary(90000)
                .postedBy("employer@example.com")
                .build();

        User anotherApplicant = User.builder()
                .id(3L)
                .email("another@example.com")
                .name("Bob Applicant")
                .role(Role.APPLICANT)
                .build();

        Application anotherApplication = Application.builder()
                .id(2L)
                .job(anotherJob)
                .user(anotherApplicant)
                .build();

        List<Job> employerJobs = Arrays.asList(sampleJob, anotherJob);
        List<Application> allApplications = Arrays.asList(sampleApplication, anotherApplication);

        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findByPostedBy("employer@example.com")).thenReturn(employerJobs);
        // Mock findAll() instead of findByJob() calls since that's what the implementation uses
        when(applicationRepository.findAll()).thenReturn(allApplications);

        List<ApplicationRecruiterDto> result = applicationService.getApplicationsForAllPostedJobs(authentication);

        assertEquals(2, result.size());

        // Sort by application ID to ensure consistent order
        result.sort((a, b) -> Long.compare(a.getApplicationId(), b.getApplicationId()));

        ApplicationRecruiterDto firstDto = result.get(0);
        assertEquals(1L, firstDto.getApplicationId());
        assertEquals(101L, firstDto.getJobId());
        assertEquals("Java Developer", firstDto.getJobTitle());
        assertEquals("Tech Corp", firstDto.getCompanyName());
        assertEquals("John Applicant", firstDto.getApplicantName());
        assertEquals("applicant@example.com", firstDto.getApplicantEmail());

        ApplicationRecruiterDto secondDto = result.get(1);
        assertEquals(2L, secondDto.getApplicationId());
        assertEquals(102L, secondDto.getJobId());
        assertEquals("Frontend Developer", secondDto.getJobTitle());
        assertEquals("Web Corp", secondDto.getCompanyName());
        assertEquals("Bob Applicant", secondDto.getApplicantName());
        assertEquals("another@example.com", secondDto.getApplicantEmail());

        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).findByPostedBy("employer@example.com");
        verify(applicationRepository).findAll();
        // Remove these verifications since findByJob is not called
        // verify(applicationRepository).findByJob(sampleJob);
        // verify(applicationRepository).findByJob(anotherJob);
    }

    @Test
    void testGetApplicationsForAllPostedJobs_UserNotFound() {
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForAllPostedJobs(authentication));

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jobRepository, never()).findByPostedBy(any());
    }

    @Test
    void testGetApplicationsForAllPostedJobs_NoJobsPosted() {
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findByPostedBy("employer@example.com")).thenReturn(Arrays.asList());

        List<ApplicationRecruiterDto> result = applicationService.getApplicationsForAllPostedJobs(authentication);

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).findByPostedBy("employer@example.com");
        verify(applicationRepository, never()).findByJob(any());
    }

    @Test
    void testGetApplicationsForSpecificPostedJob_Success() {
        List<Application> applications = Arrays.asList(sampleApplication);
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.findByJob(sampleJob)).thenReturn(applications);

        List<ApplicationRecruiterDto> result = applicationService.getApplicationsForSpecificPostedJob(authentication, 101L);

        assertEquals(1, result.size());
        ApplicationRecruiterDto dto = result.get(0);
        assertEquals(1L, dto.getApplicationId());
        assertEquals(101L, dto.getJobId());
        assertEquals("Java Developer", dto.getJobTitle());
        assertEquals("Tech Corp", dto.getCompanyName());
        assertEquals("John Applicant", dto.getApplicantName());
        assertEquals("applicant@example.com", dto.getApplicantEmail());

        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository).findByJob(sampleJob);
    }

    @Test
    void testGetApplicationsForSpecificPostedJob_JobNotFound() {
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());

        JobNotFoundException exception = assertThrows(JobNotFoundException.class,
                () -> applicationService.getApplicationsForSpecificPostedJob(authentication, 101L));

        assertEquals("Job not found with id: 101", exception.getMessage());
        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository, never()).findByJob(any());
    }

    @Test
    void testGetApplicationsForSpecificPostedJob_UnauthorizedAccess() {
        Job anotherEmployerJob = Job.builder()
                .id(101L)
                .title("Java Developer")
                .description("Spring Boot Developer")
                .company("Tech Corp")
                .remote(true)
                .salary(100000)
                .postedBy("another@example.com")
                .build();

        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(anotherEmployerJob));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForSpecificPostedJob(authentication, 101L));

        assertEquals("Unauthorized: This job was not posted by you", exception.getMessage());
        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository, never()).findByJob(any());
    }

    @Test
    void testGetApplicationsForSpecificPostedJob_UserNotFound() {
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForSpecificPostedJob(authentication, 101L));

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).findByJob(any());
    }

    @Test
    void testGetApplicationsForSpecificPostedJob_NoApplications() {
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.findByJob(sampleJob)).thenReturn(Arrays.asList());

        List<ApplicationRecruiterDto> result = applicationService.getApplicationsForSpecificPostedJob(authentication, 101L);

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("employer@example.com");
        verify(jobRepository).findById(101L);
        verify(applicationRepository).findByJob(sampleJob);
    }

    @Test
    void testMultipleApplicationsForSameJob() {
        User secondApplicant = User.builder()
                .id(4L)
                .email("second@example.com")
                .name("Second Applicant")
                .role(Role.APPLICANT)
                .build();

        Application secondApplication = Application.builder()
                .id(3L)
                .job(sampleJob)
                .user(secondApplicant)
                .build();

        List<Application> applications = Arrays.asList(sampleApplication, secondApplication);
        when(authentication.getName()).thenReturn("employer@example.com");
        when(userRepository.findByEmail("employer@example.com")).thenReturn(Optional.of(employerUser));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.findByJob(sampleJob)).thenReturn(applications);

        List<ApplicationRecruiterDto> result = applicationService.getApplicationsForSpecificPostedJob(authentication, 101L);

        assertEquals(2, result.size());
        assertEquals("John Applicant", result.get(0).getApplicantName());
        assertEquals("Second Applicant", result.get(1).getApplicantName());
    }
}