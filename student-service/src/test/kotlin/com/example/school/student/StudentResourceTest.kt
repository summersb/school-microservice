package com.example.school.student

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.jwt.Claim
import io.quarkus.test.security.jwt.JwtSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

@QuarkusTest
class StudentResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can list all seeded students`() {
        given()
            .`when`().get("/students")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(10))
    }

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can create update and delete a student`() {
        val newId = given()
            .contentType(ContentType.JSON)
            .body(
                """{"firstName":"Test","lastName":"User","email":"test.user@example.edu","dateOfBirth":"2005-01-01"}"""
            )
            .`when`().post("/students")
            .then()
            .statusCode(201)
            .body("firstName", equalTo("Test"))
            .extract().path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body(
                """{"firstName":"Test","lastName":"User","email":"test.user@example.edu","dateOfBirth":"2005-01-01","enrollmentStatus":"INACTIVE"}"""
            )
            .`when`().put("/students/$newId")
            .then()
            .statusCode(200)
            .body("enrollmentStatus", equalTo("INACTIVE"))

        given()
            .`when`().delete("/students/$newId")
            .then()
            .statusCode(204)

        given()
            .`when`().get("/students/$newId")
            .then()
            .statusCode(404)
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `student can read their own record`() {
        given()
            .`when`().get("/students/00000000-0000-0000-0001-000000000001")
            .then()
            .statusCode(200)
            .body("firstName", equalTo("Alice"))
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `student cannot read another student's record`() {
        given()
            .`when`().get("/students/00000000-0000-0000-0001-000000000002")
            .then()
            .statusCode(403)
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    fun `student cannot create students`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{"firstName":"Nope","lastName":"Nope","email":"nope@example.edu","dateOfBirth":"2005-01-01"}"""
            )
            .`when`().post("/students")
            .then()
            .statusCode(403)
    }

    @Test
    fun `unauthenticated requests are rejected`() {
        given()
            .`when`().get("/students")
            .then()
            .statusCode(401)
    }
}
