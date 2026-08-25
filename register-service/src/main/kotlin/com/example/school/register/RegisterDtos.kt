package com.example.school.register

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class EnrollRequest(@field:NotNull val studentId: UUID, @field:NotNull val classId: UUID)
data class DropRequest(@field:NotNull val studentId: UUID, @field:NotNull val classId: UUID)
data class EnrollmentResult(val scheduleEntryId: UUID, val studentId: UUID, val classId: UUID, val courseCode: String, val courseTitle: String, val semester: String, val status: String)
