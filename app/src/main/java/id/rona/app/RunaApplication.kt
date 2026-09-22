package id.rona.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.rona.app.data.crypto.SqlCipherNativeLoader
import javax.inject.Inject

@HiltAndroidApp
class RunaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Must precede any SQLCipher/Room use — including Hilt-created
        // singletons, workers, and database construction. Idempotent.
        SqlCipherNativeLoader.load()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
