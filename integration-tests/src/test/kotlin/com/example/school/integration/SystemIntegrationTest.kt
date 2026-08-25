package com.example.school.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Black-box end-to-end tests that exercise the REAL running system through
 * the Apache proxy -- no mocks. They assume `docker compose up --build` is
 * already running (see the repo README). Run with:
 *
 *     mvn test -pl integration-tests -DskipITs=false
 */
class SystemIntegrationTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            RestAssured.baseURI = System.getProperty("base.url", "http://localhost:8080")
        }

        private fun login(username: String, password: String): Pair<String, String?> {
            val response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""{"username":"$username","password":"$password"}""")
                .`when`().post("/auth-api/auth/login")
                .then().statusCode(200)
                .extract()
            val token = response.path<String>("token")
            val studentId = response.path<String>("studentId")
            return token to studentId
        }
    }

    @Test
    fun `full student journey - login, browse, enroll, view schedule, drop`() {
        val (token, studentId) = login("alice.johnson", "password123")
        assertNotNull(token, "login should return a JWT")
        assertNotNull(studentId, "alice.johnson should be linked to a studentId")

        val auth = "Bearer $token"

        // Browse the current semester's catalog and find a class with a
        // known/stable capacity (CS201 has 0 seats taken in the seed data).
        val classes = RestAssured.given()
            .header("Authorization", auth)
            .`when`().get("/classes-api/classes?semester=FALL2026")
            .then().statusCode(200)
            .extract().jsonPath().getList<Map<String, Any>>("")

        val cs201 = classes.first { it["courseCode"] == "CS201" }
        val classId = cs201["id"] as String
        val seatsAvailableBefore = (cs201["seatsAvailable"] as Int)

        // Enroll.
        val enrollResponse = RestAssured.given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body("""{"studentId":"$studentId","classId":"$classId"}""")
            .`when`().post("/register-api/register/enroll")
            .then().statusCode(201)
            .body("status", org.hamcrest.Matchers.equalTo("ENROLLED"))
            .extract()

        val scheduleEntryId = enrollResponse.path<String>("scheduleEntryId")
        assertNotNull(scheduleEntryId)

        // It shows up in "current" schedule.
        RestAssured.given()
            .header("Authorization", auth)
            .`when`().get("/schedule-api/schedules/student/$studentId/current")
            .then().statusCode(200)
            .body("classId", org.hamcrest.Matchers.hasItem(classId))

        // The seat count on the class dropped by exactly one.
        val classAfterEnroll = RestAssured.given()
            .header("Authorization", auth)
            .`when`().get("/classes-api/classes/$classId")
            .then().statusCode(200)
            .extract()
        assertEquals(seatsAvailableBefore - 1, classAfterEnroll.path<Int>("seatsAvailable"))

        // Drop it again.
        RestAssured.given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body("""{"studentId":"$studentId","classId":"$classId"}""")
            .`when`().post("/register-api/register/drop")
            .then().statusCode(200)
            .body("status", org.hamcrest.Matchers.equalTo("DROPPED"))

        // No longer in "current" schedule.
        val currentAfterDrop = RestAssured.given()
            .header("Authorization", auth)
            .`when`().get("/schedule-api/schedules/student/$studentId/current")
            .then().statusCode(200)
            .extract().jsonPath().getList<String>("classId")
        assertFalse(currentAfterDrop.contains(classId), "dropped class should no longer be in the current schedule")

        // The seat was released.
        val classAfterDrop = RestAssured.given()
            .header("Authorization", auth)
            .`when`().get("/classes-api/classes/$classId")
            .then().statusCode(200)
            .extract()
        assertEquals(seatsAvailableBefore, classAfterDrop.path<Int>("seatsAvailable"))
    }

    @Test
    fun `admin can manage students end to end through the proxy`() {
        val (adminToken, _) = login("admin", "password123")
        val auth = "Bearer $adminToken"

        val createResponse = RestAssured.given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body("""{"firstName":"Integration","lastName":"Test","email":"integration.test@example.edu","dateOfBirth":"2005-06-15"}""")
            .`when`().post("/student-api/students")
            .then().statusCode(201)
            .extract()
        val newStudentId = createResponse.path<String>("id")

        RestAssured.given()
            .header("Authorization", auth)
            .`when`().get("/student-api/students/$newStudentId")
            .then().statusCode(200)
            .body("email", org.hamcrest.Matchers.equalTo("integration.test@example.edu"))

        RestAssured.given()
            .header("Authorization", auth)
            .`when`().delete("/student-api/students/$newStudentId")
            .then().statusCode(204)
    }

    @Test
    fun `unauthenticated requests are rejected by every service through the proxy`() {
        RestAssured.given().`when`().get("/student-api/students").then().statusCode(401)
        RestAssured.given().`when`().get("/faculty-api/faculty").then().statusCode(401)
        RestAssured.given().`when`().get("/classes-api/classes").then().statusCode(401)
    }

    @Test
    fun `login with wrong password is rejected`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"username":"admin","password":"wrong-password"}""")
            .`when`().post("/auth-api/auth/login")
            .then().statusCode(401)
    }

    @Test
    fun `frontend is served at slash ui`() {
        RestAssured.given()
            .`when`().get("/ui/")
            .then().statusCode(200)
            .contentType(org.hamcrest.Matchers.containsString("text/html"))
    }

    /**
     * A 200 on /ui/ is not enough to know the app actually loads: if Vite's `base`
     * does not match the path Apache serves the app from, index.html asks for
     * /assets/... at the server root, every one of those 404s and the user gets a
     * blank page. So follow the asset URLs the page really references.
     */
    @Test
    fun `frontend assets referenced by the page are served`() {
        val html = RestAssured.given()
            .`when`().get("/ui/")
            .then().statusCode(200)
            .extract().asString()

        val assetUrls = Regex("""(?:src|href)="([^"]*/assets/[^"]+)"""")
            .findAll(html)
            .map { it.groupValues[1] }
            .toList()

        assertTrue(assetUrls.isNotEmpty(), "index.html referenced no /assets/ URLs; markup was:\n$html")
        assertTrue(
            assetUrls.all { it.startsWith("/ui/") },
            "asset URLs must be served from under /ui/, but found: $assetUrls"
        )

        assetUrls.forEach { url ->
            val status = RestAssured.given().`when`().get(url).statusCode
            assertEquals(200, status, "asset $url referenced by /ui/ was not served")
        }
    }

    /** A hard refresh on a client-side route must still return the SPA shell. */
    @Test
    fun `client-side routes fall back to the SPA shell`() {
        listOf("/ui/login", "/ui/classes").forEach { path ->
            RestAssured.given()
                .`when`().get(path)
                .then().statusCode(200)
                .contentType(org.hamcrest.Matchers.containsString("text/html"))
                .body(org.hamcrest.Matchers.containsString("""<div id="root">"""))
        }
    }
}
