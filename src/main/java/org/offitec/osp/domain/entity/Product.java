package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;

import java.util.List;

@Entity
@Table(
    name = "product",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"brand", "series", "model"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq_gen")
    @SequenceGenerator(name = "product_seq_gen", sequenceName = "osp_product_sequence", allocationSize = 50)
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

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDetails> productDetails;
}
