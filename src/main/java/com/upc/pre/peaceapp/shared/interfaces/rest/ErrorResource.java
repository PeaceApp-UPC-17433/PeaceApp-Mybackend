package com.upc.pre.peaceapp.shared.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Formato único de error: mensaje general y, si aplica, mensaje por campo. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResource(String message, Map<String, String> errors) {
    public static ErrorResource of(String message) {
        return new ErrorResource(message, Map.of());
    }
}
