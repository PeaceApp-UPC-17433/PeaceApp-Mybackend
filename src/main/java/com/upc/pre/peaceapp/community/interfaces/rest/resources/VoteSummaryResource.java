package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;

/** {@code myVote} es nulo si quien consulta no votó. */
public record VoteSummaryResource(long confirmations, long disputes, long net, VoteType myVote,
                                  int verificationThreshold) {
}
