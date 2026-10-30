package com.upc.pre.peaceapp.community.domain.model.commands;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagReason;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;

public record FlagContentCommand(FlagTargetType targetType, Long targetId, String requesterEmail, FlagReason reason,
                                 String details) {
}
