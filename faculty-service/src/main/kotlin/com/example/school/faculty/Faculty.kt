package com.example.school.faculty

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "faculty")
class Faculty : PanacheEntityBase {

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

    @Column(name = "department", nullable = false)
    lateinit var department: String

    @Column(name = "title", nullable = false)
    lateinit var title: String

    @Column(name = "hire_date", nullable = false)
    lateinit var hireDate: LocalDate

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    companion object : PanacheCompanionBase<Faculty, UUID>
}
