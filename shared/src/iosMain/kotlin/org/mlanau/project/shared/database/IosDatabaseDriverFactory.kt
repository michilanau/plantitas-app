package org.mlanau.project.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.mlanau.project.plant.infrastructure.persistence.PlantDb

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(PlantDb.Schema, "plant.db")
    }
}
