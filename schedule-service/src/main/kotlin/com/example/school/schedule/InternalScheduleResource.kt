package com.example.school.schedule

import jakarta.annotation.security.PermitAll
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * Internal write API for schedule entries, used only by register-service. Never
 * proxied publicly and never protected by a JWT -- access is gated by
 * InternalAuthFilter checking the X-Internal-Token header, so every method here is
 * @PermitAll at the JAX-RS level.
 */
@Path("/internal/schedule-entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class InternalScheduleResource {

    @POST
    @PermitAll
    @Transactional
    fun create(@Valid request: CreateScheduleEntryRequest): Response {
        val entry = ScheduleEntry().apply {
            studentId = request.studentId
            classId = request.classId
            courseCode = request.courseCode
            courseTitle = request.courseTitle
            semester = request.semester
            status = "ENROLLED"
        }
        entry.persist()
        return Response.status(Response.Status.CREATED).entity(ScheduleEntryResponse.from(entry)).build()
    }

    @GET
    @PermitAll
    fun find(
        @QueryParam("studentId") studentId: UUID,
        @QueryParam("classId") classId: UUID,
        @QueryParam("status") status: String?
    ): List<ScheduleEntryResponse> {
        val entries = if (status != null) {
            ScheduleEntry.list("studentId = ?1 and classId = ?2 and status = ?3", studentId, classId, status)
        } else {
            ScheduleEntry.list("studentId = ?1 and classId = ?2", studentId, classId)
        }
        return entries.map { ScheduleEntryResponse.from(it) }
    }

    @PATCH
    @Path("/{id}/status")
    @PermitAll
    @Transactional
    fun updateStatus(@PathParam("id") id: UUID, @Valid request: UpdateStatusRequest): Response {
        val entry = ScheduleEntry.findById(id) ?: throw NotFoundException("Schedule entry $id not found")
        entry.status = request.status
        return Response.ok(ScheduleEntryResponse.from(entry)).build()
    }
}
