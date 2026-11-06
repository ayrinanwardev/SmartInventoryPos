package com.example.smartinventorypos.data.local.database

import androidx.room3.ColumnTypeConverter
import com.example.smartinventorypos.domain.model.SyncStatus

/** These numbers are stored on devices, so an existing status must never change its number. */
class SyncStatusConverter {
    @ColumnTypeConverter
    fun toCode(status: SyncStatus): Int = when (status) {
        SyncStatus.PENDING -> 0
        SyncStatus.SYNCED -> 1
        SyncStatus.REJECTED -> 2
        SyncStatus.REJECTED_ACKNOWLEDGED -> 3
    }

    @ColumnTypeConverter
    fun fromCode(code: Int): SyncStatus = when (code) {
        0 -> SyncStatus.PENDING
        1 -> SyncStatus.SYNCED
        2 -> SyncStatus.REJECTED
        3 -> SyncStatus.REJECTED_ACKNOWLEDGED
        else -> error("Unknown sale status $code")
    }
}
