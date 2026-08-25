package com.example.school.register

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.logging.Logger
import java.util.UUID

/**
 * Orchestrates enrolling/dropping students in classes across student-service,
 * classes-service and schedule-service. This service owns no business data of
 * its own -- it exists purely to coordinate calls to the other three services
 * and to compensate (release a reserved seat) if the schedule-entry write
 * fails after the seat has already been reserved. It does own one
 * infrastructure table, idempotency_key (see IdempotencyKeyStore), used only
 * to dedupe retried enroll requests.
 */
@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class RegisterResource(
    @RestClient private val studentClient: StudentServiceClient,
    @RestClient private val classesClient: ClassesServiceClient,
    @RestClient private val scheduleClient: ScheduleServiceClient,
    private val jwt: JsonWebToken,
    private val internalToken: InternalTokenHolder,
    private val idempotencyKeys: IdempotencyKeyStore,
    private val mapper: ObjectMapper
) {
    private val log = Logger.getLogger(RegisterResource::class.java)

    @POST
    @Path("/enroll")
    @RolesAllowed("STUDENT", "ADMIN")
    fun enroll(
        @Valid request: EnrollRequest,
        @HeaderParam("Authorization") authorization: String?,
        @HeaderParam("Idempotency-Key") idempotencyKey: String?
    ): Response {
        assertCanActFor(request.studentId)

        // No key: behave exactly as before, no dedup protection. Real callers
        // (the frontend, an API client) should send one, but requiring it
        // would be a breaking API change, so it stays optional -- the same
        // choice Stripe's API makes.
        if (idempotencyKey == null) {
            return doEnroll(request, authorization)
        }

        val key = try {
            UUID.fromString(idempotencyKey)
        } catch (e: IllegalArgumentException) {
            throw WebApplicationException("Idempotency-Key must be a UUID", Response.Status.BAD_REQUEST)
        }

        when (val claim = idempotencyKeys.claim(key, idempotencyKeys.hash(request.studentId, request.classId))) {
            is IdempotencyClaim.Replay ->
                return Response.status(claim.status).entity(claim.body).build()
            IdempotencyClaim.Conflict ->
                throw WebApplicationException("Idempotency-Key was already used for a different request", Response.Status.CONFLICT)
            IdempotencyClaim.InProgress ->
                throw WebApplicationException("A request with this Idempotency-Key is already being processed", Response.Status.CONFLICT)
            IdempotencyClaim.Acquired -> {}
        }

        // We own the key now. Run the saga for real exactly once; on any
        // failure release the key rather than caching the failure, so the
        // client can retry the same Idempotency-Key and get a fresh attempt
        // instead of being stuck replaying a stale error forever.
        try {
            val response = doEnroll(request, authorization)
            idempotencyKeys.complete(key, response.status, mapper.writeValueAsString(response.entity))
            return response
        } catch (e: Exception) {
            idempotencyKeys.release(key)
            throw e
        }
    }

    private fun doEnroll(request: EnrollRequest, authorization: String?): Response {
        // 1. Confirm the student exists and is active.
        val student = try {
            studentClient.getStudent(request.studentId, authorization)
        } catch (e: WebApplicationException) {
            throw NotFoundException("Student ${request.studentId} not found")
        }
        if (student.enrollmentStatus != "ACTIVE") {
            throw WebApplicationException("Student ${request.studentId} is not an active student", Response.Status.CONFLICT)
        }

        // 2. Confirm the class exists and has room.
        val courseClass = try {
            classesClient.getClass(request.classId, authorization)
        } catch (e: WebApplicationException) {
            throw NotFoundException("Class ${request.classId} not found")
        }
        if (courseClass.seatsAvailable <= 0) {
            throw WebApplicationException("Class ${courseClass.courseCode} has no seats available", Response.Status.CONFLICT)
        }

        // 3. Reserve the seat.
        classesClient.reserveSeat(request.classId, internalToken.value)

        // 4. Record the schedule entry; compensate by releasing the seat if this fails.
        val entry = try {
            scheduleClient.createEntry(
                CreateScheduleEntryRequest(request.studentId, request.classId, courseClass.courseCode, courseClass.title, courseClass.semester),
                internalToken.value
            )
        } catch (e: Exception) {
            log.error("Failed to create schedule entry after reserving a seat; releasing seat", e)
            classesClient.releaseSeat(request.classId, internalToken.value)
            throw WebApplicationException("Failed to complete enrollment; please try again", Response.Status.BAD_GATEWAY)
        }

        val result = EnrollmentResult(entry.id, entry.studentId, entry.classId, entry.courseCode, entry.courseTitle, entry.semester, entry.status)
        return Response.status(Response.Status.CREATED).entity(result).build()
    }

    @POST
    @Path("/drop")
    @RolesAllowed("STUDENT", "ADMIN")
    fun drop(@Valid request: DropRequest): Response {
        assertCanActFor(request.studentId)

        val matches = scheduleClient.findEntry(request.studentId, request.classId, "ENROLLED", internalToken.value)
        val entry = matches.firstOrNull() ?: throw NotFoundException("No active enrollment found for this student in this class")

        scheduleClient.updateStatus(entry.id, UpdateStatusRequest("DROPPED"), internalToken.value)
        classesClient.releaseSeat(request.classId, internalToken.value)

        return Response.ok(mapOf("scheduleEntryId" to entry.id, "status" to "DROPPED")).build()
    }

    private fun assertCanActFor(studentId: UUID) {
        if (jwt.groups.contains("STUDENT") && !jwt.groups.contains("ADMIN")) {
            val ownStudentId = jwt.getClaim<String>("studentId")
            if (ownStudentId == null || ownStudentId != studentId.toString()) {
                throw ForbiddenException("Students may only register or drop classes for themselves")
            }
        }
    }
}
