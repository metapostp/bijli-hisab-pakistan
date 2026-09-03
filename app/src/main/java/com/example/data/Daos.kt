package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BijliDao {
    // Properties
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    fun getAllProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: String): PropertyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertyEntity)

    @Delete
    suspend fun deleteProperty(property: PropertyEntity)

    // Property Units
    @Query("SELECT * FROM property_units WHERE propertyId = :propertyId ORDER BY sortOrder ASC, name ASC")
    fun getUnitsForProperty(propertyId: String): Flow<List<PropertyUnitEntity>>

    @Query("SELECT * FROM property_units WHERE propertyId = :propertyId ORDER BY sortOrder ASC, name ASC")
    suspend fun getUnitsForPropertySync(propertyId: String): List<PropertyUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<PropertyUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: PropertyUnitEntity)

    @Update
    suspend fun updateUnit(unit: PropertyUnitEntity)

    @Delete
    suspend fun deleteUnit(unit: PropertyUnitEntity)

    // Saved Bills
    @Query("SELECT * FROM saved_bills ORDER BY createdAt DESC")
    fun getAllSavedBills(): Flow<List<SavedBillEntity>>

    @Query("SELECT * FROM saved_bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): SavedBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: SavedBillEntity)

    @Delete
    suspend fun deleteBill(bill: SavedBillEntity)

    // Saved Portion Shares
    @Query("SELECT * FROM saved_portion_shares WHERE billId = :billId")
    fun getSharesForBill(billId: String): Flow<List<SavedPortionShareEntity>>

    @Query("SELECT * FROM saved_portion_shares WHERE billId = :billId")
    suspend fun getSharesForBillSync(billId: String): List<SavedPortionShareEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<SavedPortionShareEntity>)

    @Update
    suspend fun updateShare(share: SavedPortionShareEntity)

    // Payments
    @Query("SELECT * FROM payment_entries WHERE billId = :billId ORDER BY paymentDate DESC")
    fun getPaymentsForBill(billId: String): Flow<List<PaymentEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntryEntity)

    @Transaction
    suspend fun recordPaymentAndUpdateShare(payment: PaymentEntryEntity, share: SavedPortionShareEntity) {
        insertPayment(payment)
        val newPaid = share.paidAmountRupees + payment.amountRupees
        val newStatus = when {
            newPaid >= share.allocatedAmountRupees && newPaid > share.allocatedAmountRupees -> "ADVANCE"
            newPaid >= share.allocatedAmountRupees -> "PAID"
            newPaid > 0 -> "PARTIAL"
            else -> "UNPAID"
        }
        updateShare(share.copy(paidAmountRupees = newPaid, paymentStatus = newStatus))
    }
}
