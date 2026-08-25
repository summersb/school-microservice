package com.example.school.classes

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class ClassResponse(
    val id: UUID,
    val courseCode: String,
    val title: String,
    val description: String?,
    val semester: String,
    val facultyId: UUID,
    val facultyName: String,
    val capacity: Int,
    val seatsTaken: Int,
    val seatsAvailable: Int,
    val room: String?,
    val dayOfWeek: String?,
    val startTime: String?,
    val endTime: String?
) {
    companion object {
        fun from(c: CourseClass) = ClassResponse(
            c.id!!, c.courseCode, c.title, c.description, c.semester, c.facultyId, c.facultyName,
            c.capacity, c.seatsTaken, c.capacity - c.seatsTaken, c.room, c.dayOfWeek, c.startTime, c.endTime
        )
    }
}

data class CreateClassRequest(
    @field:NotBlank val courseCode: String,
    @field:NotBlank val title: String,
    val description: String?,
    @field:NotBlank val semester: String,
    @field:NotNull val facultyId: UUID,
    @field:Min(1) val capacity: Int,
    val room: String?,
    val dayOfWeek: String?,
    val startTime: String?,
    val endTime: String?
)

data class UpdateClassRequest(
    @field:NotBlank val courseCode: String,
    @field:NotBlank val title: String,
    val description: String?,
    @field:NotBlank val semester: String,
    @field:NotNull val facultyId: UUID,
    @field:Min(1) val capacity: Int,
    val room: String?,
    val dayOfWeek: String?,
    val startTime: String?,
    val endTime: String?
)

data class SeatOperationResponse(
    val classId: UUID,
    val capacity: Int,
    val seatsTaken: Int,
    val seatsAvailable: Int
)
