package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.AssetType;

@Entity
@Table(name = "product_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_asset_seq_gen")
    @SequenceGenerator(name = "product_asset_seq_gen", sequenceName = "osp_product_asset_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(nullable = false)
    private String url;
}
