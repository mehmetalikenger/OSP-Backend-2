package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImages {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_images_seq_gen")
    @SequenceGenerator(name = "product_images_seq_gen", sequenceName = "osp_product_images_sequence")
    private Long id;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @Column(nullable = false)
    private String url;
}
