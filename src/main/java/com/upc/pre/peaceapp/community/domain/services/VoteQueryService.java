package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.queries.GetVoteSummaryQuery;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteSummary;

public interface VoteQueryService {
    VoteSummary handle(GetVoteSummaryQuery query);
}
