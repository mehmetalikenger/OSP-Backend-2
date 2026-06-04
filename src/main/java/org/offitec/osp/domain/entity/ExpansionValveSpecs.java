package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expansion_valve_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpansionValveSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exp_valve_specs_seq_gen")
    @SequenceGenerator(name = "exp_valve_specs_seq_gen", sequenceName = "osp_exp_valve_specs_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exp_valve_id", nullable = false)
    private ExpansionValve expansionValve;

    @Column(nullable = false)
    private double capacity;
}
