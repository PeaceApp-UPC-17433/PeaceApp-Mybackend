package com.upc.pre.peaceapp.community.domain.model.queries;

import org.springframework.data.domain.Pageable;

public record GetModerationLogQuery(boolean admin, Pageable pageable) {
}
