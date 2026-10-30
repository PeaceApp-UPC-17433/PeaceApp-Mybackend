package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.commands.CastVoteCommand;
import com.upc.pre.peaceapp.community.domain.model.commands.RetractVoteCommand;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteSummary;

public interface VoteCommandService {
    /** Crea o cambia el voto del usuario. Si las confirmaciones netas llegan al umbral, avisa a reportes. */
    VoteSummary handle(CastVoteCommand command);

    /** Idempotente: retirar un voto que no existe no es un error. */
    VoteSummary handle(RetractVoteCommand command);
}
