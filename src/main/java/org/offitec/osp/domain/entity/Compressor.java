package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.CompressorKind;

@Entity
@Table(name = "compressor", uniqueConstraints = {
        @UniqueConstraint(name = "uc_compressor_model", columnNames = {"model"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Compressor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compressor_seq_gen")
    @SequenceGenerator(name = "compressor_seq_gen", sequenceName = "osp_compressor_sequence", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CompressorKind type;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}
