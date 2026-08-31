package com.yuanbao.todo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yuanbao.todo.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.taskDataStore: DataStore<Preferences> by preferencesDataStore(name = "todo_store")

class TaskRepository(private val context: Context) {

    private object Keys {
        val TASKS = stringPreferencesKey("tasks_json")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val dataStore: DataStore<Preferences> get() = context.taskDataStore

    val tasks: Flow<List<Task>> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> decode(prefs[Keys.TASKS]) }

    /** 读取当前全量列表（用于做增量修改） */
    suspend fun snapshot(): List<Task> = tasks.first()

    suspend fun replaceAll(list: List<Task>) {
        dataStore.edit { prefs ->
            prefs[Keys.TASKS] = json.encodeToString(list)
        }
    }

    private fun decode(raw: String?): List<Task> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Task>>(raw) }.getOrDefault(emptyList())
    }
}
