package com.redmath.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.repository.ApplicationRepository;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.parameters.P;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class ApplicationControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User applicantUser;
    private User employerUser;
    private Job testJob;

    @BeforeEach
    void setUp() {
        // Create test users
        applicantUser = User.builder()
                .email("applicant@example.com")
                .name("Test Applicant")
                .role(Role.APPLICANT)
                .password("password")
                .provider(AuthProvider.LOCAL)
                .build();
        applicantUser = userRepository.save(applicantUser);

        employerUser = User.builder()
                .email("employer@example.com")
                .name("Test Employer")
                .role(Role.EMPLOYER)
                .password("password")
                .provider(AuthProvider.LOCAL)
                .build();
        employerUser = userRepository.save(employerUser);

        // Create test job
        testJob = Job.builder()
                .title("Java Developer")
                .description("Java development position")
                .company("Tech Corp")
                .remote(true)
                .salary(75000.0)
                .postedBy("employer@example.com")
                .build();
        testJob = jobRepository.save(testJob);
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testCompleteApplicationFlow() throws Exception {
        // Test applying to job
        mockMvc.perform(post("/api/v1/application/{jobId}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        // Verify application was saved in database
        var applications = applicationRepository.findAll();
        assertThat(applications).hasSize(2); //2 because liquibase adds a default application on startup
        assertThat(applications.get(1).getJob().getId()).isEqualTo(testJob.getId());
        assertThat(applications.get(1).getUser().getEmail()).isEqualTo("applicant@example.com");

        // Test retrieving my applications
        mockMvc.perform(get("/api/v1/application/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$[0].companyName").value("Tech Corp"));
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testEmployerViewApplicationsFlow() throws Exception {
        // Create an application first
        Application application = Application.builder()
                .job(testJob)
                .user(applicantUser)
                .build();
        applicationRepository.save(application);

        // Test getting all applications for employer's jobs
        mockMvc.perform(get("/api/v1/application/recruiter/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$[0].applicantEmail").value("applicant@example.com"))
                .andExpect(jsonPath("$[0].applicantName").value("Test Applicant"));

        // Test getting applications for specific job
        mockMvc.perform(get("/api/v1/application/recruiter/job/{jobId}", testJob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].jobId").value(testJob.getId()))
                .andExpect(jsonPath("$[0].applicantEmail").value("applicant@example.com"));
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testDuplicateApplication_ShouldFail() throws Exception {
        // Apply once
        mockMvc.perform(post("/api/v1/application/{jobId}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isOk());

        // Try to apply again - should fail
        mockMvc.perform(post("/api/v1/application/{jobId}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isConflict());

        // Verify only one application exists
        var applications = applicationRepository.findAll();
        assertThat(applications).hasSize(2); //2 because liquibase adds a default application on startup
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplyToNonExistentJob_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/application/{jobId}", 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        // Verify no application was created
        var applications = applicationRepository.findAll();
        assertThat(applications).hasSize(1); //1 because liquibase adds a default application on startup
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testEmployerCannotApplyToJobs() throws Exception {
        mockMvc.perform(post("/api/v1/application/{jobId}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Verify no application was created
        var applications = applicationRepository.findAll();
        assertThat(applications).hasSize(1); //1 because liquibase adds a default application on startup

    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplicantCannotAccessEmployerEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/application/recruiter/all"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/application/recruiter/job/{jobId}", testJob.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(post("/api/v1/application/{jobId}", testJob.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/application/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/application/recruiter/all"))
                .andExpect(status().isUnauthorized());
    }
}