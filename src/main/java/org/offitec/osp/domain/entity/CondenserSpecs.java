package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "condenser_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CondenserSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cond_specs_seq_gen")
    @SequenceGenerator(name = "cond_specs_seq_gen", sequenceName = "osp_cond_specs_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cond_id", nullable = false)
    private Condenser condenser;

    @Column(nullable = false)
    private double capacity;
}
