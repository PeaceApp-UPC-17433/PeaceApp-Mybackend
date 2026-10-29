package com.upc.pre.peaceapp.reports.interfaces.rest;

import com.upc.pre.peaceapp.reports.domain.model.commands.UpdateReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetHeatmapQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.SearchReportsQuery;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportFilter;
import com.upc.pre.peaceapp.reports.domain.services.ReportCommandService;
import com.upc.pre.peaceapp.reports.domain.services.ReportQueryService;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.CreateLocationResource;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.HeatCellResource;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.LocationResource;
import com.upc.pre.peaceapp.reports.interfaces.rest.transform.HeatCellResourceFromValueObjectAssembler;
import com.upc.pre.peaceapp.reports.interfaces.rest.transform.ReportResourceFromEntityAssembler;
import com.upc.pre.peaceapp.shared.interfaces.rest.AuthenticatedRequester;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Compatibilidad con la API de ubicaciones anterior. Las coordenadas ahora pertenecen al reporte,
 * así que estos endpoints son una vista sobre {@code reports}; el flujo nuevo es {@code /api/v1/reports}.
 */
@RestController
@RequestMapping(value = "/api/v1/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Location", description = "Location Endpoints")
public class LocationsController {
    private static final int MAX_LOCATIONS = 1000;

    private final ReportCommandService commandService;
    private final ReportQueryService queryService;

    public LocationsController(ReportCommandService commandService, ReportQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /** Mueve el reporte a las coordenadas indicadas (solo su dueño o un administrador). */
    @PostMapping({"", "/"})
    public ResponseEntity<LocationResource> saveLocation(@Valid @RequestBody CreateLocationResource resource,
                                                         Authentication authentication) {
        var report = commandService.handle(new UpdateReportCommand(resource.idReport(), authentication.getName(),
                AuthenticatedRequester.isAdmin(authentication), null, null, null, null, null,
                GeoPoint.of(resource.latitude(), resource.longitude()), null, null));
        return ResponseEntity.created(URI.create("/api/v1/reports/" + report.getId()))
                .body(ReportResourceFromEntityAssembler.toLocationFromEntity(report));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<LocationResource>> getLocations() {
        var newest = PageRequest.of(0, MAX_LOCATIONS, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var query = new SearchReportsQuery(ReportFilter.none(), null, null, null, newest);
        return ResponseEntity.ok(queryService.handle(query).stream()
                .map(ReportResourceFromEntityAssembler::toLocationFromEntity).toList());
    }

    /** Zonas con al menos {@code quantity_reports} reportes activos. */
    @GetMapping("/dangerous")
    public ResponseEntity<List<HeatCellResource>> getDangerousLocations(
            @RequestParam("quantity_reports") int quantityReports) {
        var query = new GetHeatmapQuery(null, 0.005, quantityReports);
        return ResponseEntity.ok(queryService.handle(query).stream()
                .map(HeatCellResourceFromValueObjectAssembler::toResourceFromValueObject).toList());
    }
}
