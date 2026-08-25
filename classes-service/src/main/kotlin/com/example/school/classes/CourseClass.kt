package com.example.school.classes

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
@Table(name = "course_class")
class CourseClass : PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "course_code", nullable = false)
    lateinit var courseCode: String

    @Column(name = "title", nullable = false)
    lateinit var title: String

    @Column(name = "description")
    var description: String? = null

    @Column(name = "semester", nullable = false)
    lateinit var semester: String

    @Column(name = "faculty_id", nullable = false)
    lateinit var facultyId: UUID

    @Column(name = "faculty_name", nullable = false)
    lateinit var facultyName: String

    @Column(name = "capacity", nullable = false)
    var capacity: Int = 0

    @Column(name = "seats_taken", nullable = false)
    var seatsTaken: Int = 0

    @Column(name = "room")
    var room: String? = null

    @Column(name = "day_of_week")
    var dayOfWeek: String? = null

    @Column(name = "start_time")
    var startTime: String? = null

    @Column(name = "end_time")
    var endTime: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    companion object : PanacheCompanionBase<CourseClass, UUID>
}
