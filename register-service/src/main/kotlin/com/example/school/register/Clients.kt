package com.example.school.register

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.Instant
import java.util.UUID

/**
 * Only the fields register-service actually needs from student-service's
 * StudentResponse. Jackson ignores the extra fields present in the real
 * response by default.
 */
data class StudentSummary(val id: UUID, val firstName: String, val lastName: String, val email: String, val enrollmentStatus: String)

@RegisterRestClient(configKey = "student-api")
@Produces(MediaType.APPLICATION_JSON)
interface StudentServiceClient {
    @GET
    @Path("/students/{id}")
    fun getStudent(@PathParam("id") id: UUID, @HeaderParam("Authorization") authorization: String?): StudentSummary
}

data class ClassSummary(
    val id: UUID, val courseCode: String, val title: String, val semester: String,
    val capacity: Int, val seatsTaken: Int, val seatsAvailable: Int
)
data class SeatOperationResponse(val classId: UUID, val capacity: Int, val seatsTaken: Int, val seatsAvailable: Int)

@RegisterRestClient(configKey = "classes-api")
@Produces(MediaType.APPLICATION_JSON)
interface ClassesServiceClient {
    @GET
    @Path("/classes/{id}")
    fun getClass(@PathParam("id") id: UUID, @HeaderParam("Authorization") authorization: String?): ClassSummary

    @POST
    @Path("/internal/classes/{id}/reserve-seat")
    fun reserveSeat(@PathParam("id") id: UUID, @HeaderParam("X-Internal-Token") token: String): SeatOperationResponse

    @POST
    @Path("/internal/classes/{id}/release-seat")
    fun releaseSeat(@PathParam("id") id: UUID, @HeaderParam("X-Internal-Token") token: String): SeatOperationResponse
}

data class ScheduleEntryResponse(
    val id: UUID, val studentId: UUID, val classId: UUID, val courseCode: String,
    val courseTitle: String, val semester: String, val status: String, val enrolledAt: Instant, val updatedAt: Instant
)
data class CreateScheduleEntryRequest(val studentId: UUID, val classId: UUID, val courseCode: String, val courseTitle: String, val semester: String)
data class UpdateStatusRequest(val status: String)

@RegisterRestClient(configKey = "schedule-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ScheduleServiceClient {
    @POST
    @Path("/internal/schedule-entries")
    fun createEntry(request: CreateScheduleEntryRequest, @HeaderParam("X-Internal-Token") token: String): ScheduleEntryResponse

    @GET
    @Path("/internal/schedule-entries")
    fun findEntry(
        @QueryParam("studentId") studentId: UUID,
        @QueryParam("classId") classId: UUID,
        @QueryParam("status") status: String,
        @HeaderParam("X-Internal-Token") token: String
    ): List<ScheduleEntryResponse>

    @PATCH
    @Path("/internal/schedule-entries/{id}/status")
    fun updateStatus(@PathParam("id") id: UUID, request: UpdateStatusRequest, @HeaderParam("X-Internal-Token") token: String): ScheduleEntryResponse
}
