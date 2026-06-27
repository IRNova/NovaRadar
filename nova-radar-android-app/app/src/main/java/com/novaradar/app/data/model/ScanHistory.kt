package com.novaradar.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ip: String,
    val port: Int,
    val ping: Long,
    val novaId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
