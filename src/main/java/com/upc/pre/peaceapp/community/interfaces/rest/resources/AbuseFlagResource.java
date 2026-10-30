package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagReason;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;

import java.time.Instant;

public record AbuseFlagResource(Long id, FlagTargetType targetType, Long targetId, Long reporterId, FlagReason reason,
                                String details, FlagStatus status, Long resolvedBy, String resolutionNote,
                                Instant createdAt, Instant resolvedAt) {
}
