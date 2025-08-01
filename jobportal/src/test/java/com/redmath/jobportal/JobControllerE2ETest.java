package com.redmath.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
public class JobControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User employerUser;
    private User anotherEmployerUser;
    private Job testJob;

    @BeforeEach
    void setUp() {
        // Create test users
        employerUser = User.builder()
                .email("employer@example.com")
                .name("Test Employer")
                .role(Role.EMPLOYER)
                .password("password")
                .provider(AuthProvider.LOCAL)
                .build();
        employerUser = userRepository.save(employerUser);

        anotherEmployerUser = User.builder()
                .email("another@example.com")
                .name("Another Employer")
                .role(Role.EMPLOYER)
                .password("password")
                .provider(AuthProvider.LOCAL)
                .build();
        anotherEmployerUser = userRepository.save(anotherEmployerUser);

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
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testGetAllJobs() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Software Engineer")) // From liquibase
                .andExpect(jsonPath("$[1].title").value("Java Developer")); // From test setup
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testGetJobById() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{id}", testJob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testJob.getId()))
                .andExpect(jsonPath("$.title").value("Java Developer"))
                .andExpect(jsonPath("$.company").value("Tech Corp"))
                .andExpect(jsonPath("$.remote").value(true))
                .andExpect(jsonPath("$.salary").value(75000.0))
                .andExpect(jsonPath("$.postedBy").value("employer@example.com"));
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testGetJobById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testCreateJob_Success() throws Exception {
        Job newJob = Job.builder()
                .title("Senior Python Developer")
                .description("Senior Python development position")
                .company("Python Corp")
                .remote(false)
                .salary(90000.0)
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newJob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Python Developer"))
                .andExpect(jsonPath("$.company").value("Python Corp"))
                .andExpect(jsonPath("$.remote").value(false))
                .andExpect(jsonPath("$.salary").value(90000.0))
                .andExpect(jsonPath("$.postedBy").value("employer@example.com"));

        // Verify job was saved in database
        var allJobs = jobRepository.findAll();
        assertThat(allJobs).hasSize(3); // 1 from liquibase + 1 from setup + 1 new
        var savedJob = allJobs.stream()
                .filter(job -> "Senior Python Developer".equals(job.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(savedJob.getPostedBy()).isEqualTo("employer@example.com");
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testUpdateJob_Success() throws Exception {
        Job updatedJob = Job.builder()
                .title("Senior Java Developer")
                .description("Updated Java development position")
                .company("Updated Tech Corp")
                .remote(false)
                .salary(85000.0)
                .build();

        mockMvc.perform(put("/api/v1/jobs/{id}", testJob.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedJob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.description").value("Updated Java development position"))
                .andExpect(jsonPath("$.company").value("Updated Tech Corp"))
                .andExpect(jsonPath("$.remote").value(false))
                .andExpect(jsonPath("$.salary").value(85000.0));

        // Verify job was updated in database
        var updatedJobFromDb = jobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(updatedJobFromDb.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(updatedJobFromDb.getSalary()).isEqualTo(85000.0);
    }

    @Test
    @WithMockUser(username = "another@example.com", roles = {"EMPLOYER"})
    void testUpdateJob_Unauthorized() throws Exception {
        Job updatedJob = Job.builder()
                .title("Unauthorized Update")
                .build();

        mockMvc.perform(put("/api/v1/jobs/{id}", testJob.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedJob)))
                .andExpect(status().isConflict());

        // Verify job was not updated
        var jobFromDb = jobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(jobFromDb.getTitle()).isEqualTo("Java Developer"); // Original title
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testDeleteJob_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/{id}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify job was deleted from database
        var jobExists = jobRepository.findById(testJob.getId());
        assertThat(jobExists).isEmpty();
    }

    @Test
    @WithMockUser(username = "another@example.com", roles = {"EMPLOYER"})
    void testDeleteJob_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/{id}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isConflict());

        // Verify job was not deleted
        var jobExists = jobRepository.findById(testJob.getId());
        assertThat(jobExists).isPresent();
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplicantCannotCreateJob() throws Exception {
        Job newJob = Job.builder()
                .title("Unauthorized Job")
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newJob)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplicantCannotUpdateJob() throws Exception {
        Job updatedJob = Job.builder()
                .title("Unauthorized Update")
                .build();

        mockMvc.perform(put("/api/v1/jobs/{id}", testJob.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedJob)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplicantCannotDeleteJob() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/{id}", testJob.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testUnauthenticatedCanAccess() throws Exception {
        // Public endpoints should work without authentication
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/jobs/{id}", testJob.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testUnauthenticatedCannotAccessProtectedEndpoints() throws Exception {
        Job newJob = Job.builder()
                .title("Test Job")
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newJob)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/jobs/{id}", testJob.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newJob)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/jobs/{id}", testJob.getId()))
                .andExpect(status().isForbidden());
    }
}