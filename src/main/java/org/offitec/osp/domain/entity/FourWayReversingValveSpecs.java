package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "four_way_reversing_valve_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FourWayReversingValveSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "four_way_reversing_valve_specs_seq_gen")
    @SequenceGenerator(name = "four_way_reversing_valve_specs_seq_gen", sequenceName = "osp_four_way_reversing_valve_specs_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "four_way_reversing_valve_id", nullable = false)
    private FourWayReversingValve fourWayReversingValve;

    @Column(nullable = false)
    private double capacity;
}
