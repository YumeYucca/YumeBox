/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.data.store.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Builds the traffic-statistics Room database and returns its DAO.
 *
 * Keeping construction here confines the `TrafficDatabase` / `RoomDatabase` / `Room` types to `:data`; callers (e.g.
 * `:app` DI wiring) only ever touch [TrafficStatisticsDao], which exposes no Room supertypes in its signatures. This
 * lets `:data` keep Room as an `implementation` dependency without leaking it onto the `:app` compile classpath.
 */
fun createTrafficStatisticsDao(context: Context): TrafficStatisticsDao =
    Room.databaseBuilder(
            context.applicationContext,
            TrafficDatabase::class.java,
            TrafficDatabase.DATABASE_NAME,
        )
        .addMigrations(TRAFFIC_MIGRATION_1_2)
        .build()
        .trafficStatisticsDao()

private val TRAFFIC_MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `traffic_hourly` (
                    `hour_start_millis` INTEGER NOT NULL,
                    `total_upload` INTEGER NOT NULL,
                    `total_download` INTEGER NOT NULL,
                    PRIMARY KEY(`hour_start_millis`)
                )
                """
                    .trimIndent()
            )
        }
    }
