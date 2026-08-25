package com.example.school.register

import jakarta.enterprise.context.ApplicationScoped
import java.security.MessageDigest
import java.util.UUID
import javax.sql.DataSource

/**
 * Outcome of trying to claim an Idempotency-Key. [Acquired] means the caller
 * owns the key and should run the request; the other three mean it should not.
 */
sealed interface IdempotencyClaim {
    /** No prior request has used this key -- go ahead and do the work. */
    object Acquired : IdempotencyClaim

    /** This key was already used, but for a request with different content. */
    object Conflict : IdempotencyClaim

    /** This key is claimed and that request hasn't finished yet. */
    object InProgress : IdempotencyClaim

    /** This key already completed -- here's the exact response to return again. */
    data class Replay(val status: Int, val body: String) : IdempotencyClaim
}

/**
 * Backs the Idempotency-Key header on POST /register/enroll (see
 * RegisterResource.enroll). This is deliberately raw JDBC against the
 * idempotency_key table rather than a Panache entity: it's bookkeeping for the
 * HTTP layer, not a business entity, and it needs to be durable -- an
 * in-memory map would lose exactly the record a retry needs at the moment
 * this service crashes and restarts, which is precisely when retries happen.
 */
@ApplicationScoped
class IdempotencyKeyStore(private val dataSource: DataSource) {

    /**
     * A stable fingerprint of the parts of the request that must match for a
     * key reuse to count as "the same request retried" rather than "this key
     * was reused for something else" (a client bug, or a stolen/reused key).
     */
    fun hash(studentId: UUID, classId: UUID): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$studentId:$classId".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Atomically claims [key], or reports what's already known about it. The
     * INSERT ... ON CONFLICT DO NOTHING is the atomic part: two concurrent
     * requests carrying the same key can both race into this method, but
     * Postgres guarantees at most one of the two INSERTs actually adds a row --
     * that row's owner is the only one who proceeds to do the work.
     */
    fun claim(key: UUID, requestHash: String): IdempotencyClaim {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO idempotency_key (key, request_hash) VALUES (?, ?) ON CONFLICT (key) DO NOTHING"
            ).use { insert ->
                insert.setObject(1, key)
                insert.setString(2, requestHash)
                if (insert.executeUpdate() == 1) {
                    return IdempotencyClaim.Acquired
                }
            }

            conn.prepareStatement(
                "SELECT request_hash, response_status, response_body FROM idempotency_key WHERE key = ?"
            ).use { select ->
                select.setObject(1, key)
                select.executeQuery().use { rs ->
                    check(rs.next()) { "idempotency_key row for $key vanished between insert and select" }
                    if (rs.getString("request_hash") != requestHash) {
                        return IdempotencyClaim.Conflict
                    }
                    val status = rs.getInt("response_status")
                    if (rs.wasNull()) {
                        return IdempotencyClaim.InProgress
                    }
                    return IdempotencyClaim.Replay(status, rs.getString("response_body"))
                }
            }
        }
    }

    /** Records the outcome so a retry with the same key gets this back instead of re-running anything. */
    fun complete(key: UUID, status: Int, body: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE idempotency_key SET response_status = ?, response_body = ? WHERE key = ?"
            ).use { update ->
                update.setInt(1, status)
                update.setString(2, body)
                update.setObject(3, key)
                update.executeUpdate()
            }
        }
    }

    /**
     * Frees a claimed key that never reached [complete] -- the request failed
     * (a business error like "no seats" or a downstream failure), so it was
     * never actually satisfied. Only successful completions get cached; this
     * lets a client retry with the same key instead of being stuck replaying a
     * stale failure forever.
     */
    fun release(key: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM idempotency_key WHERE key = ?").use { delete ->
                delete.setObject(1, key)
                delete.executeUpdate()
            }
        }
    }
}
