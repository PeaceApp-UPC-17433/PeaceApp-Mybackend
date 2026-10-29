package com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.BoundingBox;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

/** Traduce los criterios de consulta del dominio a predicados JPA. */
public final class ReportSpecifications {
    private ReportSpecifications() {
    }

    public static Specification<Report> matching(ReportFilter filter, String text, Long reporterId, BoundingBox box) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.category() != null) predicates.add(cb.equal(root.get("category"), filter.category()));
            if (filter.severity() != null) predicates.add(cb.equal(root.get("severity"), filter.severity()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            if (filter.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), filter.to()));
            if (reporterId != null) predicates.add(cb.equal(root.get("reporterId"), reporterId));
            if (text != null && !text.isBlank()) {
                var pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("detail")), pattern, '\\'),
                        cb.like(cb.lower(root.get("address")), pattern, '\\')));
            }
            if (box != null) {
                var location = root.get("location");
                predicates.add(cb.between(location.get("latitude"), box.minLat(), box.maxLat()));
                predicates.add(cb.between(location.get("longitude"), box.minLng(), box.maxLng()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
