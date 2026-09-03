package com.example.data

import kotlinx.coroutines.flow.Flow

class BijliRepository(private val dao: BijliDao) {

    val allProperties: Flow<List<PropertyEntity>> = dao.getAllProperties()
    val allSavedBills: Flow<List<SavedBillEntity>> = dao.getAllSavedBills()

    fun getUnitsForProperty(propertyId: String): Flow<List<PropertyUnitEntity>> =
        dao.getUnitsForProperty(propertyId)

    suspend fun getUnitsForPropertySync(propertyId: String): List<PropertyUnitEntity> =
        dao.getUnitsForPropertySync(propertyId)

    suspend fun insertProperty(property: PropertyEntity) =
        dao.insertProperty(property)

    suspend fun deleteProperty(property: PropertyEntity) =
        dao.deleteProperty(property)

    suspend fun insertUnits(units: List<PropertyUnitEntity>) =
        dao.insertUnits(units)

    suspend fun insertUnit(unit: PropertyUnitEntity) =
        dao.insertUnit(unit)

    suspend fun updateUnit(unit: PropertyUnitEntity) =
        dao.updateUnit(unit)

    suspend fun deleteUnit(unit: PropertyUnitEntity) =
        dao.deleteUnit(unit)

    suspend fun saveBillWithShares(bill: SavedBillEntity, shares: List<SavedPortionShareEntity>) {
        dao.insertBill(bill)
        dao.insertShares(shares)
    }

    fun getSharesForBill(billId: String): Flow<List<SavedPortionShareEntity>> =
        dao.getSharesForBill(billId)

    suspend fun getSharesForBillSync(billId: String): List<SavedPortionShareEntity> =
        dao.getSharesForBillSync(billId)

    suspend fun deleteBill(bill: SavedBillEntity) =
        dao.deleteBill(bill)

    fun getPaymentsForBill(billId: String): Flow<List<PaymentEntryEntity>> =
        dao.getPaymentsForBill(billId)

    suspend fun recordPayment(payment: PaymentEntryEntity, share: SavedPortionShareEntity) =
        dao.recordPaymentAndUpdateShare(payment, share)
}
