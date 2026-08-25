package com.example.school.faculty

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class FacultyResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val department: String,
    val title: String,
    val hireDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(f: Faculty) = FacultyResponse(
            id = f.id!!,
            firstName = f.firstName,
            lastName = f.lastName,
            email = f.email,
            department = f.department,
            title = f.title,
            hireDate = f.hireDate,
            createdAt = f.createdAt,
            updatedAt = f.updatedAt
        )
    }
}

data class CreateFacultyRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val department: String,
    @field:NotBlank val title: String,
    @field:NotNull val hireDate: LocalDate
)

data class UpdateFacultyRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val department: String,
    @field:NotBlank val title: String,
    @field:NotNull val hireDate: LocalDate
)
