package com.example.school.register

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.jwt.Claim
import io.quarkus.test.security.jwt.JwtSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.time.Instant
import java.util.UUID

/**
 * register-service orchestrates calls to student-service, classes-service and
 * schedule-service; every test here mocks all three REST clients so no real
 * network calls to the other services happen. It owns one real table,
 * idempotency_key, which the tests below that pass an Idempotency-Key header
 * exercise for real against Quarkus's Dev Services Postgres -- everything
 * else runs with no idempotency key, unaffected by that table's existence.
 */
@QuarkusTest
class RegisterResourceTest {

    @InjectMock
    @RestClient
    private lateinit var studentClient: StudentServiceClient

    @InjectMock
    @RestClient
    private lateinit var classesClient: ClassesServiceClient

    @InjectMock
    @RestClient
    private lateinit var scheduleClient: ScheduleServiceClient

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `student can enroll themselves in a class with seats available`() {
        val studentId = UUID.fromString("00000000-0000-0000-0001-000000000001")
        val classId = UUID.fromString("00000000-0000-0000-0003-000000000001")
        val entryId = UUID.randomUUID()
        val now = Instant.now()

        Mockito.`when`(studentClient.getStudent(anyArg(), anyArg()))
            .thenReturn(StudentSummary(studentId, "Alice", "Anderson", "alice@example.edu", "ACTIVE"))
        Mockito.`when`(classesClient.getClass(anyArg(), anyArg()))
            .thenReturn(ClassSummary(classId, "CS101", "Intro to CS", "FALL2026", 30, 10, 20))
        Mockito.`when`(classesClient.reserveSeat(anyArg(), anyArg()))
            .thenReturn(SeatOperationResponse(classId, 30, 11, 19))
        Mockito.`when`(scheduleClient.createEntry(anyArg(), anyArg()))
            .thenReturn(ScheduleEntryResponse(entryId, studentId, classId, "CS101", "Intro to CS", "FALL2026", "ENROLLED", now, now))

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body("""{"studentId":"$studentId","classId":"$classId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(201)
            .body("status", equalTo("ENROLLED"))

        Mockito.verify(scheduleClient).createEntry(anyArg(), anyArg())
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `enroll fails with 409 and never reserves a seat when the class is full`() {
        val studentId = UUID.fromString("00000000-0000-0000-0001-000000000001")
        val classId = UUID.fromString("00000000-0000-0000-0003-000000000002")

        Mockito.`when`(studentClient.getStudent(anyArg(), anyArg()))
            .thenReturn(StudentSummary(studentId, "Alice", "Anderson", "alice@example.edu", "ACTIVE"))
        Mockito.`when`(classesClient.getClass(anyArg(), anyArg()))
            .thenReturn(ClassSummary(classId, "CS102", "Data Structures", "FALL2026", 20, 20, 0))

        given()
            .contentType(ContentType.JSON)
            .body("""{"studentId":"$studentId","classId":"$classId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(409)

        Mockito.verify(classesClient, Mockito.never()).reserveSeat(anyArg(), anyArg())
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `enroll releases the reserved seat when schedule-service fails after reservation`() {
        val studentId = UUID.fromString("00000000-0000-0000-0001-000000000001")
        val classId = UUID.fromString("00000000-0000-0000-0003-000000000003")

        Mockito.`when`(studentClient.getStudent(anyArg(), anyArg()))
            .thenReturn(StudentSummary(studentId, "Alice", "Anderson", "alice@example.edu", "ACTIVE"))
        Mockito.`when`(classesClient.getClass(anyArg(), anyArg()))
            .thenReturn(ClassSummary(classId, "CS103", "Algorithms", "FALL2026", 30, 10, 20))
        Mockito.`when`(classesClient.reserveSeat(anyArg(), anyArg()))
            .thenReturn(SeatOperationResponse(classId, 30, 11, 19))
        Mockito.`when`(classesClient.releaseSeat(anyArg(), anyArg()))
            .thenReturn(SeatOperationResponse(classId, 30, 10, 20))
        Mockito.`when`(scheduleClient.createEntry(anyArg(), anyArg()))
            .thenThrow(RuntimeException("boom"))

        given()
            .contentType(ContentType.JSON)
            .body("""{"studentId":"$studentId","classId":"$classId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(502)

        Mockito.verify(classesClient, Mockito.times(1)).releaseSeat(anyArg(), anyArg())
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `student cannot enroll a different student and no downstream clients are called`() {
        val otherStudentId = UUID.fromString("00000000-0000-0000-0001-000000000099")
        val classId = UUID.fromString("00000000-0000-0000-0003-000000000001")

        given()
            .contentType(ContentType.JSON)
            .body("""{"studentId":"$otherStudentId","classId":"$classId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(403)

        Mockito.verify(studentClient, Mockito.never()).getStudent(anyArg(), anyArg())
        Mockito.verify(classesClient, Mockito.never()).getClass(anyArg(), anyArg())
        Mockito.verify(classesClient, Mockito.never()).reserveSeat(anyArg(), anyArg())
        Mockito.verify(scheduleClient, Mockito.never()).createEntry(anyArg(), anyArg())
    }

    @Test
    @TestSecurity(user = "admin", roles = ["ADMIN"])
    fun `admin can enroll any student`() {
        val studentId = UUID.fromString("00000000-0000-0000-0001-000000000005")
        val classId = UUID.fromString("00000000-0000-0000-0003-000000000001")
        val entryId = UUID.randomUUID()
        val now = Instant.now()

        Mockito.`when`(studentClient.getStudent(anyArg(), anyArg()))
            .thenReturn(StudentSummary(studentId, "Bob", "Baker", "bob@example.edu", "ACTIVE"))
        Mockito.`when`(classesClient.getClass(anyArg(), anyArg()))
            .thenReturn(ClassSummary(classId, "CS101", "Intro to CS", "FALL2026", 30, 10, 20))
        Mockito.`when`(classesClient.reserveSeat(anyArg(), anyArg()))
            .thenReturn(SeatOperationResponse(classId, 30, 11, 19))
        Mockito.`when`(scheduleClient.createEntry(anyArg(), anyArg()))
            .thenReturn(ScheduleEntryResponse(entryId, studentId, classId, "CS101", "Intro to CS", "FALL2026", "ENROLLED", now, now))

        given()
            .contentType(ContentType.JSON)
            .body("""{"studentId":"$studentId","classId":"$classId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(201)
            .body("status", equalTo("ENROLLED"))
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `repeating the same Idempotency-Key replays the first response instead of enrolling twice`() {
        val studentId = UUID.fromString("00000000-0000-0000-0001-000000000001")
        val classId = UUID.fromString("00000000-0000-0000-0003-000000000001")
        val entryId = UUID.randomUUID()
        val now = Instant.now()
        val idempotencyKey = UUID.randomUUID().toString()

        Mockito.`when`(studentClient.getStudent(anyArg(), anyArg()))
            .thenReturn(StudentSummary(studentId, "Alice", "Anderson", "alice@example.edu", "ACTIVE"))
        Mockito.`when`(classesClient.getClass(anyArg(), anyArg()))
            .thenReturn(ClassSummary(classId, "CS101", "Intro to CS", "FALL2026", 30, 10, 20))
        Mockito.`when`(classesClient.reserveSeat(anyArg(), anyArg()))
            .thenReturn(SeatOperationResponse(classId, 30, 11, 19))
        Mockito.`when`(scheduleClient.createEntry(anyArg(), anyArg()))
            .thenReturn(ScheduleEntryResponse(entryId, studentId, classId, "CS101", "Intro to CS", "FALL2026", "ENROLLED", now, now))

        val body = """{"studentId":"$studentId","classId":"$classId"}"""

        val first = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey)
            .body(body)
            .`when`().post("/register/enroll")
            .then()
            .statusCode(201)
            .body("status", equalTo("ENROLLED"))
            .extract().path<String>("scheduleEntryId")

        // Same key, same body, sent again -- as if the client's first response
        // never arrived and it retried. Nothing downstream runs a second time:
        // the saga executed exactly once, this call just replays that result.
        val second = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey)
            .body(body)
            .`when`().post("/register/enroll")
            .then()
            .statusCode(201)
            .body("status", equalTo("ENROLLED"))
            .extract().path<String>("scheduleEntryId")

        assert(first == second) { "replayed response should be identical to the first, got $first then $second" }
        Mockito.verify(scheduleClient, Mockito.times(1)).createEntry(anyArg(), anyArg())
        Mockito.verify(classesClient, Mockito.times(1)).reserveSeat(anyArg(), anyArg())
    }

    @Test
    @TestSecurity(user = "student1", roles = ["STUDENT"])
    @JwtSecurity(claims = [Claim(key = "studentId", value = "00000000-0000-0000-0001-000000000001")])
    fun `reusing an Idempotency-Key for a different request is rejected, not silently replayed`() {
        val studentId = UUID.fromString("00000000-0000-0000-0001-000000000001")
        val firstClassId = UUID.fromString("00000000-0000-0000-0003-000000000001")
        val secondClassId = UUID.fromString("00000000-0000-0000-0003-000000000002")
        val idempotencyKey = UUID.randomUUID().toString()

        Mockito.`when`(studentClient.getStudent(anyArg(), anyArg()))
            .thenReturn(StudentSummary(studentId, "Alice", "Anderson", "alice@example.edu", "ACTIVE"))
        Mockito.`when`(classesClient.getClass(anyArg(), anyArg()))
            .thenReturn(ClassSummary(firstClassId, "CS101", "Intro to CS", "FALL2026", 30, 10, 20))
        Mockito.`when`(classesClient.reserveSeat(anyArg(), anyArg()))
            .thenReturn(SeatOperationResponse(firstClassId, 30, 11, 19))
        Mockito.`when`(scheduleClient.createEntry(anyArg(), anyArg()))
            .thenReturn(ScheduleEntryResponse(UUID.randomUUID(), studentId, firstClassId, "CS101", "Intro to CS", "FALL2026", "ENROLLED", Instant.now(), Instant.now()))

        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey)
            .body("""{"studentId":"$studentId","classId":"$firstClassId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(201)

        // Same key, but a request that means something different -- this must
        // not be answered from the cache as if it were the same enrollment.
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey)
            .body("""{"studentId":"$studentId","classId":"$secondClassId"}""")
            .`when`().post("/register/enroll")
            .then()
            .statusCode(409)

        // Only the first (accepted) request should have reached the saga at all.
        Mockito.verify(classesClient, Mockito.times(1)).getClass(anyArg(), anyArg())
    }
}

/**
 * Mockito's argument matchers return null, which Kotlin rejects when the mocked
 * method declares non-null parameter types. Routing the matcher through an
 * unbounded generic keeps that null away from Kotlin's null checks -- Mockito only
 * records the matcher and never uses the value returned here.
 */
private fun <T> anyArg(): T = ArgumentMatchers.any()
