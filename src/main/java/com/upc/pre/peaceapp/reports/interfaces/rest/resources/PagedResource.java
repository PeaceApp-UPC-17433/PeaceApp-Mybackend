package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PagedResource<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <E, T> PagedResource<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResource<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
