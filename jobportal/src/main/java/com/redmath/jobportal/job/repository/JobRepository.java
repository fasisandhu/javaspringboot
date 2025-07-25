package com.redmath.jobportal.job.repository;

import com.redmath.jobportal.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job,Long> {
    List<Job> findByPostedBy(String username);
}
