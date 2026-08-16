package id.rona.app.data.crypto

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test

class SqlCipherLibraryLoaderTest {

    @Test
    fun loadsExactlyOnce() {
        val count = AtomicInteger(0)
        val loader = SqlCipherLibraryLoader { count.incrementAndGet() }

        loader.load()
        loader.load()
        loader.load()

        assertThat(count.get()).isEqualTo(1)
    }

    @Test
    fun loadIsIdempotentAcrossInterleavedCalls() {
        val count = AtomicInteger(0)
        val loader = SqlCipherLibraryLoader { count.incrementAndGet() }
        loader.load()
        loader.load()
        assertThat(count.get()).isEqualTo(1)
    }

    @Test
    fun concurrentCallsLoadOnlyOnce() {
        val count = AtomicInteger(0)
        val loader = SqlCipherLibraryLoader { count.incrementAndGet() }

        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)

        repeat(threads) {
            pool.execute {
                ready.countDown()
                start.await()
                loader.load()
            }
        }
        ready.await()
        start.countDown()
        pool.shutdown()
        assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue()

        assertThat(count.get()).isEqualTo(1)
    }

    @Test
    fun failurePropagatesAndDoesNotMarkLoaded() {
        val count = AtomicInteger(0)
        val loader = SqlCipherLibraryLoader {
            count.incrementAndGet()
            throw UnsatisfiedLinkError("missing native library")
        }

        val first = runCatching { loader.load() }.exceptionOrNull()
        assertThat(first).isInstanceOf(UnsatisfiedLinkError::class.java)

        // Not marked loaded: a subsequent retry attempts loading again.
        val second = runCatching { loader.load() }.exceptionOrNull()
        assertThat(second).isInstanceOf(UnsatisfiedLinkError::class.java)
        assertThat(count.get()).isEqualTo(2)
    }

    @Test
    fun successAfterFailureRetriesAndThenSticks() {
        val count = AtomicInteger(0)
        val loader = SqlCipherLibraryLoader {
            if (count.incrementAndGet() == 1) {
                throw UnsatisfiedLinkError("first attempt fails")
            }
        }

        assertThat(runCatching { loader.load() }.isFailure).isTrue()
        loader.load()
        loader.load()

        assertThat(count.get()).isEqualTo(2)
    }

    @Test
    fun libraryNameIsSqlcipher() {
        assertThat(SqlCipherLibraryLoader.LIBRARY_NAME).isEqualTo("sqlcipher")
    }
}
