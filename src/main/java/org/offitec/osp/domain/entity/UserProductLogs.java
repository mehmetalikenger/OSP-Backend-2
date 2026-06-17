package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.UserAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_product_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProductLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_product_logs_seq_gen")
    @SequenceGenerator(name = "user_product_logs_seq_gen", sequenceName = "osp_user_product_logs_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserAction action;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
