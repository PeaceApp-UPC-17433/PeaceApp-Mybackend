package com.upc.pre.peaceapp.community.domain.model.commands;

public record AddCommentCommand(Long reportId, String requesterEmail, String text) {
}
