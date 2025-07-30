package com.redmath.jobportal;

import com.redmath.jobportal.application.controller.ApplicatonController;
import com.redmath.jobportal.application.dto.ApplicationRecruiterDto;
import com.redmath.jobportal.application.dto.ApplicationUserDto;
import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.application.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicatonController.class)
@EnableMethodSecurity
public class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    private ApplicationUserDto sampleDto() {
        return new ApplicationUserDto(1L, 101L, "Java Developer","Tech Corp");
    }

    private Application sampleApplication() {
        return Application.builder()
                .id(1L)
                .build();
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplyToJob_AsApplicant_Success() throws Exception {
        Mockito.when(applicationService.applyToJob(eq(101L), any())).thenReturn(sampleApplication());

        mockMvc.perform(post("/api/v1/application/101").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testApplyToJob_AsEmployer_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/application/101"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testMyApplications_AsApplicant_Success() throws Exception {
        Mockito.when(applicationService.getApplicationsByUser(any()))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/v1/application/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobTitle").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testGetApplicationsForAllJobs_AsEmployer_Success() throws Exception {
        ApplicationRecruiterDto recruiterDto = new ApplicationRecruiterDto(1L, 101L, "Java Developer", "Tech Corp", "Applicant", "applicant@example.com");
        Mockito.when(applicationService.getApplicationsForAllPostedJobs(any()))
                .thenReturn(List.of(recruiterDto));

        mockMvc.perform(get("/api/v1/application/recruiter/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$[0].applicantEmail").value("applicant@example.com"));
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void testGetApplicationsForSpecificJob_AsEmployer_Success() throws Exception {
        ApplicationRecruiterDto recruiterDto = new ApplicationRecruiterDto(1L, 101L, "Java Developer", "Tech Corp", "Applicant","applicant@example.com");
        Mockito.when(applicationService.getApplicationsForSpecificPostedJob(any(), eq(101L)))
                .thenReturn(List.of(recruiterDto));

        mockMvc.perform(get("/api/v1/application/recruiter/job/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(101))
                .andExpect(jsonPath("$[0].applicantEmail").value("applicant@example.com"));
    }

    @Test
    @WithMockUser(username = "applicant@example.com", roles = {"APPLICANT"})
    void testApplicantCannotAccessEmployerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/application/recruiter/all").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
