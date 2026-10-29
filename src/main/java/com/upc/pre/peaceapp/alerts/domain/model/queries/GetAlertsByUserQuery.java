package com.upc.pre.peaceapp.alerts.domain.model.queries;

import org.springframework.data.domain.Pageable;

public record GetAlertsByUserQuery(Long userId, String requesterEmail, boolean admin, Pageable pageable) {
}
