package com.upc.pre.peaceapp.community.domain.exceptions;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException() {
        super("El comentario no existe");
    }
}
