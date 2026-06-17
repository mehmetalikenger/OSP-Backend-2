package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.Mod;

@Entity
@Table(
    name = "unit_details",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"unit_id", "mod"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnitDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unit_details_seq_gen")
    @SequenceGenerator(name = "unit_details_seq_gen", sequenceName = "osp_unit_details_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

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
