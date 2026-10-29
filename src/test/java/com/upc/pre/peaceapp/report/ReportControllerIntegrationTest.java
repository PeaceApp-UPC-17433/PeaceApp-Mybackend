package com.upc.pre.peaceapp.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReportControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;

    private static Map<String, Object> body(String title, String type, double lat, double lng) {
        return Map.of("title", title, "detail", "Detalle de prueba", "type", type,
                "latitude", lat, "longitude", lng, "address", "Av. Prueba 123");
    }

    private long createReport(String token, Map<String, Object> body) throws Exception {
        var response = mockMvc.perform(post("/api/v1/reports/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void whenCreateReport_thenIsOwnedByTokenUserAndPending() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("reportuser@example.com", "password123");

        mockMvc.perform(post("/api/v1/reports/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("Robo en la esquina", "THEFT", -12.05, -77.04))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/reports/")))
                .andExpect(jsonPath("$.title").value("Robo en la esquina"))
                .andExpect(jsonPath("$.type").value("THEFT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.severity").value("MEDIUM"))
                .andExpect(jsonPath("$.latitude").value(-12.05))
                .andExpect(jsonPath("$.idUser").isNumber());
    }

    @Test
    void whenCreateReportWithoutToken_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/reports/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("x", "THEFT", 0, 0))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenCreateReportWithInvalidFields_thenBadRequestPerField() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("invalidreport@example.com", "password123");

        mockMvc.perform(post("/api/v1/reports/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"latitude\":95,\"address\":\"Calle\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hay campos inválidos"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());
    }

    @Test
    void whenGetUnknownReport_thenNotFound() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("viewer@example.com", "password123");

        mockMvc.perform(get("/api/v1/reports/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reporte no encontrado"));
    }

    @Test
    void whenOwnerEditsReport_thenOnlyProvidedFieldsChange() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("editor@example.com", "password123");
        long id = createReport(token, body("Original", "ACCIDENT", -12.1, -77.1));

        mockMvc.perform(put("/api/v1/reports/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Editado\",\"severity\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Editado"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.type").value("ACCIDENT"))
                .andExpect(jsonPath("$.latitude").value(-12.1));
    }

    @Test
    void whenEditSendsOnlyLatitude_thenBadRequest() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("halfcoords@example.com", "password123");
        long id = createReport(token, body("Coordenadas", "OTHER", -12.1, -77.1));

        mockMvc.perform(put("/api/v1/reports/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-12.2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenAnotherUserEditsOrDeletes_thenForbidden() throws Exception {
        String owner = authHelper.registerAndAuthenticateTestUser("owner@example.com", "password123");
        String other = authHelper.registerAndAuthenticateTestUser("intruder@example.com", "password123");
        long id = createReport(owner, body("Ajeno", "THEFT", -12.0, -77.0));

        mockMvc.perform(put("/api/v1/reports/" + id)
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hackeado\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/reports/" + id).header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.title").value("Ajeno"));
    }

    @Test
    void whenOwnerDeletes_thenReportIsGone() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("deleter@example.com", "password123");
        long id = createReport(token, body("Borrar", "OTHER", -12.0, -77.0));

        mockMvc.perform(delete("/api/v1/reports/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/reports/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenChangeStatus_thenOnlyAdminAndValidTransitions() throws Exception {
        String user = authHelper.registerAndAuthenticateTestUser("statususer@example.com", "password123");
        String admin = authHelper.registerAndAuthenticateTestAdmin("statusadmin@example.com", "password123");
        long id = createReport(user, body("Estado", "EMERGENCY", -12.0, -77.0));

        mockMvc.perform(put("/api/v1/reports/" + id + "/status")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/reports/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
        mockMvc.perform(put("/api/v1/reports/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isConflict());
        // un administrador también puede borrar reportes ajenos
        mockMvc.perform(delete("/api/v1/reports/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void whenSearchWithFilters_thenPagedAndFiltered() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("searcher@example.com", "password123");
        String other = authHelper.registerAndAuthenticateTestUser("othersearcher@example.com", "password123");
        createReport(token, body("Zzbusqueda alfa", "HARASSMENT", -12.0, -77.0));
        createReport(token, body("Zzbusqueda beta", "HARASSMENT", -12.0, -77.0));
        createReport(token, body("Zzbusqueda gamma", "LIGHTING", -12.0, -77.0));
        createReport(other, body("Zzbusqueda ajena", "HARASSMENT", -12.0, -77.0));

        mockMvc.perform(get("/api/v1/reports?q=zzbusqueda&type=HARASSMENT&mine=true&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("HARASSMENT"));

        mockMvc.perform(get("/api/v1/reports?q=zzbusqueda&sortBy=createdAt&direction=asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].title").value("Zzbusqueda alfa"));
    }

    @Test
    void whenSearchWithInvalidParameters_thenBadRequest() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("badparams@example.com", "password123");

        mockMvc.perform(get("/api/v1/reports?sortBy=password").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports?type=NOPE").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro type no es válido"));
    }

    @Test
    void whenQueryMapByAreaAndRadius_thenOnlyPointsInside() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("maptester@example.com", "password123");
        // Zona aislada (polo sur) para no mezclarse con los datos de otros tests
        createReport(token, body("Cerca", "THEFT", -80.0000, 100.0000));
        createReport(token, body("Cerca 2", "ACCIDENT", -80.0010, 100.0010));
        createReport(token, body("Lejos", "THEFT", -80.0500, 100.0500));

        mockMvc.perform(get("/api/v1/reports/map?minLat=-80.01&maxLat=-79.99&minLng=99.99&maxLng=100.01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].latitude").isNumber())
                .andExpect(jsonPath("$[0].title").doesNotExist());

        mockMvc.perform(get("/api/v1/reports/map?lat=-80.0&lng=100.0&radiusMeters=500&type=ACCIDENT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/reports/map").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports/map?minLat=1&maxLat=2&minLng=1&maxLng=2&lat=1&lng=1&radiusMeters=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenQueryHeatmap_thenCellsAreGroupedWithRiskLevel() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("heatuser@example.com", "password123");
        String admin = authHelper.registerAndAuthenticateTestAdmin("heatadmin@example.com", "password123");
        // Tres reportes en la misma celda y uno lejano, en una zona aislada (polo norte)
        long first = createReport(token, body("Calor 1", "THEFT", 80.0011, 10.0011));
        createReport(token, body("Calor 2", "THEFT", 80.0012, 10.0012));
        createReport(token, body("Calor 3", "ACCIDENT", 80.0013, 10.0013));
        createReport(token, body("Calor lejano", "THEFT", 80.2, 10.2));
        String area = "minLat=79.9&maxLat=80.3&minLng=9.9&maxLng=10.3";

        mockMvc.perform(get("/api/v1/reports/heatmap?" + area).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].count").value(3))
                .andExpect(jsonPath("$[0].riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$[1].count").value(1))
                .andExpect(jsonPath("$[1].riskLevel").value("LOW"));

        // un reporte descartado deja de contar
        mockMvc.perform(put("/api/v1/reports/" + first + "/status").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISCARDED\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/heatmap?" + area + "&minCount=2").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].count").value(2));

        mockMvc.perform(get("/api/v1/reports/heatmap?cellSize=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenListReportsByUser_thenReturnsOnlyThatUserReports() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("byuser@example.com", "password123");
        long id = createReport(token, body("Mio", "OTHER", -12.0, -77.0));
        JsonNode report = objectMapper.readTree(mockMvc.perform(get("/api/v1/reports/" + id)
                .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/reports/user/" + report.get("idUser").asLong())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].idUser", everyItem(is(report.get("idUser").asInt()))));
    }
}
