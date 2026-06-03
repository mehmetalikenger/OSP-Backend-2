package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq_gen")
    @SequenceGenerator(name = "product_seq_gen", sequenceName = "osp_product_sequence", allocationSize = 50)
    private Long id;

    private String brand = "OffiTec";
    private String series;

    @Column(nullable = false)
    private String model;

    @JoinColumn(name = "category_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @JoinColumn(name = "type_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnitType unitType;

    @JoinColumn(name = "product_details_id")
    @OneToMany(fetch = FetchType.LAZY)
    private ProductDetails productDetails;
}
