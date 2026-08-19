package id.rona.app.data.db

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for running database transactions, enabling JVM unit tests without Room SQLite stubs.
 */
interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val db: RonaDatabase,
) : TransactionRunner {
    override suspend fun <T> invoke(block: suspend () -> T): T = db.withTransaction(block)
}
