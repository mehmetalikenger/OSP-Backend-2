package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "compressor_type")
public class CompressorType {

    private enum Type {
        RC,
        SC,
        SCR,
        ISCR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Type type;
}
