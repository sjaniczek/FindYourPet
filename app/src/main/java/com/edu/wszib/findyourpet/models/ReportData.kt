package com.edu.wszib.findyourpet.models

data class ReportData(
    val reportId: String = "",
    val reportedByUid: String = "",
    val reportedUserUid: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "pending" // pending / reviewed / resolved
)