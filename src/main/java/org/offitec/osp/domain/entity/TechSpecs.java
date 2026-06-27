package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tech_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TechSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tech_specs_seq_gen")
    @SequenceGenerator(name = "tech_specs_seq_gen", sequenceName = "osp_tech_specs_sequence", allocationSize = 50)
    private Long id;

    // --- Per-mode technical attributes (differ between cooling and heating) ---
    // Capacity/maxCapacity moved to CompressorRating.modeCapacities (per mode). A unit-mode's
    // capacity is the rating's CompressorModeCapacity whose mod == UnitDetails.mod.

    @Column(name = "cop_err")
    private double copErr;

    @Column(name = "condenser_required_duty")
    private double condenserRequiredDuty;

    @Column(name = "quiet_condenser_required_duty")
    private double quietCondenserRequiredDuty;

    // Link to the compressor rating (compressor + refrigerant coefficient set). All calculation
    // goes through the faithful CompressorPerformanceEngine (Frascold cycle or Copeland polynomial).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compressor_rating_id")
    private CompressorRating compressorRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condenser_specs_id")
    private CondenserSpecs condenserSpecs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaporator_specs_id")
    private EvaporatorSpecs evaporatorSpecs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_valve_specs_id")
    private ExpansionValveSpecs expansionValveSpecs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "four_way_reversing_valve_specs_id")
    private FourWayReversingValveSpecs fourWayReversingValveSpecs;
}
