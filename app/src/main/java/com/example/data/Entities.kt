package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val propertyType: String = "HOUSE", // HOUSE, PLAZA, RENTAL_HOUSE, HOSTEL, CUSTOM
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "property_units")
data class PropertyUnitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val name: String, // e.g. "Ground Floor", "Shop 1", "Portion A"
    val tenantName: String = "",
    val tenantPhone: String = "",
    val defaultRate: Double = 0.0,
    val lastReading: Double = 0.0,
    val sortOrder: Int = 0
)

@Entity(tableName = "saved_bills")
data class SavedBillEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val propertyId: String? = null,
    val propertyName: String = "",
    val title: String,
    val billMonthYear: String = "",
    val sourceBillRupees: Long,
    val mainMeterPrev: Double,
    val mainMeterCurr: Double,
    val mainMeterUnits: Double,
    val subMetersTotalUnits: Double,
    val commonUnits: Double,
    val allocationMethod: String,
    val commonUnitsHandling: String,
    val totalAllocatedRupees: Long,
    val remainingRupees: Long,
    val status: String = "FINALIZED", // DRAFT, FINALIZED, PARTIALLY_PAID, PAID
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_portion_shares")
data class SavedPortionShareEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billId: String,
    val portionName: String,
    val tenantName: String = "",
    val tenantPhone: String = "",
    val previousReading: Double,
    val currentReading: Double,
    val unitsUsed: Double,
    val commonUnitsShare: Double = 0.0,
    val totalBillableUnits: Double,
    val allocatedAmountRupees: Long,
    val paidAmountRupees: Long = 0L,
    val paymentStatus: String = "UNPAID", // UNPAID, PARTIAL, PAID, ADVANCE
    val notes: String = ""
)

@Entity(tableName = "payment_entries")
data class PaymentEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billId: String,
    val shareId: String,
    val tenantName: String,
    val amountRupees: Long,
    val paymentDate: Long = System.currentTimeMillis(),
    val method: String = "CASH", // CASH, BANK_TRANSFER, EASYPAISA, JAZZCASH, NAYAPAY, SADAPAY, CHEQUE, OTHER
    val notes: String = ""
)
