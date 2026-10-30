package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import java.time.Instant;

public record CommentResource(Long id, Long reportId, Long authorId, String text, Instant createdAt) {
}
