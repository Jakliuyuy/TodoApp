package com.yuanbao.todo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuanbao.todo.data.TaskRepository
import com.yuanbao.todo.model.Filter
import com.yuanbao.todo.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TaskRepository(app)

    val tasks: StateFlow<List<Task>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(Filter.ALL)
    val filter: StateFlow<Filter> = _filter.asStateFlow()

    /** 当前筛选下、已排序的列表 */
    val visibleTasks: StateFlow<List<Task>> =
        combine(tasks, _filter) { list, f ->
            val filtered = when (f) {
                Filter.ALL -> list
                Filter.ACTIVE -> list.filter { !it.done }
                Filter.DONE -> list.filter { it.done }
            }
            filtered.sortedWith(
                compareBy<Task> { it.done }
                    .thenByDescending { it.important }
                    .thenByDescending { it.createdAt }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** (已完成数, 总数) */
    val stats: StateFlow<Pair<Int, Int>> = tasks
        .map { list -> list.count { it.done } to list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    fun add(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        mutate { list -> list + Task(title = trimmed) }
    }

    fun toggle(id: String) {
        mutate { list ->
            list.map { if (it.id == id) it.copy(done = !it.done) else it }
        }
    }

    fun toggleImportant(id: String) {
        mutate { list ->
            list.map { if (it.id == id) it.copy(important = !it.important) else it }
        }
    }

    fun rename(id: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        mutate { list ->
            list.map { if (it.id == id) it.copy(title = trimmed) else it }
        }
    }

    fun remove(id: String) {
        mutate { list -> list.filter { it.id != id } }
    }

    fun clearDone() {
        mutate { list -> list.filter { !it.done } }
    }

    fun setFilter(filter: Filter) {
        _filter.value = filter
    }

    private fun mutate(transform: (List<Task>) -> List<Task>) {
        viewModelScope.launch {
            repository.replaceAll(transform(tasks.value))
        }
    }
}
