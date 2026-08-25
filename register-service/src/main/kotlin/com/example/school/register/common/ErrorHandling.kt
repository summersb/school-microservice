package com.example.school.register.common

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

/** Consistent JSON error shape used across every service in the system. */
data class ErrorResponse(val error: String, val status: Int)

@Provider
class GenericExceptionMapper : ExceptionMapper<Exception> {
    private val log = Logger.getLogger(GenericExceptionMapper::class.java)

    override fun toResponse(exception: Exception): Response {
        return when (exception) {
            is WebApplicationException -> {
                val status = exception.response.status
                Response.status(status)
                    .entity(ErrorResponse(exception.message ?: "Error", status))
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            }
            is ConstraintViolationException -> {
                val message = exception.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" }
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse(message, 400))
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            }
            else -> {
                log.error("Unhandled exception", exception)
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponse("Internal server error", 500))
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            }
        }
    }
}
