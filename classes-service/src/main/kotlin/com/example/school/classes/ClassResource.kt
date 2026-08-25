package com.example.school.classes

import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.UUID

/**
 * PUBLIC catalog API: any authenticated role (ADMIN, FACULTY, STUDENT) can browse
 * classes. Writes are ADMIN-only and validate the given facultyId against
 * faculty-service, forwarding the caller's own Authorization header since
 * faculty-service's GET /faculty/{id} requires an authenticated role.
 */
@Path("/classes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ClassResource(
    @RestClient private val facultyClient: FacultyServiceClient
) {

    @GET
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun list(@QueryParam("semester") semester: String?): List<ClassResponse> {
        val classes = if (semester != null) CourseClass.list("semester", semester) else CourseClass.listAll()
        return classes.map { ClassResponse.from(it) }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun get(@PathParam("id") id: UUID): ClassResponse {
        val c = CourseClass.findById(id) ?: throw NotFoundException("Class $id not found")
        return ClassResponse.from(c)
    }

    @POST
    @RolesAllowed("ADMIN")
    @Transactional
    fun create(@Valid request: CreateClassRequest, @HeaderParam("Authorization") authorization: String?): Response {
        val faculty = try {
            facultyClient.getFaculty(request.facultyId, authorization)
        } catch (e: jakarta.ws.rs.WebApplicationException) {
            throw jakarta.ws.rs.WebApplicationException("facultyId ${request.facultyId} does not exist", Response.Status.BAD_REQUEST)
        }
        val entity = CourseClass().apply {
            courseCode = request.courseCode
            title = request.title
            description = request.description
            semester = request.semester
            facultyId = request.facultyId
            facultyName = "${faculty.firstName} ${faculty.lastName}"
            capacity = request.capacity
            seatsTaken = 0
            room = request.room
            dayOfWeek = request.dayOfWeek
            startTime = request.startTime
            endTime = request.endTime
        }
        entity.persist()
        return Response.status(Response.Status.CREATED).entity(ClassResponse.from(entity)).build()
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Transactional
    fun update(@PathParam("id") id: UUID, @Valid request: UpdateClassRequest, @HeaderParam("Authorization") authorization: String?): Response {
        val entity = CourseClass.findById(id) ?: throw NotFoundException("Class $id not found")
        val faculty = try {
            facultyClient.getFaculty(request.facultyId, authorization)
        } catch (e: jakarta.ws.rs.WebApplicationException) {
            throw jakarta.ws.rs.WebApplicationException("facultyId ${request.facultyId} does not exist", Response.Status.BAD_REQUEST)
        }
        entity.courseCode = request.courseCode
        entity.title = request.title
        entity.description = request.description
        entity.semester = request.semester
        entity.facultyId = request.facultyId
        entity.facultyName = "${faculty.firstName} ${faculty.lastName}"
        entity.capacity = request.capacity
        entity.room = request.room
        entity.dayOfWeek = request.dayOfWeek
        entity.startTime = request.startTime
        entity.endTime = request.endTime
        return Response.ok(ClassResponse.from(entity)).build()
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Transactional
    fun delete(@PathParam("id") id: UUID): Response {
        val entity = CourseClass.findById(id) ?: throw NotFoundException("Class $id not found")
        entity.delete()
        return Response.noContent().build()
    }
}
