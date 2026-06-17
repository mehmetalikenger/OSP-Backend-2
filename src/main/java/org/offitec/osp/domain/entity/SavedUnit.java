package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_unit", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "unit_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saved_unit_seq_gen")
    @SequenceGenerator(name = "saved_unit_seq_gen", sequenceName = "osp_saved_unit_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime savedAt;
}
