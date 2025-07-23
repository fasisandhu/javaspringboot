package com.redmath.jobportal.application.repository;

import com.redmath.jobportal.application.model.Application;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application,Long> {
    List<Application> findByUser(User user);
    List<Application> findByJob(Job job);
}
