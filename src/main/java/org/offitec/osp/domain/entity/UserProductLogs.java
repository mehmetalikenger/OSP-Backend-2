package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_product_logs")
public class UserProductLogs {

    private enum Action {
        VIEW,
        CALCULATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_product_logs_seq_gen")
    @SequenceGenerator(name = "user_product_logs_seq_gen", sequenceName = "osp_user_product_logs_sequence")
    private Long id;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;
}
