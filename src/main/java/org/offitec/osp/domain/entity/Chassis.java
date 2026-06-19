package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chassis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chassis {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chassis_seq_gen")
    @SequenceGenerator(name = "chassis_seq_gen", sequenceName = "osp_chassis_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = true)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean deleted = false;
}
