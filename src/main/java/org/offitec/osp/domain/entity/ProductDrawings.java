package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_drawings")
public class ProductDrawings {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_drawings_seq_gen")
    @SequenceGenerator(name = "product_drawings_seq_gen", sequenceName = "osp_product_drawings_sequence")
    private Long id;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @Column(nullable = false)
    private String url;
}
