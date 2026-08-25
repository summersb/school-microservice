package com.example.school.student

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class EnrollmentStatus { ACTIVE, INACTIVE, GRADUATED, WITHDRAWN }

@Entity
@Table(name = "student")
class Student : PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "first_name", nullable = false)
    lateinit var firstName: String

    @Column(name = "last_name", nullable = false)
    lateinit var lastName: String

    @Column(name = "email", nullable = false, unique = true)
    lateinit var email: String

    @Column(name = "date_of_birth", nullable = false)
    lateinit var dateOfBirth: LocalDate

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false)
    var enrollmentStatus: EnrollmentStatus = EnrollmentStatus.ACTIVE

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    companion object : PanacheCompanionBase<Student, UUID>
}
