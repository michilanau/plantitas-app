package org.mlanau.project.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.mlanau.project.plant.infrastructure.persistence.PlantDb

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = PlantDb.Schema,
            name = "plant.db",
            // NativeSqliteDriver pools multiple SQLite connections; a one-off
            // `driver.execute(null, "PRAGMA foreign_keys = ON;", 0)` only reaches whichever single
            // connection happens to run it, not the pool. This applies to every connection it opens,
            // matching AndroidSqliteDriver.Callback.onOpen on the Android side.
            onConfiguration = { config ->
                config.copy(extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true))
            }
        )
    }
}
