package com.upc.pre.peaceapp.notifications.domain.model.aggregates;

import com.upc.pre.peaceapp.notifications.domain.model.valueobjects.DevicePlatform;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

/** Dispositivo donde recibir avisos push. Un token pertenece a un solo usuario a la vez. */
@Getter
@Entity
@Table(name = "device_registrations", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class DeviceRegistration extends AuditableAbstractAggregateRoot<DeviceRegistration> {
    public static final int TOKEN_MAX = 512;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "token", nullable = false, length = TOKEN_MAX)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private DevicePlatform platform;

    protected DeviceRegistration() {
    }

    public DeviceRegistration(Long profileId, String token, DevicePlatform platform) {
        if (profileId == null) throw new IllegalArgumentException("El dispositivo necesita un usuario");
        if (platform == null) throw new IllegalArgumentException("La plataforma es obligatoria");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("El token del dispositivo es obligatorio");
        if (token.length() > TOKEN_MAX) throw new IllegalArgumentException("El token del dispositivo es demasiado largo");
        this.profileId = profileId;
        this.token = token.trim();
        this.platform = platform;
    }

    /** El mismo dispositivo inició sesión con otra cuenta: pasa a pertenecer a la nueva. */
    public void reassign(Long profileId, DevicePlatform platform) {
        this.profileId = profileId;
        this.platform = platform;
    }
}
