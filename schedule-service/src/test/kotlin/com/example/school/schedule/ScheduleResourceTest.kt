package com.example.school.schedule

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.jwt.Claim
import io.quarkus.test.security.jwt.JwtSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

@QuarkusTest
class ScheduleResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can list a student's full schedule`() {
        given()
            .`when`().get("/schedules/student/00000000-0000-0000-0001-000000000001")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(4))
    }

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can list a student's current schedule only`() {
        given()
            .`when`().get("/schedules/student/00000000-0000-0000-0001-000000000001/current")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("status", everyItem(equalTo("ENROLLED")))
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `student can read their own schedule`() {
        given()
            .`when`().get("/schedules/student/00000000-0000-0000-0001-000000000001")
            .then()
            .statusCode(200)
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `student cannot read another student's schedule`() {
        given()
            .`when`().get("/schedules/student/00000000-0000-0000-0001-000000000002")
            .then()
            .statusCode(403)
    }

    @Test
    fun `internal create without shared secret is rejected`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{"studentId":"00000000-0000-0000-0001-000000000001","classId":"00000000-0000-0000-0003-000000000001","courseCode":"CS101","courseTitle":"Intro to Computer Science","semester":"FALL2026"}"""
            )
            .`when`().post("/internal/schedule-entries")
            .then()
            .statusCode(401)
    }

    @Test
    fun `internal create with valid shared secret succeeds`() {
        given()
            .header("X-Internal-Token", "dev-only-internal-token-change-me")
            .contentType(ContentType.JSON)
            .body(
                """{"studentId":"00000000-0000-0000-0001-000000000006","classId":"00000000-0000-0000-0003-000000000002","courseCode":"CS102","courseTitle":"Data Structures","semester":"FALL2026"}"""
            )
            .`when`().post("/internal/schedule-entries")
            .then()
            .statusCode(201)
            .body("status", equalTo("ENROLLED"))
    }
}
