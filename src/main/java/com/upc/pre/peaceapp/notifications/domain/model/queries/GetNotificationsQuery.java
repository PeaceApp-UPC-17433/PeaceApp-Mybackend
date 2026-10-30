package com.upc.pre.peaceapp.notifications.domain.model.queries;

import org.springframework.data.domain.Pageable;

public record GetNotificationsQuery(String requesterEmail, boolean unreadOnly, Pageable pageable) {
}
