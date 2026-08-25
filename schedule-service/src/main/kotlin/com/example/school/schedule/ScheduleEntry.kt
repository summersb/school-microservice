package com.example.school.schedule

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "schedule_entry")
class ScheduleEntry : PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "student_id", nullable = false)
    lateinit var studentId: UUID

    @Column(name = "class_id", nullable = false)
    lateinit var classId: UUID

    @Column(name = "course_code", nullable = false)
    lateinit var courseCode: String

    @Column(name = "course_title", nullable = false)
    lateinit var courseTitle: String

    @Column(name = "semester", nullable = false)
    lateinit var semester: String

    // Plain string (ENROLLED/DROPPED/COMPLETED) rather than an enum, for simplicity.
    @Column(name = "status", nullable = false)
    var status: String = "ENROLLED"

    @Column(name = "enrolled_at", nullable = false)
    var enrolledAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    companion object : PanacheCompanionBase<ScheduleEntry, UUID>
}
