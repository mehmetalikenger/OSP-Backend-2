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

    private double capacity;

    @Column(name = "compressor_qty")
    private int compressorQty;

    @Column(name = "condenser_required_duty")
    private double condenserRequiredDuty;

    @Column(name = "quiet_condenser_required_duty")
    private double quietCondenserRequiredDuty;

    @Column(name = "fan_pi")
    private double fanPI;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chassis_id")
    private Chassis chassis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compressor_specs_id")
    private CompressorSpecs compressorSpecs;

    @Column(name = "cop_err")
    private double copErr;

    @Column(name = "condenser_qty")
    private int condenserQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condenser_specs_id")
    private CondenserSpecs condenserSpecs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaporator_specs_id")
    private EvaporatorSpecs evaporatorSpecs;

    private double width;
    private double length;
    private double height;

    @Column(name = "number_of_fans")
    private int numberOfFans;

    @Column(name = "fan_diameter")
    private double fanDiameter;

    @Column(name = "airflow_rate")
    private double airflowRate;

    @Column(name = "expansion_valve_qty")
    private int expansionValveQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_valve_specs_id")
    private ExpansionValveSpecs expansionValveSpecs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "four_way_reversing_valve_specs_id")
    private FourWayReversingValveSpecs fourWayReversingValveSpecs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refrigerant_id")
    private Refrigerant refrigerant;

    @Column(name = "discharge_line_diameter")
    private String dischargeLineDiameter;

    @Column(name = "liquid_line_diameter")
    private String liquidLineDiameter;

    @Column(name = "suction_line_diameter")
    private String suctionLineDiameter;

    @Column(name = "gas_tank")
    private double gasTank;
}
