package com.upc.pre.peaceapp.alerts.domain.model.queries;

import org.springframework.data.domain.Pageable;

public record GetAlertsQuery(String requesterEmail, boolean unreadOnly, Pageable pageable) {
}
