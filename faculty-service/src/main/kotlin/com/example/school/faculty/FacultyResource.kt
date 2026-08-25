package com.example.school.faculty

import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * Owns and manages faculty records. Reads (list and single-record) are
 * allowed for any authenticated role (ADMIN, FACULTY, STUDENT) since faculty
 * information is not sensitive and students need to browse it. Writes are
 * ADMIN-only.
 */
@Path("/faculty")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class FacultyResource {

    @GET
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun list(): List<FacultyResponse> {
        return Faculty.listAll().map { FacultyResponse.from(it) }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun get(@PathParam("id") id: UUID): Response {
        val faculty = Faculty.findById(id) ?: throw NotFoundException("Faculty $id not found")
        return Response.ok(FacultyResponse.from(faculty)).build()
    }

    @POST
    @RolesAllowed("ADMIN")
    @Transactional
    fun create(@Valid request: CreateFacultyRequest): Response {
        val faculty = Faculty().apply {
            firstName = request.firstName
            lastName = request.lastName
            email = request.email
            department = request.department
            title = request.title
            hireDate = request.hireDate
        }
        faculty.persist()
        return Response.status(Response.Status.CREATED).entity(FacultyResponse.from(faculty)).build()
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Transactional
    fun update(@PathParam("id") id: UUID, @Valid request: UpdateFacultyRequest): Response {
        val faculty = Faculty.findById(id) ?: throw NotFoundException("Faculty $id not found")
        faculty.firstName = request.firstName
        faculty.lastName = request.lastName
        faculty.email = request.email
        faculty.department = request.department
        faculty.title = request.title
        faculty.hireDate = request.hireDate
        return Response.ok(FacultyResponse.from(faculty)).build()
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Transactional
    fun delete(@PathParam("id") id: UUID): Response {
        val faculty = Faculty.findById(id) ?: throw NotFoundException("Faculty $id not found")
        faculty.delete()
        return Response.noContent().build()
    }
}
