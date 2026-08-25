package com.example.school.auth

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

@QuarkusTest
class AuthResourceTest {

    @Test
    fun `login with correct credentials returns a token`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin","password":"password123"}""")
            .`when`().post("/auth/login")
            .then()
            .statusCode(200)
            .body("token", not(equalTo("")))
            .body("role", equalTo("ADMIN"))
            .body("username", equalTo("admin"))
    }

    @Test
    fun `login with wrong password is rejected`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin","password":"wrong-password"}""")
            .`when`().post("/auth/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `register with a new username succeeds`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"brand.new.user","password":"password123","role":"STUDENT"}""")
            .`when`().post("/auth/register")
            .then()
            .statusCode(201)
            .body("username", equalTo("brand.new.user"))
            .body("role", equalTo("STUDENT"))
    }

    @Test
    fun `register with a duplicate username is rejected`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin","password":"password123","role":"ADMIN"}""")
            .`when`().post("/auth/register")
            .then()
            .statusCode(409)
    }

    @Test
    fun `me without a token is rejected`() {
        given()
            .`when`().get("/auth/me")
            .then()
            .statusCode(401)
    }
}
