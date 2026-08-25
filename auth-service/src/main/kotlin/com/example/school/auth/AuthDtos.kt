package com.example.school.auth

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val token: String,
    val expiresInSeconds: Long,
    val username: String,
    val role: String,
    val studentId: UUID?,
    val facultyId: UUID?
)

data class RegisterRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
    @field:NotBlank val role: String,
    val studentId: UUID? = null,
    val facultyId: UUID? = null
)

data class MeResponse(
    val username: String,
    val role: String,
    val studentId: UUID?,
    val facultyId: UUID?
)
