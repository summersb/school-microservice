package com.example.school.classes

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * Protects every path under `/internal/` with a shared-secret header instead of a
 * JWT. Those endpoints are called only by register-service, never by end users and
 * never proxied publicly, so there is no JWT on the request to validate.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
class InternalAuthFilter(
    @ConfigProperty(name = "app.internal.token") private val expectedToken: String
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path.trimStart('/')
        if (!path.startsWith("internal/")) return

        val provided = requestContext.getHeaderString("X-Internal-Token")
        if (provided == null || provided != expectedToken) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(mapOf("error" to "Missing or invalid internal service token", "status" to 401))
                    .build()
            )
        }
    }
}
