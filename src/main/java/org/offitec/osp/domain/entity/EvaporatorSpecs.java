package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaporator_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaporatorSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evap_specs_seq_gen")
    @SequenceGenerator(name = "evap_specs_seq_gen", sequenceName = "osp_evap_specs_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evap_id", nullable = false)
    private Evaporator evaporator;

    @Column(nullable = false)
    private double capacity;
}
