package com.example.school.classes.common

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import org.jboss.logging.Logger

/** Consistent JSON error shape used across every service in the system. */
data class ErrorResponse(val error: String, val status: Int)

@Provider
class GenericExceptionMapper : ExceptionMapper<Exception> {
    private val log = Logger.getLogger(GenericExceptionMapper::class.java)

    override fun toResponse(exception: Exception): Response {
        val dbConstraint = findDatabaseConstraintViolation(exception)
        return when {
            exception is WebApplicationException -> {
                val status = exception.response.status
                Response.status(status)
                    .entity(ErrorResponse(exception.message ?: "Error", status))
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            }
            exception is ConstraintViolationException -> {
                val message = exception.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" }
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse(message, 400))
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            }
            // Now that every table lives in one database, writes can be refused by a
            // foreign key (e.g. deleting a faculty member who still teaches a class) or
            // by a unique key. That is a conflict with existing data, not a server bug.
            dbConstraint != null -> {
                log.warnf("Database constraint '%s' rejected the request", dbConstraint.constraintName)
                val message = "Request conflicts with existing data (constraint: ${dbConstraint.constraintName})"
                Response.status(Response.Status.CONFLICT)
                    .entity(ErrorResponse(message, 409))
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

    /**
     * Hibernate reports constraint violations at flush/commit time, so by the time the
     * exception reaches here it is wrapped in a few layers of persistence/transaction
     * exceptions -- hence the walk down the cause chain.
     */
    private fun findDatabaseConstraintViolation(exception: Throwable): HibernateConstraintViolationException? {
        var cause: Throwable? = exception
        while (cause != null) {
            if (cause is HibernateConstraintViolationException) return cause
            cause = cause.cause
        }
        return null
    }
}
