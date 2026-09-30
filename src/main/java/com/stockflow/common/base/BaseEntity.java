package com.stockflow.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Base class for all JPA entities. Provides audit fields and an optimistic
 * locking version counter so concurrent edits collide predictably rather than
 * silently overwriting each other.
 *
 * <p>Subclasses get createdAt / updatedAt / createdBy / updatedBy automatically
 * through {@link org.springframework.data.jpa.domain.support.AuditingEntityListener},
 * which requires {@link org.springframework.data.jpa.repository.config.EnableJpaAuditing}
 * (declared on the application class) and an {@code AuditorAware} bean that
 * resolves the current user's email from the {@code SecurityContext}.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        // Identity is determined by the subclass id, which is only safe to read
        // after a persist call. Use Objects.equals to tolerate null ids on
        // not-yet-persisted entities.
        Object thatId = that.entityId();
        return entityId() != null && Objects.equals(entityId(), thatId);
    }

    @Override
    public int hashCode() {
        // Consistent with equals: when id is null we fall back to the class
        // hash, which matches the JPA recommendation for transient entities.
        return entityId() == null ? getClass().hashCode() : Objects.hash(entityId());
    }

    /**
     * Subclasses must expose their primary key.
     */
    public abstract Object entityId();
}
