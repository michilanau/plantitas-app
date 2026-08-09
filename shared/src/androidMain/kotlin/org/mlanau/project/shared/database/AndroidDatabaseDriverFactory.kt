package org.mlanau.project.shared.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.mlanau.project.plant.infrastructure.persistence.PlantDb

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PlantDb.Schema,
            context = context,
            name = "plant.db",
            callback = object : AndroidSqliteDriver.Callback(PlantDb.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }
}
