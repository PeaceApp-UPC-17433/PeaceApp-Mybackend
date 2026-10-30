package com.upc.pre.peaceapp.community.domain.model.commands;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;

public record CastVoteCommand(Long reportId, String requesterEmail, VoteType type) {
}
