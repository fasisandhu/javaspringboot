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

}