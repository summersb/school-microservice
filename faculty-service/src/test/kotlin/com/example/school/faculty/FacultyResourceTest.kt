package com.example.school.faculty

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

@QuarkusTest
class FacultyResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can list all seeded faculty`() {
        given()
            .`when`().get("/faculty")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(6))
    }

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can create update and delete a faculty member`() {
        val newId = given()
            .contentType(ContentType.JSON)
            .body(
                """{"firstName":"Test","lastName":"User","email":"test.user@example.edu","department":"Testing","title":"Lecturer","hireDate":"2020-01-01"}"""
            )
            .`when`().post("/faculty")
            .then()
            .statusCode(201)
            .body("firstName", equalTo("Test"))
            .extract().path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body(
                """{"firstName":"Test","lastName":"User","email":"test.user@example.edu","department":"Testing","title":"Senior Lecturer","hireDate":"2020-01-01"}"""
            )
            .`when`().put("/faculty/$newId")
            .then()
            .statusCode(200)
            .body("title", equalTo("Senior Lecturer"))

        given()
            .`when`().delete("/faculty/$newId")
            .then()
            .statusCode(204)

        given()
            .`when`().get("/faculty/$newId")
            .then()
            .statusCode(404)
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    fun `student can read a single faculty record`() {
        given()
            .`when`().get("/faculty/00000000-0000-0000-0002-000000000001")
            .then()
            .statusCode(200)
            .body("lastName", equalTo("Miller"))
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    fun `student cannot create faculty`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{"firstName":"Nope","lastName":"Nope","email":"nope@example.edu","department":"Nope","title":"Nope","hireDate":"2020-01-01"}"""
            )
            .`when`().post("/faculty")
            .then()
            .statusCode(403)
    }

    @Test
    fun `unauthenticated requests are rejected`() {
        given()
            .`when`().get("/faculty")
            .then()
            .statusCode(401)
    }
}
