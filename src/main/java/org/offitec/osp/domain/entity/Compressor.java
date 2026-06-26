package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.CompressorKind;

@Entity
@Table(name = "compressor")
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

    // Maximum Operating Current and Locked Rotor Amperage (A).
    // Nullable so rows created before these columns existed (NULL) still hydrate.
    @Column(name = "moc")
    private Double moc;

    @Column(name = "lra")
    private Double lra;

    // Refrigerant is a property of the compressor (selected in the add/edit compressor
    // form, after LRA). A unit's refrigerant is derived from its compressor. EAGER so the
    // compressor lists returned directly as JSON include it without a lazy-init error.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "refrigerant_id")
    private Refrigerant refrigerant;

    // ---- Frascold catalogue data, populated by the frdata.mdb importer. ----
    // These are nullable so admin-created compressors (which don't have them) still persist.

    // Original FSS3 tblSRClist primary key. Unique per imported variant — the model name is NOT
    // unique (e.g. a three-phase and a single-phase compressor can share one name), so this is the
    // idempotency key for re-running the import.
    @Column(name = "src_key", unique = true)
    private Integer srcKey;

    // Thermodynamic family: "SK" (subcritical: 2nd polynomial variable is condensing temperature)
    // or "TK" (transcritical CO2: 2nd variable is gas-cooler pressure). Drives the engine branch.
    @Column(name = "frascold_type")
    private String frascoldType;

    // FSS3 model-definition key (tblModelDef) and its human description, kept for traceability.
    @Column(name = "mdd_key")
    private Integer mddKey;

    @Column(name = "model_description")
    private String modelDescription;

    // Swept displacement (cm3/rev) and piston count.
    @Column(name = "displacement")
    private Double displacement;

    @Column(name = "piston_count")
    private Integer pistonCount;

    // Maximum discharge gas temperature (deg C) for the family.
    @Column(name = "t_dis_max")
    private Double tDisMax;

    @Column(name = "nominal_hp")
    private Double nominalHp;

    // Oil-cooling duty polynomial coefficients (Q_oil = a + b*x + c*x^2), from tblSRClist.
    @Column(name = "oil_a") private Double oilA;
    @Column(name = "oil_b") private Double oilB;
    @Column(name = "oil_c") private Double oilC;

    // True for rows seeded from frdata.mdb (vs. manually added via the admin form).
    @Column(name = "imported", nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean imported = false;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean deleted = false;
}
