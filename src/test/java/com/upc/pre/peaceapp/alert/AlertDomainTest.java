package com.upc.pre.peaceapp.alert;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportSeverity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertDomainTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void defaultSettingsAskForEverythingWithinOneKilometer() {
        var settings = AlertSettings.defaultsFor(1L);
        assertThat(settings.getRadiusMeters()).isEqualTo(1000);
        assertThat(settings.wantsAlertFor(ReportCategory.THEFT, ReportSeverity.LOW, NOW)).isTrue();
        assertThat(settings.hasFreshLocation(NOW)).isFalse();
    }

    @Test
    void settingsFilterByTypeSeverityAndMute() {
        var settings = AlertSettings.defaultsFor(1L);
        settings.configure(500, ReportSeverity.HIGH, Set.of(ReportCategory.LIGHTING), NOW.plus(Duration.ofHours(1)));

        assertThat(settings.wantsAlertFor(ReportCategory.THEFT, ReportSeverity.CRITICAL, NOW)).isFalse(); // silenciado
        assertThat(settings.wantsAlertFor(ReportCategory.THEFT, ReportSeverity.CRITICAL, NOW.plus(Duration.ofHours(2)))).isTrue();
        assertThat(settings.wantsAlertFor(ReportCategory.THEFT, ReportSeverity.MEDIUM, NOW.plus(Duration.ofHours(2)))).isFalse(); // severidad baja
        assertThat(settings.wantsAlertFor(ReportCategory.LIGHTING, ReportSeverity.HIGH, NOW.plus(Duration.ofHours(2)))).isFalse(); // tipo ignorado
    }

    @Test
    void settingsRejectRadiusOutOfRange() {
        var settings = AlertSettings.defaultsFor(1L);
        assertThatThrownBy(() -> settings.configure(50, ReportSeverity.LOW, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings.configure(20_001, ReportSeverity.LOW, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void locationGoesStaleAfterOneDay() {
        var settings = AlertSettings.defaultsFor(1L);
        settings.moveTo(GeoPoint.of(-12, -77), NOW);
        assertThat(settings.hasFreshLocation(NOW.plus(Duration.ofHours(23)))).isTrue();
        assertThat(settings.hasFreshLocation(NOW.plus(Duration.ofHours(25)))).isFalse();
    }

    @Test
    void markReadKeepsFirstReadDate() {
        var alert = new Alert(1L, 2L, "Robo", ReportCategory.THEFT, ReportSeverity.HIGH, GeoPoint.of(0, 0), 120);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.UNREAD);
        alert.markRead(NOW);
        alert.markRead(NOW.plusSeconds(60));
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.READ);
        assertThat(alert.getReadAt()).isEqualTo(NOW);
    }
}
