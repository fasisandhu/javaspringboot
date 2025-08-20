package com.redmath.jobportal.job.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String company;

    private boolean remote;

    private double salary;

    @Column(nullable = false)
    private String postedBy;

    // Copy constructor
    public Job(Job other) {
        this.id = other.id;
        this.title = other.title;
        this.description = other.description;
        this.company = other.company;
        this.remote = other.remote;
        this.salary = other.salary;
        this.postedBy = other.postedBy;
    }
}
