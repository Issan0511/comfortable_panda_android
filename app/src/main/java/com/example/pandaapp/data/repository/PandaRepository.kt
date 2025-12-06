package com.example.pandaapp.data.repository

import com.example.pandaapp.data.api.PandaApiClient
import com.example.pandaapp.data.model.Assignment

class PandaRepository(
    private val apiClient: PandaApiClient
) {
    suspend fun loginAndFetchAssignments(username: String, password: String): List<Assignment> {
        return apiClient.fetchAssignments(username, password)
    }
}
