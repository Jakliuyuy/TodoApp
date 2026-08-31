package com.yuanbao.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuanbao.todo.R
import com.yuanbao.todo.model.Filter
import com.yuanbao.todo.model.Task
import com.yuanbao.todo.viewmodel.TodoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(viewModel: TodoViewModel = viewModel()) {
    val tasks by viewModel.visibleTasks.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办清单") },
                actions = {
                    IconButton(onClick = { viewModel.clearDone() }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.clear_done)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StatsHeader(done = stats.first, total = stats.second)
            InputRow(
                text = input,
                onTextChange = { input = it },
                onAdd = {
                    viewModel.add(input)
                    input = ""
                }
            )
            FilterRow(current = filter, onSelect = viewModel::setFilter)
            Divider()

            if (tasks.isEmpty()) {
                EmptyHint(filter = filter)
            } else {
                TaskList(
                    tasks = tasks,
                    viewModel = viewModel,
                    onEdit = { editing = it }
                )
            }
        }
    }

    editing?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editing = null },
            onSave = { title ->
                viewModel.rename(task.id, title)
                editing = null
            }
        )
    }
}

@Composable
private fun StatsHeader(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_done, done, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InputRow(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.input_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() })
        )
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.add)
            )
        }
    }
}

@Composable
private fun FilterRow(current: Filter, onSelect: (Filter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = current == Filter.ALL,
            onClick = { onSelect(Filter.ALL) },
            label = { Text(stringResource(R.string.filter_all)) }
        )
        FilterChip(
            selected = current == Filter.ACTIVE,
            onClick = { onSelect(Filter.ACTIVE) },
            label = { Text(stringResource(R.string.filter_active)) }
        )
        FilterChip(
            selected = current == Filter.DONE,
            onClick = { onSelect(Filter.DONE) },
            label = { Text(stringResource(R.string.filter_done)) }
        )
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    viewModel: TodoViewModel,
    onEdit: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = tasks, key = { it.id }) { task ->
            TaskItem(
                task = task,
                onToggle = { viewModel.toggle(task.id) },
                onStar = { viewModel.toggleImportant(task.id) },
                onDelete = { viewModel.remove(task.id) },
                onEdit = { onEdit(task) }
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onEdit,
        colors = CardDefaults.cardColors(
            containerColor = if (task.important && !task.done) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                color = if (task.done) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            IconButton(onClick = onStar) {
                Icon(
                    imageVector = if (task.important) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "标为重要",
                    tint = if (task.important) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(filter: Filter) {
    val text = when (filter) {
        Filter.ALL -> stringResource(R.string.empty_all)
        Filter.ACTIVE -> stringResource(R.string.empty_active)
        Filter.DONE -> stringResource(R.string.empty_done)
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(task.id) { mutableStateOf(task.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑任务") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
