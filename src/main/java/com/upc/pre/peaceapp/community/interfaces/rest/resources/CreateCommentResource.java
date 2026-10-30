package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentResource(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String text) {
}
