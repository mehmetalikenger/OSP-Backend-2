package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_icons")
public class ProductIcons {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_icons_seq_gen")
    @SequenceGenerator(name = "product_icons_seq_gen", sequenceName = "osp_product_icons_sequence")
    private Long id;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @Column(nullable = false)
    private String url;
}
