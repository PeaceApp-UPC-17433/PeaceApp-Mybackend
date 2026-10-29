package com.upc.pre.peaceapp.reports.domain.exceptions;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException() {
        super("Reporte no encontrado");
    }
}
