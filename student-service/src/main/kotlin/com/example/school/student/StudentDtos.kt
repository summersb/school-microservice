package com.example.school.student

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class StudentResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val dateOfBirth: LocalDate,
    val enrollmentStatus: EnrollmentStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(s: Student) = StudentResponse(
            id = s.id!!,
            firstName = s.firstName,
            lastName = s.lastName,
            email = s.email,
            dateOfBirth = s.dateOfBirth,
            enrollmentStatus = s.enrollmentStatus,
            createdAt = s.createdAt,
            updatedAt = s.updatedAt
        )
    }
}

data class CreateStudentRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotNull @field:Past val dateOfBirth: LocalDate,
    val enrollmentStatus: EnrollmentStatus = EnrollmentStatus.ACTIVE
)

data class UpdateStudentRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotNull @field:Past val dateOfBirth: LocalDate,
    @field:NotNull val enrollmentStatus: EnrollmentStatus
)
