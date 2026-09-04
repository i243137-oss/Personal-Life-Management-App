package com.example.data.api

import com.example.data.model.AddLuggageItemRequest
import com.example.data.model.ApplyLuggageTemplateRequest
import com.example.data.model.CreateLuggageTripRequest
import com.example.data.model.LuggageListResponse
import com.example.data.model.LuggageMutationResponse
import com.example.data.model.LuggageSingleResponse
import com.example.data.model.UpdateLuggageItemRequest
import com.example.data.model.UpdateLuggageTripRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LuggageApiService {

    @GET("api/luggage")
    suspend fun getTrips(): Response<LuggageListResponse>

    @GET("api/luggage/{id}")
    suspend fun getTripById(@Path("id") id: String): Response<LuggageSingleResponse>

    @POST("api/luggage")
    suspend fun createTrip(@Body request: CreateLuggageTripRequest): Response<LuggageSingleResponse>

    @PUT("api/luggage/{id}")
    suspend fun updateTrip(
        @Path("id") id: String,
        @Body request: UpdateLuggageTripRequest
    ): Response<LuggageSingleResponse>

    @DELETE("api/luggage/{id}")
    suspend fun deleteTrip(@Path("id") id: String): Response<LuggageMutationResponse>

    @POST("api/luggage/{id}/items")
    suspend fun addItem(
        @Path("id") tripId: String,
        @Body request: AddLuggageItemRequest
    ): Response<LuggageSingleResponse>

    @PATCH("api/luggage/{id}/items/{itemId}/toggle")
    suspend fun toggleItem(
        @Path("id") tripId: String,
        @Path("itemId") itemId: String
    ): Response<LuggageSingleResponse>

    @PUT("api/luggage/{id}/items/{itemId}")
    suspend fun updateItem(
        @Path("id") tripId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateLuggageItemRequest
    ): Response<LuggageSingleResponse>

    @DELETE("api/luggage/{id}/items/{itemId}")
    suspend fun deleteItem(
        @Path("id") tripId: String,
        @Path("itemId") itemId: String
    ): Response<LuggageSingleResponse>

    @POST("api/luggage/{id}/template")
    suspend fun applyTemplate(
        @Path("id") tripId: String,
        @Body request: ApplyLuggageTemplateRequest
    ): Response<LuggageSingleResponse>
}
