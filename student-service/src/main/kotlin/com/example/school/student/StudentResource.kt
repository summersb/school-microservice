package com.example.school.student

import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

/**
 * Owns and manages student records. Reads are allowed for ADMIN and FACULTY
 * (any student) and for STUDENT (their own record only, resolved from the
 * `studentId` claim on their JWT). Writes are ADMIN-only.
 */
@Path("/students")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class StudentResource(
    private val jwt: JsonWebToken
) {

    @GET
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun list(@QueryParam("status") status: EnrollmentStatus?): List<StudentResponse> {
        if (jwt.groups.contains("STUDENT") && !jwt.groups.contains("ADMIN") && !jwt.groups.contains("FACULTY")) {
            throw ForbiddenException("Students may not list all student records")
        }
        val students = if (status != null) Student.list("enrollmentStatus", status) else Student.listAll()
        return students.map { StudentResponse.from(it) }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun get(@PathParam("id") id: UUID): Response {
        assertCanAccess(id)
        val student = Student.findById(id) ?: throw NotFoundException("Student $id not found")
        return Response.ok(StudentResponse.from(student)).build()
    }

    @POST
    @RolesAllowed("ADMIN")
    @Transactional
    fun create(@Valid request: CreateStudentRequest): Response {
        val student = Student().apply {
            firstName = request.firstName
            lastName = request.lastName
            email = request.email
            dateOfBirth = request.dateOfBirth
            enrollmentStatus = request.enrollmentStatus
        }
        student.persist()
        return Response.status(Response.Status.CREATED).entity(StudentResponse.from(student)).build()
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Transactional
    fun update(@PathParam("id") id: UUID, @Valid request: UpdateStudentRequest): Response {
        val student = Student.findById(id) ?: throw NotFoundException("Student $id not found")
        student.firstName = request.firstName
        student.lastName = request.lastName
        student.email = request.email
        student.dateOfBirth = request.dateOfBirth
        student.enrollmentStatus = request.enrollmentStatus
        return Response.ok(StudentResponse.from(student)).build()
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Transactional
    fun delete(@PathParam("id") id: UUID): Response {
        val student = Student.findById(id) ?: throw NotFoundException("Student $id not found")
        student.delete()
        return Response.noContent().build()
    }

    private fun assertCanAccess(id: UUID) {
        if (jwt.groups.contains("STUDENT") && !jwt.groups.contains("ADMIN") && !jwt.groups.contains("FACULTY")) {
            val ownStudentId = jwt.getClaim<String>("studentId")
            if (ownStudentId == null || ownStudentId != id.toString()) {
                throw ForbiddenException("Students may only view their own record")
            }
        }
    }
}
