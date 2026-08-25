package com.example.school.schedule

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class ScheduleEntryResponse(
    val id: UUID,
    val studentId: UUID,
    val classId: UUID,
    val courseCode: String,
    val courseTitle: String,
    val semester: String,
    val status: String,
    val enrolledAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(e: ScheduleEntry) = ScheduleEntryResponse(
            id = e.id!!,
            studentId = e.studentId,
            classId = e.classId,
            courseCode = e.courseCode,
            courseTitle = e.courseTitle,
            semester = e.semester,
            status = e.status,
            enrolledAt = e.enrolledAt,
            updatedAt = e.updatedAt
        )
    }
}

data class CreateScheduleEntryRequest(
    @field:NotNull val studentId: UUID,
    @field:NotNull val classId: UUID,
    @field:NotBlank val courseCode: String,
    @field:NotBlank val courseTitle: String,
    @field:NotBlank val semester: String
)

data class UpdateStatusRequest(
    @field:NotBlank val status: String
)
