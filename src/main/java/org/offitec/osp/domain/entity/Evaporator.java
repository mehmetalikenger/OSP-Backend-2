package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.EvaporatorType;

@Entity
@Table(name = "evaporator")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaporator {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evap_seq_gen")
    @SequenceGenerator(name = "evap_seq_gen", sequenceName = "osp_evap_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = true)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private EvaporatorType type;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean deleted = false;
}
