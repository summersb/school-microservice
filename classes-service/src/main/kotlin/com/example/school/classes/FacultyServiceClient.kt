package com.example.school.classes

import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.UUID

/**
 * Only the fields classes-service actually needs from faculty-service's
 * FacultyResponse. Jackson ignores the extra fields (email, department, title,
 * hireDate, createdAt, updatedAt) present in the real response by default.
 */
data class FacultySummary(val id: UUID, val firstName: String, val lastName: String)

@RegisterRestClient(configKey = "faculty-api")
@Produces(MediaType.APPLICATION_JSON)
interface FacultyServiceClient {

    @GET
    @Path("/faculty/{id}")
    fun getFaculty(@PathParam("id") id: UUID, @HeaderParam("Authorization") authorization: String?): FacultySummary
}
