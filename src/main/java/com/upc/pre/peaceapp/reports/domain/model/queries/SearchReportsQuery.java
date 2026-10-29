package com.upc.pre.peaceapp.reports.domain.model.queries;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportFilter;
import org.springframework.data.domain.Pageable;

/**
 * Listado paginado. {@code reporterId} filtra por perfil; {@code mineEmail} lo resuelve a partir del
 * correo de quien consulta ("mis reportes"). Ambos son opcionales.
 */
public record SearchReportsQuery(ReportFilter filter, String text, Long reporterId, String mineEmail,
                                 Pageable pageable) {
}
