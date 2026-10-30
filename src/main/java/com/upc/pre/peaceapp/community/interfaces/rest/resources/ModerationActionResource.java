package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.ModerationActionType;

import java.time.Instant;

public record ModerationActionResource(Long id, Long moderatorId, ModerationActionType type,
                                       FlagTargetType targetType, Long targetId, String note, Instant createdAt) {
}
