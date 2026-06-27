package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;

import java.util.List;

@Entity
@Table(name = "unit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unit_seq_gen")
    @SequenceGenerator(name = "unit_seq_gen", sequenceName = "osp_unit_sequence", allocationSize = 50)
    private Long id;

    private String brand = "OffiTec";
    private String series;

    @Column(nullable = false)
    private String model;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private UnitCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitTypeEnum unitType;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean deleted = false;

    // --- Common technical attributes (shared across all modes of the unit) ---

    @Column(name = "compressor_qty")
    private int compressorQty;

    @Column(name = "condenser_qty")
    private int condenserQty;

    @Column(name = "expansion_valve_qty")
    private int expansionValveQty;

    // A unit's refrigerant is derived from its compressor rating via
    // UnitDetails -> TechSpecs -> CompressorRating -> Refrigerant.

    // Chassis is a unit-level selection (shared across all modes).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chassis_id")
    private Chassis chassis;

    // Unit-level compressor selection for heat pumps: cooling and heating share one compressor, so
    // it's chosen once on the model form and propagated to each mode's TechSpecs. Null for chillers
    // (which carry the rating on their single mode's TechSpecs).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compressor_rating_id")
    private CompressorRating compressorRating;

    @Column(name = "fan_pi")
    private double fanPI;

    private double width;
    private double length;
    private double height;

    @Column(name = "fan_type")
    private String fanType;

    @Column(name = "number_of_fans")
    private int numberOfFans;

    @Column(name = "fan_diameter")
    private double fanDiameter;

    @Column(name = "airflow_rate")
    private double airflowRate;

    @Column(name = "discharge_line_diameter")
    private String dischargeLineDiameter;

    @Column(name = "liquid_line_diameter")
    private String liquidLineDiameter;

    @Column(name = "suction_line_diameter")
    private String suctionLineDiameter;

    @Column(name = "gas_tank")
    private double gasTank;

    @Column(name = "water_inlet_connection")
    private String waterInletConnection;

    @Column(name = "water_outlet_connection")
    private String waterOutletConnection;

    // --- Working envelope (used to draw the safe area of the Working Limit graph in the report) ---

    @Column(name = "min_water_inlet")
    private double minWaterInlet;

    @Column(name = "max_water_inlet")
    private double maxWaterInlet;

    @Column(name = "min_water_outlet")
    private double minWaterOutlet;

    @Column(name = "max_water_outlet")
    private double maxWaterOutlet;

    @Column(name = "min_ambient")
    private double minAmbient;

    @Column(name = "max_ambient")
    private double maxAmbient;

    @OneToMany(mappedBy = "unit", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitDetails> unitDetails;

    @OneToMany(mappedBy = "unit", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitAsset> assets;
}