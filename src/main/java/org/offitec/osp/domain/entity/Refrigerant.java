package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refrigerant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Refrigerant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refrigerant_seq_gen")
    @SequenceGenerator(name = "refrigerant_seq_gen", sequenceName = "osp_refrigerant_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    // Name understood by the refrigerant-property engine (CoolProp). Null when CoolProp can't
    // model this fluid yet; ratings on such refrigerants are imported but flagged non-calculable.
    @Column(name = "coolprop_name")
    private String coolpropName;

    // Source property library in FSS3 (e.g. "ASEREP"), kept for traceability.
    @Column(name = "library")
    private String library;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean deleted = false;
}
