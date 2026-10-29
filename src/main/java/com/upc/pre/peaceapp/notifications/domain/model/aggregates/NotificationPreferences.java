package com.upc.pre.peaceapp.notifications.domain.model.aggregates;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.NotificationKind;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/** Canales activos y tipos de aviso desactivados de un usuario. Por defecto todo está activo. */
@Getter
@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(columnNames = "profile_id"))
public class NotificationPreferences extends AuditableAbstractAggregateRoot<NotificationPreferences> {
    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_disabled_kinds", joinColumns = @JoinColumn(name = "preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 30)
    private Set<NotificationKind> disabledKinds = new HashSet<>();

    protected NotificationPreferences() {
    }

    public static NotificationPreferences defaultsFor(Long profileId) {
        if (profileId == null) throw new IllegalArgumentException("Las preferencias necesitan un usuario");
        var preferences = new NotificationPreferences();
        preferences.profileId = profileId;
        preferences.inAppEnabled = true;
        preferences.pushEnabled = true;
        return preferences;
    }

    public void configure(boolean inAppEnabled, boolean pushEnabled, Set<NotificationKind> disabledKinds) {
        this.inAppEnabled = inAppEnabled;
        this.pushEnabled = pushEnabled;
        this.disabledKinds = disabledKinds == null ? new HashSet<>() : new HashSet<>(disabledKinds);
    }

    public boolean allows(NotificationKind kind) {
        return !disabledKinds.contains(kind);
    }
}
