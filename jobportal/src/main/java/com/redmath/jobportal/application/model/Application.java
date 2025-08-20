package com.redmath.jobportal.application.model;

import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.job.model.Job;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    private Job job;

    @ManyToOne(optional = false)
    private User user;

    public Job getJob() {
        return new Job(this.job);
    }

    public User getUser() {
        return new User(this.user);
    }
}
