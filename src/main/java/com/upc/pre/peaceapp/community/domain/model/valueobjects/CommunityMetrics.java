package com.upc.pre.peaceapp.community.domain.model.valueobjects;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportQualityMetrics;

/** Indicadores para el panel de administración: calidad de los reportes y actividad de la comunidad. */
public record CommunityMetrics(ReportQualityMetrics reports, long comments, long confirmations, long disputes,
                               long flagsOpen, long flagsDismissed, long flagsActionTaken) {
    public long flagsTotal() {
        return flagsOpen + flagsDismissed + flagsActionTaken;
    }
}
