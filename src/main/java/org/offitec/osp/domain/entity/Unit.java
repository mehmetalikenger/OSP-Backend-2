package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;

import java.util.List;

@Entity
@Table(
    name = "unit",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"brand", "series", "model"})
    }
)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private UnitCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitTypeEnum unitType;

    // --- Common technical attributes (shared across all modes of the unit) ---

    @Column(name = "compressor_qty")
    private int compressorQty;

    @Column(name = "condenser_qty")
    private int condenserQty;

    @Column(name = "expansion_valve_qty")
    private int expansionValveQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refrigerant_id")
    private Refrigerant refrigerant;

    @Column(name = "fan_pi")
    private double fanPI;

    private double width;
    private double length;
    private double height;

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

    @OneToMany(mappedBy = "unit", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitDetails> unitDetails;
}
