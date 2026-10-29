package com.upc.pre.peaceapp.report;

import com.upc.pre.peaceapp.reports.domain.exceptions.InvalidReportStatusTransitionException;
import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportDomainTest {

    private static Report newReport() {
        return new Report(1L, "Robo", "Detalle", ReportCategory.THEFT, null, null, GeoPoint.of(-12.05, -77.04), "Av. Lima", null);
    }

    @Test
    void newReportStartsPendingWithDefaults() {
        var report = newReport();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getSeverity()).isEqualTo(ReportSeverity.MEDIUM);
        assertThat(report.getOccurredAt()).isNotNull();
    }

    @Test
    void reportRejectsInvalidData() {
        var point = GeoPoint.of(0, 0);
        assertThatThrownBy(() -> new Report(null, "t", null, ReportCategory.OTHER, null, null, point, "a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Report(1L, " ", null, ReportCategory.OTHER, null, null, point, "a", null))
                .hasMessage("El título es obligatorio");
        assertThatThrownBy(() -> new Report(1L, "t", null, ReportCategory.OTHER, null,
                Instant.now().plus(1, ChronoUnit.DAYS), point, "a", null))
                .hasMessage("La fecha del incidente no puede estar en el futuro");
    }

    @Test
    void editChangesOnlyProvidedFields() {
        var report = newReport();
        report.edit("Nuevo", null, null, ReportSeverity.HIGH, null, null, null, null);
        assertThat(report.getTitle()).isEqualTo("Nuevo");
        assertThat(report.getDetail()).isEqualTo("Detalle");
        assertThat(report.getSeverity()).isEqualTo(ReportSeverity.HIGH);
        assertThat(report.getAddress()).isEqualTo("Av. Lima");
    }

    @Test
    void statusFollowsAllowedTransitions() {
        var report = newReport();
        report.changeStatus(ReportStatus.VERIFIED);
        report.changeStatus(ReportStatus.RESOLVED);
        assertThatThrownBy(() -> report.changeStatus(ReportStatus.PENDING))
                .isInstanceOf(InvalidReportStatusTransitionException.class);
    }

    @Test
    void geoPointValidatesRanges() {
        assertThatThrownBy(() -> GeoPoint.of(91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeoPoint.of(0, -181)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeoPoint.of(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void distanceAndCircleAreaAreCoherent() {
        var center = GeoPoint.of(-12.0464, -77.0428);
        var near = GeoPoint.of(-12.0474, -77.0428); // ~111 m al sur
        var far = GeoPoint.of(-12.0664, -77.0428);  // ~2.2 km al sur
        assertThat(center.distanceMetersTo(near)).isBetween(100.0, 125.0);

        var area = SearchArea.ofCircle(center, 500);
        assertThat(area.contains(near)).isTrue();
        assertThat(area.contains(far)).isFalse();
        assertThat(area.box().contains(near)).isTrue();
    }

    @Test
    void boundingBoxRejectsInvertedBounds() {
        assertThatThrownBy(() -> new BoundingBox(10, 5, 0, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void riskLevelGrowsWithReportCount() {
        assertThat(RiskLevel.fromCount(1)).isEqualTo(RiskLevel.LOW);
        assertThat(RiskLevel.fromCount(3)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromCount(6)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromCount(10)).isEqualTo(RiskLevel.CRITICAL);
    }
}
