package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LuggageItemDto(
    @Json(name = "_id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "category") val category: String = "Other",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "isPacked") val isPacked: Boolean = false,
    @Json(name = "isEssential") val isEssential: Boolean = false,
    @Json(name = "weightKg") val weightKg: Double = 0.0,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class LuggageTripDto(
    @Json(name = "_id") val id: String = "",
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "destination") val destination: String = "",
    @Json(name = "bagType") val bagType: String = "Cabin Bag",
    @Json(name = "departureDate") val departureDate: String? = null,
    @Json(name = "returnDate") val returnDate: String? = null,
    @Json(name = "maxWeightKg") val maxWeightKg: Double = 7.0,
    @Json(name = "colorHex") val colorHex: String = "#3B82F6",
    @Json(name = "items") val items: List<LuggageItemDto> = emptyList(),
    @Json(name = "totalItems") val totalItems: Int = 0,
    @Json(name = "packedItems") val packedItems: Int = 0,
    @Json(name = "totalWeightKg") val totalWeightKg: Double = 0.0,
    @Json(name = "packedWeightKg") val packedWeightKg: Double = 0.0,
    @Json(name = "isWeightExceeded") val isWeightExceeded: Boolean = false,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
) {
    val packingProgress: Float
        get() = if (totalItems > 0) packedItems.toFloat() / totalItems.toFloat() else 0f

    val weightPercentage: Float
        get() = if (maxWeightKg > 0) (totalWeightKg.toFloat() / maxWeightKg.toFloat()).coerceAtMost(1f) else 0f
}

@JsonClass(generateAdapter = true)
data class LuggageListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "data") val data: List<LuggageTripDto> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LuggageSingleResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: LuggageTripDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LuggageMutationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateLuggageTripRequest(
    @Json(name = "title") val title: String,
    @Json(name = "destination") val destination: String = "",
    @Json(name = "bagType") val bagType: String = "Cabin Bag",
    @Json(name = "departureDate") val departureDate: String = "",
    @Json(name = "returnDate") val returnDate: String = "",
    @Json(name = "maxWeightKg") val maxWeightKg: Double = 7.0,
    @Json(name = "colorHex") val colorHex: String = "#3B82F6"
)

@JsonClass(generateAdapter = true)
data class UpdateLuggageTripRequest(
    @Json(name = "title") val title: String? = null,
    @Json(name = "destination") val destination: String? = null,
    @Json(name = "bagType") val bagType: String? = null,
    @Json(name = "departureDate") val departureDate: String? = null,
    @Json(name = "returnDate") val returnDate: String? = null,
    @Json(name = "maxWeightKg") val maxWeightKg: Double? = null,
    @Json(name = "colorHex") val colorHex: String? = null
)

@JsonClass(generateAdapter = true)
data class AddLuggageItemRequest(
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String = "Other",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "isPacked") val isPacked: Boolean = false,
    @Json(name = "isEssential") val isEssential: Boolean = false,
    @Json(name = "weightKg") val weightKg: Double = 0.0,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateLuggageItemRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "quantity") val quantity: Int? = null,
    @Json(name = "isPacked") val isPacked: Boolean? = null,
    @Json(name = "isEssential") val isEssential: Boolean? = null,
    @Json(name = "weightKg") val weightKg: Double? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ApplyLuggageTemplateRequest(
    @Json(name = "templateKey") val templateKey: String
)

enum class LuggageCategory(val displayName: String, val iconName: String) {
    CLOTHING("Clothing", "Checkroom"),
    ELECTRONICS("Electronics", "Devices"),
    TOILETRIES("Toiletries", "CleanHands"),
    DOCUMENTS("Documents", "Description"),
    MEDICATION("Medication", "Medication"),
    VALUABLES("Valuables", "Diamond"),
    OTHER("Other", "Category")
}
