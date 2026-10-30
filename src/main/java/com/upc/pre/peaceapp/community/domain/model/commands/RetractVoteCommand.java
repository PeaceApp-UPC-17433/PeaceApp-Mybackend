package com.upc.pre.peaceapp.community.domain.model.commands;

public record RetractVoteCommand(Long reportId, String requesterEmail) {
}
