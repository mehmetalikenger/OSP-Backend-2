package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_details")
public class ProductDetails {

    private enum Mod {
        COOLING,
        HEATING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq_gen")
    @SequenceGenerator(name = "product_seq_gen", sequenceName = "osp_product_sequence", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mod mod;

    @JoinColumn(name = "def_calc_vals_id")
    @OneToOne
    private DefaultCalculationValues defCalcValues;

    @JoinColumn(name = "tech_specs_id")
    @OneToOne
    private TechSpecs techSpecs;
}
