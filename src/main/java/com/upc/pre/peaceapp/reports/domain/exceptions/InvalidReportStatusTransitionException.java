package com.upc.pre.peaceapp.reports.domain.exceptions;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;

public class InvalidReportStatusTransitionException extends RuntimeException {
    public InvalidReportStatusTransitionException(ReportStatus from, ReportStatus to) {
        super("No se puede cambiar el estado de " + from + " a " + to);
    }
}
