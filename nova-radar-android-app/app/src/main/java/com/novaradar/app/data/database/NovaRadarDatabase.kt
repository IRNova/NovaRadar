package com.novaradar.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.novaradar.app.data.dao.IpSourceDao
import com.novaradar.app.data.dao.PortConfigDao
import com.novaradar.app.data.dao.ScanHistoryDao
import com.novaradar.app.data.model.IpSource
import com.novaradar.app.data.model.PortConfig
import com.novaradar.app.data.model.ScanHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [IpSource::class, PortConfig::class, ScanHistory::class],
    version = 4,
    exportSchema = false
)
abstract class NovaRadarDatabase : RoomDatabase() {
    abstract fun ipSourceDao(): IpSourceDao
    abstract fun portConfigDao(): PortConfigDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NovaRadarDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NovaRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaRadarDatabase::class.java,
                    "nova_radar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                // Populate defaults on first creation (Room's onCreate fires during build(),
                // but at that point INSTANCE is null, so a callback can't access it).
                scope.launch(Dispatchers.IO) {
                    if (instance.ipSourceDao().getAllIpSources().first().isEmpty()) {
                        populateDatabase(instance)
                    }
                }

                instance
            }
        }

        private suspend fun populateDatabase(db: NovaRadarDatabase) {
            val defaultPorts = listOf(
                PortConfig(80, true),
                PortConfig(443, true),
                PortConfig(2053, false),
                PortConfig(2083, false),
                PortConfig(2087, false),
                PortConfig(2096, false),
                PortConfig(8443, false),
                PortConfig(2052, false),
                PortConfig(2082, false),
                PortConfig(2086, false),
                PortConfig(2095, false),
                PortConfig(8080, false)
            )
            val portDao = db.portConfigDao()
            defaultPorts.forEach { portDao.insertPortConfig(it) }

            val cloudflareCidrs = listOf(
                "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
                "104.16.0.0/13", "104.24.0.0/14",
                "108.162.192.0/18", "131.0.72.0/22", "141.101.64.0/18",
                "162.158.0.0/15", "172.64.0.0/13", "173.245.48.0/20",
                "188.114.96.0/20", "190.93.240.0/20", "197.234.240.0/22",
                "198.41.128.0/17",
                "5.226.176.0/21", "45.67.215.0/24", "45.85.118.0/23",
                "45.95.240.0/22", "63.141.128.0/24", "64.68.192.0/20",
                "66.81.240.0/20", "69.84.182.0/23", "89.116.250.0/24",
                "94.140.0.0/24", "95.179.154.0/23", "104.28.0.0/14",
                "108.61.0.0/16", "154.84.175.0/24", "159.246.0.0/16",
                "160.153.0.0/16", "185.109.21.0/24", "185.62.140.0/22",
                "188.42.0.0/16", "191.101.128.0/17", "192.200.160.0/24",
                "192.0.48.0/21", "192.0.56.0/23", "192.0.60.0/23",
                "193.9.49.0/24", "198.62.62.0/23", "203.22.223.0/24",
                "203.23.104.0/24", "203.23.106.0/24", "208.103.161.0/24"
            )

            val akamaiCidrs = listOf(
                "2.16.0.0/13", "23.0.0.0/12", "23.32.0.0/11",
                "23.64.0.0/14", "23.72.0.0/13", "23.192.0.0/11"
            )

            val vercelCidrs = listOf(
                "64.29.17.0/24", "64.239.109.0/24", "64.239.123.0/24",
                "66.33.60.0/24", "76.76.21.0/24", "143.13.0.0/16",
                "155.121.0.0/16", "198.169.1.0/24", "198.169.2.0/24",
                "216.150.1.0/24", "216.150.16.0/24", "216.198.79.0/24",
                "216.230.84.0/24", "216.230.86.0/24"
            )

            val defaultSources = listOf(
                IpSource(1, "Cloudflare", "کلودفلر", cloudflareCidrs.joinToString(","), "", "cidr", true),
                IpSource(2, "Akamai", "آکامای", akamaiCidrs.joinToString(","), "", "cidr", false),
                IpSource(3, "Vercel", "ورسل", vercelCidrs.joinToString(","), "", "cidr", false)
            )
            val ipSourceDao = db.ipSourceDao()
            defaultSources.forEach { ipSourceDao.insertIpSource(it) }
        }
    }
}
