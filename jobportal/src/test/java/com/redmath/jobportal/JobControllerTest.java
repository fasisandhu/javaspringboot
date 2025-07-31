package com.redmath.jobportal;

import com.redmath.jobportal.exceptions.JobNotFoundException;
import com.redmath.jobportal.exceptions.UnauthorizedJobAccessException;
import com.redmath.jobportal.job.controller.JobController;
import com.redmath.jobportal.job.model.Job;
import com.redmath.jobportal.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@EnableMethodSecurity
public class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    private Job sampleJob() {
        return Job.builder()
                .id(1L)
                .title("Java Developer")
                .description("Spring Boot Developer")
                .company("Redmath")
                .remote(true)
                .salary(100000)
                .postedBy("employer@example.com")
                .build();
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testCreateJob_AsEmployer_Success() throws Exception {
        Mockito.when(jobService.createJob(any(Job.class), any())).thenReturn(sampleJob());

        mockMvc.perform(post("/api/v1/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java Developer\",\"description\":\"Spring Boot Developer\",\"company\":\"Redmath\",\"remote\":true,\"salary\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testCreateJob_AsApplicant_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java Developer\",\"description\":\"Spring Boot Developer\",\"company\":\"Redmath\",\"remote\":true,\"salary\":100000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testUpdateJob_OwnedJob_Success() throws Exception {
        Mockito.when(jobService.updateJob(eq(1L), any(Job.class), any())).thenReturn(sampleJob());

        mockMvc.perform(put("/api/v1/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"description\":\"Updated Description\",\"company\":\"Redmath\",\"remote\":false,\"salary\":120000}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testUpdateJob_AsApplicant_Forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testDeleteJob_OwnedJob_Success() throws Exception {
        Mockito.doNothing().when(jobService).deleteJob(eq(1L), any());

        mockMvc.perform(delete("/api/v1/jobs/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testDeleteJob_NotOwnedJob_Forbidden() throws Exception {
        Mockito.doThrow(new UnauthorizedJobAccessException("You are not authorized to delete this job"))
                .when(jobService).deleteJob(eq(1L), any());

        mockMvc.perform(delete("/api/v1/jobs/1").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testGetAllJobs_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testGetJobById_Found() throws Exception {
        // Mocking the service to return a job when the ID is found
        Mockito.when(jobService.getJobById(1L)).thenReturn(sampleJob());

        mockMvc.perform(get("/api/v1/jobs/1")  // Change to GET instead of POST
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }


    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testGetJobById_NotFound() throws Exception {
        // Mocking the service to throw a JobNotFoundException
        Mockito.when(jobService.getJobById(2L)).thenThrow(new JobNotFoundException("Job with ID 2 not found"));
        mockMvc.perform(get("/api/v1/jobs/2")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
