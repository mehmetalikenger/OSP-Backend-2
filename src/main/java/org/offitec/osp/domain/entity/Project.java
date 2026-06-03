package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq_gen")
    @SequenceGenerator(name = "project_seq_gen", sequenceName = "osp_project_sequence")
    private Long id;

    @Column(nullable = false)
    private String name;

    private String company;

    private String address;

    private String country;

    private String city;

    private String phone;
}
