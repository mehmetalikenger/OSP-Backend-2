package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_details")
public class ProjectDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_details_seq_gen")
    @SequenceGenerator(name = "project_details_seq_gen", sequenceName = "osp_project_details_sequence")
    private Long id;

    @JoinColumn(name = "project_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @JoinColumn(name = "custom_calc_vals_id", nullable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private CustomCalculationValues customCalculationValues;

    @JoinColumn(name = "calc_output_vals_id")
    @OneToOne(fetch = FetchType.LAZY)
    private CalculationOutputValues calculationOutputValues;

    @Column(nullable = false)
    private String pdfUrl;
}
