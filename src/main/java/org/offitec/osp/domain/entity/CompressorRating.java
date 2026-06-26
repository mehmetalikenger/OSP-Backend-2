package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Performance rating of one {@link Compressor} on one {@link Refrigerant}, as published by
 * Frascold (EN12900). A compressor supports many refrigerants, each with its own coefficient
 * sets, reference condition, operating envelope and inverter range — so there is one row per
 * (compressor, refrigerant) pairing. Seeded from frdata.mdb by the importer.
 *
 * <p>Coefficient sets are stored as 10-element {@code jsonb} arrays (EN12900 C1..C10) instead of
 * 60+ flat columns. The calculation engine evaluates them against the refrigerant cycle.</p>
 */
@Entity
@Table(name = "compressor_rating",
        uniqueConstraints = @UniqueConstraint(columnNames = {"compressor_id", "refrigerant_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompressorRating {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comp_rating_seq_gen")
    @SequenceGenerator(name = "comp_rating_seq_gen", sequenceName = "osp_comp_rating_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compressor_id", nullable = false)
    private Compressor compressor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "refrigerant_id", nullable = false)
    private Refrigerant refrigerant;

    // ---- EN12900 base polynomials (C1..C10) ----
    // Reference cooling capacity Q(Te, X) where X = condensing temp (SK) or gas-cooler pressure (TK).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cap_coeffs", nullable = false, columnDefinition = "jsonb")
    private double[] capCoeffs;

    // Absorbed power P(Te, X).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "power_coeffs", nullable = false, columnDefinition = "jsonb")
    private double[] powerCoeffs;

    // Suction mass flow polynomial. Used directly for TK; for SK mass flow is derived
    // thermodynamically, but the coefficients are retained when present.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mass_coeffs", columnDefinition = "jsonb")
    private double[] massCoeffs;

    // ---- Frequency-correction polynomials (inverter / 50-60 Hz). Null when not provided. ----
    // Factor = 1 + d * poly(...), d = f/50 - 1. Applied multiplicatively to capacity / power / current.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "freq_cap_coeffs", columnDefinition = "jsonb")
    private double[] freqCapCoeffs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "freq_power_coeffs", columnDefinition = "jsonb")
    private double[] freqPowerCoeffs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "freq_current_coeffs", columnDefinition = "jsonb")
    private double[] freqCurrentCoeffs;

    // ---- Reference rating condition (the datum the polynomials were rated at) ----
    // ohRef = reference suction superheat (K); the sentinel 999 means "rated at a fixed suction-gas
    // temperature (taspRef) instead of a fixed superheat". scRef = reference subcooling (K);
    // tliqRef = reference liquid temperature (999 = saturated liquid / bubble point).
    @Column(name = "oh_ref")   private double ohRef;
    @Column(name = "tasp_ref") private double taspRef;
    @Column(name = "sc_ref")   private double scRef;
    @Column(name = "tliq_ref") private double tliqRef;

    // ---- Inverter operating range (Hz / rpm). Null/zero for fixed-speed ratings. ----
    @Column(name = "min_frequency") private Double minFrequency;
    @Column(name = "max_frequency") private Double maxFrequency;
    @Column(name = "min_speed")     private Double minSpeed;
    @Column(name = "max_speed")     private Double maxSpeed;

    // TK transcritical ratings use the mass-flow polynomial directly (UseMassCap-style behaviour).
    @Column(name = "use_mass_cap", nullable = false)
    private boolean useMassCap;

    @Column(name = "eco", nullable = false)
    private boolean eco;

    // Operating envelope as a polygon of [Te, X] points (Te = evaporating temp; X = condensing temp
    // for SK or gas-cooler pressure for TK). Used for in-range limit checks.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "envelope", columnDefinition = "jsonb")
    private double[][] envelope;

    // False when this rating's refrigerant has no CoolProp mapping yet — imported for completeness
    // but the engine will refuse to compute it until the property mapping exists.
    @Column(name = "calculable", nullable = false)
    @org.hibernate.annotations.ColumnDefault("true")
    private boolean calculable = true;
}
