package com.example.school.schedule

import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

/**
 * Public, read-only view of each student's class schedule. Reads are allowed for
 * ADMIN and FACULTY (any student) and for STUDENT (their own schedule only,
 * resolved from the `studentId` claim on their JWT). All writes live on
 * InternalScheduleResource under `/internal/...`, used only by register-service.
 */
@Path("/schedules")
@Produces(MediaType.APPLICATION_JSON)
class ScheduleResource(private val jwt: JsonWebToken) {

    @GET
    @Path("/student/{studentId}")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun all(@PathParam("studentId") studentId: UUID): List<ScheduleEntryResponse> {
        assertCanAccess(studentId)
        return ScheduleEntry.list("studentId", studentId).map { ScheduleEntryResponse.from(it) }
    }

    @GET
    @Path("/student/{studentId}/current")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun current(@PathParam("studentId") studentId: UUID): List<ScheduleEntryResponse> {
        assertCanAccess(studentId)
        return ScheduleEntry.list("studentId = ?1 and status = ?2", studentId, "ENROLLED").map { ScheduleEntryResponse.from(it) }
    }

    @GET
    @Path("/student/{studentId}/history")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun history(@PathParam("studentId") studentId: UUID): List<ScheduleEntryResponse> {
        assertCanAccess(studentId)
        return ScheduleEntry.list("studentId = ?1 and status != ?2", studentId, "ENROLLED").map { ScheduleEntryResponse.from(it) }
    }

    private fun assertCanAccess(studentId: UUID) {
        if (jwt.groups.contains("STUDENT") && !jwt.groups.contains("ADMIN") && !jwt.groups.contains("FACULTY")) {
            val ownStudentId = jwt.getClaim<String>("studentId")
            if (ownStudentId == null || ownStudentId != studentId.toString()) {
                throw ForbiddenException("Students may only view their own schedule")
            }
        }
    }
}
