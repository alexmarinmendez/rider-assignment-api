package com.softer.riderassignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softer.riderassignment.model.AssignmentStatus;
import com.softer.riderassignment.model.RiderAssignment;
import com.softer.riderassignment.repository.RiderAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RiderAssignmentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RiderAssignmentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String URL = "/api/assignments";

    @BeforeEach
    void prepararDatos() {
        repository.deleteAll();
    }

    @AfterEach
    void limpiarDatos() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("01 - GET /api/assignments lista asignaciones guardadas")
    void debeListarAsignaciones() throws Exception {
        repository.save(nuevaAsignacion(1001L, 501L));

        ResponseEntity<String> respuesta = restTemplate.getForEntity(URL, String.class);
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());

        assertAll(
                () -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertEquals(1, cuerpo.size()),
                () -> assertEquals(1001L, cuerpo.get(0).get("deliveryId").asLong()),
                () -> assertEquals(501L, cuerpo.get(0).get("riderId").asLong())
        );
    }

    @Test
    @DisplayName("02 - GET /api/assignments/{id} devuelve la asignación solicitada")
    void debeObtenerAsignacionPorId() throws Exception {
        RiderAssignment guardada = repository.save(nuevaAsignacion(1002L, 502L));

        ResponseEntity<String> respuesta = restTemplate.getForEntity(
                URL + "/{id}", String.class, guardada.getId());
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());

        assertAll(
                () -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertEquals(guardada.getId().longValue(), cuerpo.get("id").asLong()),
                () -> assertEquals(1002L, cuerpo.get("deliveryId").asLong()),
                () -> assertEquals("ASSIGNED", cuerpo.get("status").asText())
        );
    }

    @Test
    @DisplayName("03 - POST /api/assignments crea y guarda una asignación")
    void debeCrearAsignacion() throws Exception {
        Map<String, Object> solicitud = datos(1003L, 503L, "ASSIGNED");

        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                URL, solicitud, String.class);
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());

        assertAll(
                () -> assertEquals(HttpStatus.CREATED, respuesta.getStatusCode()),
                () -> assertTrue(cuerpo.get("id").asLong() > 0),
                () -> assertEquals(1003L, cuerpo.get("deliveryId").asLong()),
                () -> assertEquals("ASSIGNED", cuerpo.get("status").asText()),
                () -> assertEquals(1L, repository.count()),
                () -> assertTrue(repository.existsByDeliveryId(1003L))
        );
    }

    @Test
    @DisplayName("04 - PUT /api/assignments/{id} actualiza el rider y el estado")
    void debeActualizarAsignacion() throws Exception {
        RiderAssignment guardada = repository.save(nuevaAsignacion(1004L, 504L));
        Map<String, Object> solicitud = datos(1004L, 505L, "ACCEPTED");

        ResponseEntity<String> respuesta = restTemplate.exchange(
                URL + "/{id}", HttpMethod.PUT, new HttpEntity<>(solicitud),
                String.class, guardada.getId());
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());
        RiderAssignment actualizada = repository.findById(guardada.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertEquals(505L, cuerpo.get("riderId").asLong()),
                () -> assertEquals("ACCEPTED", cuerpo.get("status").asText()),
                () -> assertEquals(505L, actualizada.getRiderId().longValue()),
                () -> assertEquals(AssignmentStatus.ACCEPTED, actualizada.getStatus())
        );
    }

    @Test
    @DisplayName("05 - DELETE /api/assignments/{id} elimina la asignación")
    void debeEliminarAsignacion() {
        RiderAssignment guardada = repository.save(nuevaAsignacion(1005L, 506L));

        ResponseEntity<Void> respuesta = restTemplate.exchange(
                URL + "/{id}", HttpMethod.DELETE, HttpEntity.EMPTY,
                Void.class, guardada.getId());

        assertAll(
                () -> assertEquals(HttpStatus.NO_CONTENT, respuesta.getStatusCode()),
                () -> assertFalse(repository.existsById(guardada.getId())),
                () -> assertEquals(0L, repository.count())
        );
    }

    @Test
    @DisplayName("06 - POST /api/assignments rechaza un delivery duplicado")
    void debeRechazarDeliveryDuplicado() throws Exception {
        repository.save(nuevaAsignacion(1006L, 507L));
        Map<String, Object> solicitud = datos(1006L, 508L, "ASSIGNED");

        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                URL, solicitud, String.class);
        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode()),
                () -> assertEquals(409, cuerpo.get("status").asInt()),
                () -> assertTrue(cuerpo.get("message").asText().contains("ya tiene una asignación")),
                () -> assertEquals(1L, repository.count())
        );
    }

    private RiderAssignment nuevaAsignacion(Long deliveryId, Long riderId) {
        return new RiderAssignment(null, deliveryId, riderId,
                AssignmentStatus.ASSIGNED, null);
    }

    private Map<String, Object> datos(Long deliveryId, Long riderId, String estado) {
        return Map.of("deliveryId", deliveryId, "riderId", riderId, "status", estado);
    }
}