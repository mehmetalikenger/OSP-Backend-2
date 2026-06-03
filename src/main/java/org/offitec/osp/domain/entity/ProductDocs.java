package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_docs")
public class ProductDocs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_docs_seq_gen")
    @SequenceGenerator(name = "product_docs_seq_gen", sequenceName = "osp_product_docs_sequence")
    private Long id;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @Column(nullable = false)
    private String url;
}
