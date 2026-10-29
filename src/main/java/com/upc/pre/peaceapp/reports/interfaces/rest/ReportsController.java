package com.upc.pre.peaceapp.reports.interfaces.rest;

import com.upc.pre.peaceapp.reports.domain.model.commands.ChangeReportStatusCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.DeleteReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.queries.*;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.*;
import com.upc.pre.peaceapp.reports.domain.services.ReportCommandService;
import com.upc.pre.peaceapp.reports.domain.services.ReportQueryService;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.*;
import com.upc.pre.peaceapp.reports.interfaces.rest.transform.*;
import com.upc.pre.peaceapp.shared.interfaces.rest.AuthenticatedRequester;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Reportes de incidentes. Todos los endpoints piden token; el usuario que reporta sale del token.
 * Los errores los traduce {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(value = "/api/v1/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reports", description = "Reports Endpoints")
public class ReportsController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MAP_POINTS = 1000;
    /** Propiedades por las que se permite ordenar, con su nombre en el agregado. */
    private static final Map<String, String> SORTABLE = Map.of("occurredAt", "occurredAt", "createdAt", "createdAt");

    private final ReportCommandService commandService;
    private final ReportQueryService queryService;

    public ReportsController(ReportCommandService commandService, ReportQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<ReportResource> createReport(@Valid @RequestBody CreateReportResource resource,
                                                       Authentication authentication) {
        var report = commandService.handle(CreateReportCommandFromResourceAssembler
                .toCommandFromResource(authentication.getName(), resource));
        return ResponseEntity.created(URI.create("/api/v1/reports/" + report.getId()))
                .body(ReportResourceFromEntityAssembler.toResourceFromEntity(report));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<PagedResource<ReportResource>> searchReports(
            @RequestParam(required = false) ReportCategory type,
            @RequestParam(required = false) ReportSeverity severity,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {
        var query = new SearchReportsQuery(new ReportFilter(type, severity, status, from, to), q, userId,
                mine ? authentication.getName() : null, pageable(page, size, sortBy, direction));
        return ResponseEntity.ok(PagedResource.from(queryService.handle(query),
                ReportResourceFromEntityAssembler::toResourceFromEntity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResource> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(ReportResourceFromEntityAssembler
                .toResourceFromEntity(queryService.handle(new GetReportByIdQuery(id))));
    }

    /** Reportes de un perfil; equivale a {@code GET /reports?userId=}. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResource<ReportResource>> getReportsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchReportsQuery(ReportFilter.none(), null, userId, null,
                pageable(page, size, "occurredAt", "desc"));
        return ResponseEntity.ok(PagedResource.from(queryService.handle(query),
                ReportResourceFromEntityAssembler::toResourceFromEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportResource> updateReport(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateReportResource resource,
                                                       Authentication authentication) {
        var report = commandService.handle(UpdateReportCommandFromResourceAssembler.toCommandFromResource(
                id, authentication.getName(), AuthenticatedRequester.isAdmin(authentication), resource));
        return ResponseEntity.ok(ReportResourceFromEntityAssembler.toResourceFromEntity(report));
    }

    /** Es PUT y no PATCH porque la configuración de CORS solo permite GET, POST, PUT y DELETE. */
    @PutMapping("/{id}/status")
    public ResponseEntity<ReportResource> changeStatus(@PathVariable Long id,
                                                       @Valid @RequestBody ChangeReportStatusResource resource,
                                                       Authentication authentication) {
        var report = commandService.handle(new ChangeReportStatusCommand(id,
                AuthenticatedRequester.isAdmin(authentication), resource.status()));
        return ResponseEntity.ok(ReportResourceFromEntityAssembler.toResourceFromEntity(report));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new DeleteReportCommand(id, authentication.getName(),
                AuthenticatedRequester.isAdmin(authentication)));
        return ResponseEntity.noContent().build();
    }

    /**
     * Marcadores del área visible. El área es un rectángulo (minLat, maxLat, minLng, maxLng)
     * o un círculo (lat, lng, radiusMeters), pero no ambos.
     */
    @GetMapping("/map")
    public ResponseEntity<List<MapPointResource>> getMapPoints(
            @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng, @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusMeters,
            @RequestParam(required = false) ReportCategory type,
            @RequestParam(required = false) ReportSeverity severity,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "500") int limit) {
        var area = resolveArea(minLat, maxLat, minLng, maxLng, lat, lng, radiusMeters);
        var query = new GetReportsInAreaQuery(area, new ReportFilter(type, severity, status, from, to),
                Math.min(Math.max(limit, 1), MAX_MAP_POINTS));
        return ResponseEntity.ok(queryService.handle(query).stream()
                .map(ReportResourceFromEntityAssembler::toMapPointFromEntity).toList());
    }

    /** Mapa de calor: reportes activos agrupados en celdas de {@code cellSize} grados. */
    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatCellResource>> getHeatmap(
            @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng, @RequestParam(required = false) Double maxLng,
            @RequestParam(defaultValue = "0.005") double cellSize,
            @RequestParam(defaultValue = "1") int minCount) {
        var query = new GetHeatmapQuery(resolveBox(minLat, maxLat, minLng, maxLng), cellSize, minCount);
        return ResponseEntity.ok(queryService.handle(query).stream()
                .map(HeatCellResourceFromValueObjectAssembler::toResourceFromValueObject).toList());
    }

    static Pageable pageable(int page, int size, String sortBy, String direction) {
        var property = SORTABLE.get(sortBy);
        if (property == null)
            throw new IllegalArgumentException("Solo se puede ordenar por: " + String.join(", ", SORTABLE.keySet()));
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc"))
            throw new IllegalArgumentException("La dirección del orden debe ser asc o desc");
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.fromString(direction), property).and(Sort.by(Sort.Direction.DESC, "id")));
    }

    static BoundingBox resolveBox(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        var given = java.util.stream.Stream.of(minLat, maxLat, minLng, maxLng).filter(v -> v != null).count();
        if (given == 0) return null;
        if (given != 4) throw new IllegalArgumentException("El rectángulo necesita minLat, maxLat, minLng y maxLng");
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }

    static SearchArea resolveArea(Double minLat, Double maxLat, Double minLng, Double maxLng,
                                  Double lat, Double lng, Double radiusMeters) {
        var box = resolveBox(minLat, maxLat, minLng, maxLng);
        var circleParams = java.util.stream.Stream.of(lat, lng, radiusMeters).filter(v -> v != null).count();
        if (box != null && circleParams > 0)
            throw new IllegalArgumentException("Indica un rectángulo o un círculo, no ambos");
        if (box != null) return SearchArea.ofBox(box);
        if (circleParams != 3)
            throw new IllegalArgumentException("Indica un área: minLat, maxLat, minLng y maxLng, o lat, lng y radiusMeters");
        return SearchArea.ofCircle(GeoPoint.of(lat, lng), radiusMeters);
    }
}
