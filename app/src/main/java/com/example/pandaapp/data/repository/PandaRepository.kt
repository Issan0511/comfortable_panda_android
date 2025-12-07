package com.example.pandaapp.data.repository

import android.content.Context
import com.example.pandaapp.data.api.PandaApiClient
import com.example.pandaapp.data.model.Assignment

class PandaRepository(context: Context) {
    private val client = PandaApiClient()

    suspend fun fetchAssignments(username: String, password: String): List<Assignment> {
        return client.fetchAssignments(username, password)
    }
}
