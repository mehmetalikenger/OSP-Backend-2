package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expansion_valve")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpansionValve {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exp_valve_seq_gen")
    @SequenceGenerator(name = "exp_valve_seq_gen", sequenceName = "osp_exp_valve_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}
