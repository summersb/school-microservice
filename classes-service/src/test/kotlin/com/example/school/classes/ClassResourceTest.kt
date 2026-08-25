package com.example.school.classes

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.UUID

@QuarkusTest
class ClassResourceTest {

    @InjectMock
    @RestClient
    private lateinit var facultyClient: FacultyServiceClient

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    fun `student can list classes for a semester`() {
        given()
            .`when`().get("/classes?semester=FALL2026")
            .then()
            .statusCode(200)
            .body("size()", equalTo(6))
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    fun `student can read a single class with correct seats available`() {
        given()
            .`when`().get("/classes/00000000-0000-0000-0003-000000000001")
            .then()
            .statusCode(200)
            .body("capacity", equalTo(30))
            .body("seatsTaken", equalTo(2))
            .body("seatsAvailable", equalTo(28))
    }

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can create a class and faculty name comes from the mocked faculty client`() {
        Mockito.`when`(facultyClient.getFaculty(anyArg(), anyArg()))
            .thenReturn(FacultySummary(UUID.fromString("00000000-0000-0000-0002-000000000001"), "Susan", "Miller"))

        given()
            .contentType(ContentType.JSON)
            .body(
                """{"courseCode":"CS301","title":"Algorithms","description":"Advanced algorithms.","semester":"FALL2026","facultyId":"00000000-0000-0000-0002-000000000001","capacity":20,"room":"Eng Hall 300","dayOfWeek":"MWF","startTime":"14:00","endTime":"14:50"}"""
            )
            .`when`().post("/classes")
            .then()
            .statusCode(201)
            .body("courseCode", equalTo("CS301"))
            .body("facultyName", equalTo("Susan Miller"))
    }

    @Test
    fun `internal reserve-seat without shared secret is rejected`() {
        given()
            .`when`().post("/internal/classes/00000000-0000-0000-0003-000000000002/reserve-seat")
            .then()
            .statusCode(401)
    }

    @Test
    fun `internal reserve-seat with valid shared secret succeeds`() {
        given()
            .header("X-Internal-Token", "dev-only-internal-token-change-me")
            .`when`().post("/internal/classes/00000000-0000-0000-0003-000000000002/reserve-seat")
            .then()
            .statusCode(200)
            .body("seatsTaken", equalTo(1))
    }
}

/**
 * Mockito's argument matchers return null, which Kotlin rejects when the mocked
 * method declares non-null parameter types. Routing the matcher through an
 * unbounded generic keeps that null away from Kotlin's null checks -- Mockito only
 * records the matcher and never uses the value returned here.
 */
private fun <T> anyArg(): T = ArgumentMatchers.any()
