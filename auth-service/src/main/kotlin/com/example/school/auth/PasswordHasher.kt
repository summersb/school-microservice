package com.example.school.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PasswordHasher {
    fun hash(plain: String): String = BCrypt.withDefaults().hashToString(10, plain.toCharArray())
    fun verify(plain: String, hash: String): Boolean = BCrypt.verifyer().verify(plain.toCharArray(), hash).verified
}
