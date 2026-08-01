package com.novaradar.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "port_config")
data class PortConfig(
    @PrimaryKey val port: Int,
    val isEnabled: Boolean = true
)
