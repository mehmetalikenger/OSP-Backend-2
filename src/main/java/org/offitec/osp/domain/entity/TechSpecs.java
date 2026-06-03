package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tech_specs")
public class TechSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tech_specs_seq_gen")
    @SequenceGenerator(name = "tech_specs_seq_gen", sequenceName = "ops_tech_specs_sequence")
    private Long id;

    private double capacity;
    private int compressorQty;
    private double condenserRequiredDuty;
    private double quietCondenserRequiredDuty;
    private double fanPI;

    @JoinColumn(name = "chassis_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Chassis chassis;

    @JoinColumn(name = "compressor_specs_id")
    @OneToOne(fetch = FetchType.LAZY)
    private CompressorSpecs compressorSpecs;

    private double cop_err;
    private int condenserQty;

    @JoinColumn(name = "condenser_specs_id")
    @OneToOne(fetch = FetchType.LAZY)
    private CondenserSpecs condenserSpecs;

    @JoinColumn(name = "evaporator_specs_id")
    @OneToOne(fetch = FetchType.LAZY)
    private EvaporatorSpecs evaporatorSpecs;

    private double width;
    private double length;
    private double height;
    private int numberOfFans;
    private double fanDiameter;
    private double airflowRate;
    private int expansionValveQty;

    @JoinColumn(name="expansion_valve_specs_id")
    @OneToOne(fetch = FetchType.LAZY)
    private ExpansionValveSpecs expansionValveSpecs;

    @JoinColumn(name = "four_way_reversing_valve_specs_id")
    @OneToOne(fetch = FetchType.LAZY)
    private FourWayReversingValveSpecs fourWayReversingValveSpecs;

    @JoinColumn(name = "refrigerant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Refrigerant refrigerant;

    private String dischargeLineDiameter;
    private String liquidLineDiameter;
    private String suctionLineDiameter;
    private double gasTank;
}
