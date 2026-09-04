package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.local.LocalDataManager
import com.example.data.model.AddLuggageItemRequest
import com.example.data.model.ApplyLuggageTemplateRequest
import com.example.data.model.CreateLuggageTripRequest
import com.example.data.model.LuggageTripDto
import com.example.data.model.UpdateLuggageItemRequest
import com.example.data.model.UpdateLuggageTripRequest

class LuggageRepository(
    private val apiClient: ApiClient,
    private val localDataManager: LocalDataManager
) {

    suspend fun getTrips(): Result<List<LuggageTripDto>> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.getTrips()
            if (response.isSuccessful && response.body() != null) {
                val trips = response.body()!!.data
                localDataManager.setLuggageTrips(trips)
                Result.success(trips)
            } else {
                Result.success(localDataManager.getLuggageTrips())
            }
        } catch (e: Exception) {
            Result.success(localDataManager.getLuggageTrips())
        }
    }

    suspend fun getTripById(id: String): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.getTripById(id)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val local = localDataManager.getLuggageTripById(id)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Luggage trip not found"))
            }
        } catch (e: Exception) {
            val local = localDataManager.getLuggageTripById(id)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun createTrip(request: CreateLuggageTripRequest): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.createTrip(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val trip = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                current.add(0, trip)
                localDataManager.setLuggageTrips(current)
                Result.success(trip)
            } else {
                Result.success(localDataManager.createLuggageTrip(request))
            }
        } catch (e: Exception) {
            Result.success(localDataManager.createLuggageTrip(request))
        }
    }

    suspend fun updateTrip(id: String, request: UpdateLuggageTripRequest): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.updateTrip(id, request)
            if (response.isSuccessful && response.body()?.data != null) {
                val trip = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                val idx = current.indexOfFirst { it.id == id }
                if (idx != -1) current[idx] = trip
                localDataManager.setLuggageTrips(current)
                Result.success(trip)
            } else {
                val local = localDataManager.updateLuggageTrip(id, request)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Failed to update luggage trip"))
            }
        } catch (e: Exception) {
            val local = localDataManager.updateLuggageTrip(id, request)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun deleteTrip(id: String): Result<Boolean> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.deleteTrip(id)
            localDataManager.deleteLuggageTrip(id)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            val removed = localDataManager.deleteLuggageTrip(id)
            Result.success(removed)
        }
    }

    suspend fun addItem(tripId: String, request: AddLuggageItemRequest): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.addItem(tripId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                val idx = current.indexOfFirst { it.id == tripId }
                if (idx != -1) current[idx] = updated
                localDataManager.setLuggageTrips(current)
                Result.success(updated)
            } else {
                val local = localDataManager.addLuggageItem(tripId, request)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Failed to add luggage item"))
            }
        } catch (e: Exception) {
            val local = localDataManager.addLuggageItem(tripId, request)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun toggleItem(tripId: String, itemId: String): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.toggleItem(tripId, itemId)
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                val idx = current.indexOfFirst { it.id == tripId }
                if (idx != -1) current[idx] = updated
                localDataManager.setLuggageTrips(current)
                Result.success(updated)
            } else {
                val local = localDataManager.toggleLuggageItem(tripId, itemId)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Failed to toggle item"))
            }
        } catch (e: Exception) {
            val local = localDataManager.toggleLuggageItem(tripId, itemId)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun updateItem(
        tripId: String,
        itemId: String,
        request: UpdateLuggageItemRequest
    ): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.updateItem(tripId, itemId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                val idx = current.indexOfFirst { it.id == tripId }
                if (idx != -1) current[idx] = updated
                localDataManager.setLuggageTrips(current)
                Result.success(updated)
            } else {
                val local = localDataManager.updateLuggageItem(tripId, itemId, request)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Failed to update item"))
            }
        } catch (e: Exception) {
            val local = localDataManager.updateLuggageItem(tripId, itemId, request)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun deleteItem(tripId: String, itemId: String): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.deleteItem(tripId, itemId)
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                val idx = current.indexOfFirst { it.id == tripId }
                if (idx != -1) current[idx] = updated
                localDataManager.setLuggageTrips(current)
                Result.success(updated)
            } else {
                val local = localDataManager.deleteLuggageItem(tripId, itemId)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Failed to delete item"))
            }
        } catch (e: Exception) {
            val local = localDataManager.deleteLuggageItem(tripId, itemId)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun applyTemplate(tripId: String, templateKey: String): Result<LuggageTripDto> {
        return try {
            val api = apiClient.getLuggageApiService()
            val response = api.applyTemplate(tripId, ApplyLuggageTemplateRequest(templateKey))
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data!!
                val current = localDataManager.getLuggageTrips().toMutableList()
                val idx = current.indexOfFirst { it.id == tripId }
                if (idx != -1) current[idx] = updated
                localDataManager.setLuggageTrips(current)
                Result.success(updated)
            } else {
                val local = localDataManager.applyLuggageTemplate(tripId, templateKey)
                if (local != null) Result.success(local)
                else Result.failure(Exception("Failed to apply template"))
            }
        } catch (e: Exception) {
            val local = localDataManager.applyLuggageTemplate(tripId, templateKey)
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }
}
