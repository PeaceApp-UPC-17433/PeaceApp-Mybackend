package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;
import jakarta.validation.constraints.NotNull;

public record CastVoteResource(@NotNull(message = "El tipo de voto es obligatorio") VoteType type) {
}
