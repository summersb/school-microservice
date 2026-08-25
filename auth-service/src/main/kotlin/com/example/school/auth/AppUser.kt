package com.example.school.auth

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_user")
class AppUser : PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "username", nullable = false, unique = true)
    lateinit var username: String

    @Column(name = "password_hash", nullable = false)
    lateinit var passwordHash: String

    @Column(name = "role", nullable = false)
    lateinit var role: String

    @Column(name = "student_id")
    var studentId: UUID? = null

    @Column(name = "faculty_id")
    var facultyId: UUID? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    companion object : PanacheCompanionBase<AppUser, UUID> {
        fun findByUsername(username: String): AppUser? = find("username", username).firstResult()
    }
}
