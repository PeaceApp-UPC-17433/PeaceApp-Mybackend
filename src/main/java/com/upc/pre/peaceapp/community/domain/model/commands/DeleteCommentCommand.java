package com.upc.pre.peaceapp.community.domain.model.commands;

public record DeleteCommentCommand(Long commentId, String requesterEmail, boolean admin) {
}
