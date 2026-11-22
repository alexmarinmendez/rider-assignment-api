package com.softer.riderassignment;

import com.softer.riderassignment.model.AssignmentStatus;
import com.softer.riderassignment.model.RiderAssignment;
import com.softer.riderassignment.repository.RiderAssignmentRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ApplicationTest {

    private static final String INSERT_ASSIGNMENT =
            "INSERT INTO rider_assignments(id, delivery_id, rider_id, status, assigned_at) "
                    + "VALUES (1, 7001, 301, 'ASSIGNED', CURRENT_TIMESTAMP)";
    private static final String DELETE_ALL = "DELETE FROM rider_assignments";

    @LocalServerPort
    private int port;

    private String baseUrl;
    private static RestTemplate restTemplate;

    @Autowired
    private RiderAssignmentRepository repository;

    @BeforeAll
    public static void init() {
        restTemplate = new RestTemplate();
    }

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/assignments";
    }

    // --------------------------------- Integration Tests ----------------------------------

    @Test
    @DisplayName("should get 0 assignments when table is empty")
    public void test1_testGetAssignmentsWhenTableIsEmpty() {
        List<?> response = restTemplate.getForObject(baseUrl, List.class);
        assertEquals(0, response.size());
        assertEquals(0, repository.findAll().size());
    }

    @Test
    @DisplayName("should get all assignments when table is not empty")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ALL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test2_testGetAssignmentsWhenTableIsNotEmpty() {
        List<?> response = restTemplate.getForObject(baseUrl, List.class);
        assertEquals(1, response.size());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("should register a new assignment")
    @Sql(statements = DELETE_ALL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test3_testAddAssignment() {
        RiderAssignment assignment = new RiderAssignment(
                null, 7001L, 301L, AssignmentStatus.ASSIGNED, null);

        RiderAssignment response = restTemplate.postForObject(baseUrl, assignment, RiderAssignment.class);

        assertAll(
                () -> assertNotNull(response),
                () -> assertNotNull(response.getId()),
                () -> assertEquals(7001L, response.getDeliveryId()),
                () -> assertEquals(AssignmentStatus.ASSIGNED, response.getStatus()),
                () -> assertEquals(1, repository.findAll().size())
        );
    }

    @Test
    @DisplayName("should get assignment with id=1")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ALL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test4_testFindAssignmentById() {
        RiderAssignment assignment = restTemplate.getForObject(baseUrl + "/{id}", RiderAssignment.class, 1);

        assertAll(
                () -> assertNotNull(assignment),
                () -> assertEquals(1L, assignment.getId()),
                () -> assertEquals(7001L, assignment.getDeliveryId()),
                () -> assertEquals(301L, assignment.getRiderId())
        );
    }

    @Test
    @DisplayName("should update assignment with id=1")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ALL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test5_testUpdateAssignment() {
        RiderAssignment assignment = new RiderAssignment(
                1L, 7001L, 302L, AssignmentStatus.ACCEPTED, null);

        restTemplate.put(baseUrl + "/{id}", assignment, 1);

        RiderAssignment fromDb = repository.findById(1L).orElseThrow();
        assertAll(
                () -> assertEquals(302L, fromDb.getRiderId()),
                () -> assertEquals(AssignmentStatus.ACCEPTED, fromDb.getStatus())
        );
    }

    @Test
    @DisplayName("should delete assignment with id=1")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ALL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test6_testDeleteAssignment() {
        assertEquals(1, repository.findAll().size());

        restTemplate.delete(baseUrl + "/{id}", 1);

        assertEquals(0, repository.findAll().size());
    }
}
