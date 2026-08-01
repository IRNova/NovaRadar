package com.novaradar.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ip_source")
data class IpSource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameEn: String,
    val nameFa: String,
    val cidr: String,
    val url: String = "",
    val type: String = "cidr", // cidr, proxyip, domain
    val isEnabled: Boolean
)
