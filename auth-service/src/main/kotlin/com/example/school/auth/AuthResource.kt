package com.example.school.auth

import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AuthResource(
    private val hasher: PasswordHasher,
    private val jwtIssuer: JwtIssuer,
    private val jwt: JsonWebToken
) {

    @POST
    @Path("/login")
    @PermitAll
    fun login(@Valid request: LoginRequest): Response {
        val user = AppUser.findByUsername(request.username)
            ?: throw WebApplicationException("Invalid username or password", Response.Status.UNAUTHORIZED)
        if (!hasher.verify(request.password, user.passwordHash)) {
            throw WebApplicationException("Invalid username or password", Response.Status.UNAUTHORIZED)
        }
        val (token, expiresIn) = jwtIssuer.issueToken(user)
        return Response.ok(LoginResponse(token, expiresIn, user.username, user.role, user.studentId, user.facultyId)).build()
    }

    @POST
    @Path("/register")
    @PermitAll
    @Transactional
    fun register(@Valid request: RegisterRequest): Response {
        if (AppUser.findByUsername(request.username) != null) {
            throw WebApplicationException("Username already taken", Response.Status.CONFLICT)
        }
        val user = AppUser().apply {
            username = request.username
            passwordHash = hasher.hash(request.password)
            role = request.role
            studentId = request.studentId
            facultyId = request.facultyId
        }
        user.persist()
        return Response.status(Response.Status.CREATED)
            .entity(MeResponse(user.username, user.role, user.studentId, user.facultyId))
            .build()
    }

    @GET
    @Path("/me")
    @RolesAllowed("ADMIN", "FACULTY", "STUDENT")
    fun me(): MeResponse {
        val studentId = jwt.getClaim<String>("studentId")?.let { UUID.fromString(it) }
        val facultyId = jwt.getClaim<String>("facultyId")?.let { UUID.fromString(it) }
        return MeResponse(jwt.getClaim("username"), jwt.groups.first(), studentId, facultyId)
    }
}
