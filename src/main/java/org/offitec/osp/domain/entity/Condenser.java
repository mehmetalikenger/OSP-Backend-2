package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "condenser")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Condenser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cond_seq_gen")
    @SequenceGenerator(name = "cond_seq_gen", sequenceName = "osp_cond_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = true)
    private String brand;

    @Column(nullable = false)
    private String model;
}
