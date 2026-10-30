package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.commands.FlagContentCommand;
import com.upc.pre.peaceapp.community.domain.model.commands.ResolveAbuseFlagCommand;

public interface ModerationCommandService {
    AbuseFlag handle(FlagContentCommand command);

    /** Solo administradores. Deja constancia en el historial de moderación. */
    AbuseFlag handle(ResolveAbuseFlagCommand command);
}
