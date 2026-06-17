package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.AssetType;

@Entity
@Table(name = "unit_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnitAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unit_asset_seq_gen")
    @SequenceGenerator(name = "unit_asset_seq_gen", sequenceName = "osp_unit_asset_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(nullable = false)
    private String url;
}
