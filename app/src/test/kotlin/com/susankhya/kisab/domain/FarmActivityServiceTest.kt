package com.susankhya.kisab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FarmActivityServiceTest {
    private lateinit var service: FarmSliceService

    @Before
    fun setUp() {
        service = FarmSliceService(InMemoryFarmStore())
    }

    @Test
    fun createFarmAutoProvisionsPredefinedProductsAndSupplies() {
        val farm = service.createFarm(
            "Poultry Farm",
            activities = listOf(FarmActivityType.POULTRY)
        )
        // POULTRY predefined: 4 products (Egg, Chicken (Meat), Live Chicken, Manure)
        // POULTRY predefined: 5 supplies (Chicks, Feed, Medicine, Vaccines, Husk)
        val productNames = service.products(farm.id).map { it.name }
        val supplyNames = service.supplies(farm.id).map { it.name }
        assertEquals(4, productNames.size)
        assertTrue(productNames.contains("Egg"))
        assertTrue(productNames.contains("Manure"))
        assertEquals(5, supplyNames.size)
        assertTrue(supplyNames.contains("Feed"))
        assertTrue(supplyNames.contains("Chicks"))
    }

    @Test
    fun setFarmActivitiesAutoProvisionsNewActivityItems() {
        val farm = service.createFarm("Empty Farm")
        assertEquals(0, service.products(farm.id).size)
        assertEquals(0, service.supplies(farm.id).size)

        service.setFarmActivities(farm.id, setOf(FarmActivityType.CROPS))
        val productNames = service.products(farm.id).map { it.name }
        val supplyNames = service.supplies(farm.id).map { it.name }
        // CROPS predefined: 9 products, 6 supplies
        assertEquals(9, productNames.size)
        assertTrue(productNames.contains("Paddy"))
        assertEquals(6, supplyNames.size)
        assertTrue(supplyNames.contains("Urea"))
    }

    @Test
    fun ensurePredefinedItemsIsIdempotent() {
        val farm = service.createFarm(
            "Dairy Farm",
            activities = listOf(FarmActivityType.CATTLE_BUFFALO_DAIRY)
        )
        val countBefore = service.products(farm.id).size
        // Calling again should add nothing
        val added = service.ensurePredefinedItems(farm.id)
        assertEquals(false, added)
        assertEquals(countBefore, service.products(farm.id).size)
    }

    @Test
    fun ensurePredefinedItemsRetroactivelyProvisionsExistingFarm() {
        // Simulate a legacy farm: activities but no predefined products/supplies
        val store = InMemoryFarmStore()
        val service = FarmSliceService(store)
        val farm = FarmState(
            id = "farm-legacy",
            name = "Legacy Farm",
            currencyCode = "NPR",
            activities = mutableListOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)
        )
        store.saveFarm(farm)
        store.setCurrentFarmId(farm.id)

        val added = service.ensurePredefinedItems(farm.id)
        assertTrue(added)
        val productNames = service.products(farm.id).map { it.name }
        val supplyNames = service.supplies(farm.id).map { it.name }
        assertTrue(productNames.contains("Egg"))
        assertTrue(productNames.contains("Paddy"))
        assertTrue(supplyNames.contains("Feed"))
        assertTrue(supplyNames.contains("Urea"))
    }

    @Test
    fun createFarmOrdersActivitiesByDisplayOrder() {
        val farm = service.createFarm(
            "Mixed",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)
        )
        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.POULTRY), farm.activities)
    }

    @Test
    fun createFarmDeduplicatesAndOrdersActivities() {
        val farm = service.createFarm(
            "Bad",
            activities = listOf(FarmActivityType.CROPS, FarmActivityType.CROPS, FarmActivityType.POULTRY)
        )
        assertEquals(listOf(FarmActivityType.CROPS, FarmActivityType.POULTRY), farm.activities)
    }

    @Test
    fun setFarmActivitiesDisablesOnlyActivitiesWithHistory() {
        val farm = service.createFarm(
            "Mixed",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 900,
                description = "Goat feed",
                occurredAt = "2024-02-03T12:00:00Z",
                activity = FarmActivityType.GOAT_SHEEP
            )
        )

        service.setFarmActivities(farm.id, setOf(FarmActivityType.POULTRY))

        val updated = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.POULTRY), updated.activities)
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), updated.disabledActivities)
        assertEquals(1, updated.transactions.size)
        assertEquals(FarmActivityType.GOAT_SHEEP, updated.transactions[0].activity)
    }

    @Test
    fun setFarmActivitiesDropsActivitiesWithoutHistory() {
        val farm = service.createFarm(
            "Mixed",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.setFarmActivities(farm.id, setOf(FarmActivityType.GOAT_SHEEP))

        val updated = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), updated.activities)
        assertTrue(updated.disabledActivities.isEmpty())
    }

    @Test
    fun disableFarmActivityThenReEnableKeepsHistoryAndTotals() {
        val farm = service.createFarm(
            "Poultry Farm",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 900,
                description = "Goat feed",
                occurredAt = "2024-02-03T12:00:00Z",
                activity = FarmActivityType.GOAT_SHEEP
            )
        )

        service.disableFarmActivity(farm.id, FarmActivityType.GOAT_SHEEP)
        val disabled = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), disabled.disabledActivities)
        assertEquals(900L, service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.GOAT_SHEEP }.expenseMinor)

        service.reEnableFarmActivity(farm.id, FarmActivityType.GOAT_SHEEP)
        val reEnabled = service.loadFarm(farm.id)!!
        assertEquals(
            listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP),
            reEnabled.activities
        )
        assertTrue(reEnabled.disabledActivities.isEmpty())
        assertEquals(1, reEnabled.transactions.size)
        assertEquals(900L, service.farmActivityBreakdown(farm.id).first { it.activity == FarmActivityType.GOAT_SHEEP }.expenseMinor)
    }

    @Test
    fun farmActivityBreakdownIsStableAcrossDisableReEnable() {
        val farm = service.createFarm(
            "Dairy",
            activities = listOf(FarmActivityType.CATTLE_BUFFALO_DAIRY)
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 10000,
                description = "Milk sale",
                occurredAt = "2024-05-01T12:00:00Z",
                activity = FarmActivityType.CATTLE_BUFFALO_DAIRY
            )
        )

        val before = service.farmActivityBreakdown(farm.id)
        service.disableFarmActivity(farm.id, FarmActivityType.CATTLE_BUFFALO_DAIRY)
        val after = service.farmActivityBreakdown(farm.id)

        assertEquals(before, after)
    }

    @Test
    fun addFarmActivityAddsToRunningSet() {
        val farm = service.createFarm("Farm")
        service.addFarmActivity(farm.id, FarmActivityType.FISHERY)
        assertEquals(listOf(FarmActivityType.FISHERY), service.loadFarm(farm.id)!!.activities)
    }
}