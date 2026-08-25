package com.example.school.classes

import jakarta.annotation.security.PermitAll
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * Internal seat-reservation API, used only by register-service. Never proxied
 * publicly and never protected by a JWT -- access is gated by InternalAuthFilter
 * checking the X-Internal-Token header, so every method here is @PermitAll at the
 * JAX-RS level.
 */
@Path("/internal/classes")
@Produces(MediaType.APPLICATION_JSON)
class InternalClassResource {

    @POST
    @Path("/{id}/reserve-seat")
    @PermitAll
    @Transactional
    fun reserveSeat(@PathParam("id") id: UUID): Response {
        val entity = CourseClass.findById(id) ?: throw NotFoundException("Class $id not found")
        if (entity.seatsTaken >= entity.capacity) {
            throw jakarta.ws.rs.WebApplicationException("Class ${entity.courseCode} is full", Response.Status.CONFLICT)
        }
        entity.seatsTaken += 1
        return Response.ok(SeatOperationResponse(id, entity.capacity, entity.seatsTaken, entity.capacity - entity.seatsTaken)).build()
    }

    @POST
    @Path("/{id}/release-seat")
    @PermitAll
    @Transactional
    fun releaseSeat(@PathParam("id") id: UUID): Response {
        val entity = CourseClass.findById(id) ?: throw NotFoundException("Class $id not found")
        entity.seatsTaken = maxOf(0, entity.seatsTaken - 1)
        return Response.ok(SeatOperationResponse(id, entity.capacity, entity.seatsTaken, entity.capacity - entity.seatsTaken)).build()
    }
}
