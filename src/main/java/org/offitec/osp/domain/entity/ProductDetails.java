package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.Mod;

@Entity
@Table(
    name = "product_details",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "mod"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_details_seq_gen")
    @SequenceGenerator(name = "product_details_seq_gen", sequenceName = "osp_product_details_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mod mod;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "def_calc_vals_id")
    private DefaultCalculationValues defCalcValues;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "tech_specs_id")
    private TechSpecs techSpecs;
}
