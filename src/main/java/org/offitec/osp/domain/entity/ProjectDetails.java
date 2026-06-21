package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_details_seq_gen")
    @SequenceGenerator(name = "project_details_seq_gen", sequenceName = "osp_project_details_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // ManyToOne (not OneToOne): the same unit can appear in many projects, and a
    // project can hold many units. A OneToOne here forces a UNIQUE constraint on
    // product_id, which breaks adding the same unit to more than one project.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Unit unit;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "custom_calc_vals_id", nullable = false)
    private CustomCalculationValues customCalculationValues;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "calc_output_vals_id")
    private CalculationOutputValues calculationOutputValues;

    private String pdfUrl;
}
