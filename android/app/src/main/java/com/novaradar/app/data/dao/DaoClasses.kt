package com.novaradar.app.data.dao

import androidx.room.*
import com.novaradar.app.data.model.IpSource
import com.novaradar.app.data.model.PortConfig
import com.novaradar.app.data.model.ScanHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface IpSourceDao {
    @Query("SELECT * FROM ip_source ORDER BY id ASC")
    fun getAllIpSources(): Flow<List<IpSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIpSource(source: IpSource)

    @Update
    suspend fun updateIpSource(source: IpSource)

    @Delete
    suspend fun deleteIpSource(source: IpSource)

    @Query("SELECT * FROM ip_source WHERE isEnabled = 1")
    suspend fun getEnabledSources(): List<IpSource>
}

@Dao
interface PortConfigDao {
    @Query("SELECT * FROM port_config ORDER BY port ASC")
    fun getAllPortConfigs(): Flow<List<PortConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortConfig(port: PortConfig)

    @Update
    suspend fun updatePortConfig(port: PortConfig)

    @Query("SELECT * FROM port_config WHERE isEnabled = 1")
    suspend fun getEnabledPorts(): List<PortConfig>
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY id DESC LIMIT 500")
    fun getAllHistory(): Flow<List<ScanHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ScanHistory)

    @Query("DELETE FROM scan_history")
    suspend fun clearHistory()
}
