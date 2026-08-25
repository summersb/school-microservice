package com.example.school.auth

import io.smallrye.jwt.algorithm.SignatureAlgorithm
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

/**
 * Builds signed HS256 JWTs manually with an explicit [SecretKeySpec] built from
 * `app.jwt.secret`, rather than relying on the `smallrye.jwt.sign.key.location`
 * config indirection -- this guarantees signing and verification always use the
 * exact same key bytes.
 *
 * `app.jwt.secret` holds the base64url-encoded key, the same value that every
 * service inlines as the `k` member of the "oct" JWK in
 * `mp.jwt.verify.publickey`. Decoding it here means the signing key and the
 * verification key are byte-identical by construction, driven by the single
 * `JWT_SECRET` env var.
 */
@ApplicationScoped
class JwtIssuer(
    @ConfigProperty(name = "app.jwt.secret") private val secret: String,
    @ConfigProperty(name = "app.jwt.issuer") private val issuer: String,
    @ConfigProperty(name = "app.jwt.expiration-hours") private val expirationHours: Long
) {
    /** Returns the signed token together with its lifetime in seconds. */
    fun issueToken(user: AppUser): Pair<String, Long> {
        val key = SecretKeySpec(Base64.getUrlDecoder().decode(secret), "HmacSHA256")
        val expiresInSeconds = Duration.ofHours(expirationHours).toSeconds()

        var builder = Jwt.issuer(issuer)
            .subject(user.id.toString())
            .upn(user.username)
            .groups(setOf(user.role))
            .claim("username", user.username)
            .expiresIn(expiresInSeconds)

        user.studentId?.let { builder = builder.claim("studentId", it.toString()) }
        user.facultyId?.let { builder = builder.claim("facultyId", it.toString()) }

        val token = builder.jws()
            .algorithm(SignatureAlgorithm.HS256)
            .sign(key)

        return token to expiresInSeconds
    }
}
