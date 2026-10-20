package com.prathamshah.hostelfill.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathamshah.hostelfill.data.FieldKey
import com.prathamshah.hostelfill.data.normalizeLabel
import com.prathamshah.hostelfill.viewmodel.AutofillStatus
import com.prathamshah.hostelfill.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onEditProfile: () -> Unit,
    onOpenForm: (url: String, autoSubmit: Boolean) -> Unit
) {
    val profile        by viewModel.profile.collectAsState()
    val formUrl        by viewModel.formUrl.collectAsState()
    val urlError       by viewModel.urlError.collectAsState()
    val validatedUrl   by viewModel.validatedUrl.collectAsState()
    val status         by viewModel.autofillStatus.collectAsState()
    val autoSubmit     by viewModel.autoSubmitEnabled.collectAsState()
    val customMappings by viewModel.customMappings.collectAsState()

    val clipboard = LocalClipboardManager.current

    // Per-submission meal overrides
    var breakfastOverride by remember { mutableStateOf(profile.breakfastChoice) }
    var lunchOverride     by remember { mutableStateOf(profile.lunchChoice) }
    var dinnerOverride    by remember { mutableStateOf(profile.dinnerChoice) }
    LaunchedEffect(profile) {
        breakfastOverride = profile.breakfastChoice
        lunchOverride     = profile.lunchChoice
        dinnerOverride    = profile.dinnerChoice
    }

    // Unknown-question mapping dialog
    var unknownLabelToMap by remember { mutableStateOf<String?>(null) }
    var mappingSelection  by remember { mutableStateOf(FieldKey.SKIP) }

    val scrollState = rememberScrollState()

    if (unknownLabelToMap != null) {
        UnknownFieldDialog(
            label          = unknownLabelToMap!!,
            profile        = profile,
            onSave         = { fieldKey ->
                viewModel.saveMappingForUnknown(normalizeLabel(unknownLabelToMap!!), fieldKey)
                unknownLabelToMap = null
            },
            onDismiss      = { unknownLabelToMap = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HostelFill", fontWeight = FontWeight.Bold)
                        Text("By Pratham Shah", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Profile summary card ──────────────────────────────────────────
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Profile", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(profile.fullName, fontWeight = FontWeight.Bold)
                    Text("Room ${profile.roomNumber}  ·  ${profile.email}")
                    Text(profile.phoneNumber)
                }
            }

            Divider()

            // ── URL input ─────────────────────────────────────────────────────
            Text("Hostel Form Link", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = formUrl,
                onValueChange = { viewModel.setFormUrl(it) },
                label = { Text("Paste Google Forms URL") },
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (formUrl.isNotBlank()) {
                        IconButton(onClick = { viewModel.setFormUrl("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val clip = clipboard.getText()?.text ?: ""
                        if (clip.isNotBlank()) viewModel.setFormUrl(clip)
                    }
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Paste Link")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick  = { viewModel.validateAndOpenForm() },
                    enabled  = formUrl.isNotBlank()
                ) {
                    Text("Validate")
                }
            }

            // ── Meal override for this submission ─────────────────────────────
            Divider()
            Text("Meal Choices for This Submission", fontWeight = FontWeight.SemiBold)

            MealRow("Breakfast", breakfastOverride, profile.mealOption0Label, profile.mealOption1Label) {
                breakfastOverride = it
                viewModel.setMealOverride(it, lunchOverride, dinnerOverride)
            }
            MealRow("Lunch", lunchOverride, profile.mealOption0Label, profile.mealOption1Label) {
                lunchOverride = it
                viewModel.setMealOverride(breakfastOverride, it, dinnerOverride)
            }
            MealRow("Dinner", dinnerOverride, profile.mealOption0Label, profile.mealOption1Label) {
                dinnerOverride = it
                viewModel.setMealOverride(breakfastOverride, lunchOverride, it)
            }

            // ── Auto-submit toggle ────────────────────────────────────────────
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Submit", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Submits the form automatically after filling all fields. " +
                        "Only use for the recognized hostel form.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoSubmit, onCheckedChange = { viewModel.toggleAutoSubmit(it) })
            }

            // ── Start Autofill button ─────────────────────────────────────────
            validatedUrl?.let { url ->
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = {
                        viewModel.setMealOverride(breakfastOverride, lunchOverride, dinnerOverride)
                        onOpenForm(url, autoSubmit)
                    }
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Autofill", fontSize = 16.sp)
                }
            }

            // ── Status display ────────────────────────────────────────────────
            when (val s = status) {
                AutofillStatus.Idle    -> {}
                AutofillStatus.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                is AutofillStatus.WaitingForLogin -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Text(s.message, fontSize = 13.sp)
                        }
                    }
                }

                is AutofillStatus.Running -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(s.message, fontSize = 13.sp)
                }

                is AutofillStatus.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }

                is AutofillStatus.Complete -> {
                    ResultCard(
                        status = s,
                        onMapUnknown = { label -> unknownLabelToMap = label }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MealRow(
    label: String,
    value: String,
    label0: String,
    label1: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.width(80.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = value == "0",
                onClick  = { onSelect("0") },
                label    = { Text("0 — ${label0.ifBlank { "Option 0" }}", fontSize = 12.sp) }
            )
            FilterChip(
                selected = value == "1",
                onClick  = { onSelect("1") },
                label    = { Text("1 — ${label1.ifBlank { "Option 1" }}", fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun ResultCard(
    status: AutofillStatus.Complete,
    onMapUnknown: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (status.errors.isEmpty() && status.unknown.isEmpty())
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (status.submitted) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null
                )
                Text(
                    if (status.submitted) "Form Submitted ✓" else "Autofill Complete",
                    fontWeight = FontWeight.Bold
                )
            }

            if (status.filled.isNotEmpty()) {
                Text("✓ Filled (${status.filled.size}): ${status.filled.joinToString(", ")}", fontSize = 13.sp)
            }
            if (status.skipped.isNotEmpty()) {
                Text("⚡ Skipped: ${status.skipped.joinToString(", ")}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (status.unknown.isNotEmpty()) {
                Text("⚠ Unknown questions:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                status.unknown.forEach { label ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• $label", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onMapUnknown(label) }) { Text("Map", fontSize = 12.sp) }
                    }
                }
            }
            if (status.errors.isNotEmpty()) {
                Text("✗ Errors:", fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                status.errors.forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnknownFieldDialog(
    label: String,
    profile: com.prathamshah.hostelfill.data.UserProfile,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(FieldKey.SKIP) }

    val options = listOf(
        FieldKey.FULL_NAME  to "Full Name (${profile.fullName})",
        FieldKey.EMAIL      to "Email (${profile.email})",
        FieldKey.PHONE      to "Phone (${profile.phoneNumber})",
        FieldKey.ROOM       to "Room Number (${profile.roomNumber})",
        FieldKey.BREAKFAST  to "Breakfast choice",
        FieldKey.LUNCH      to "Lunch choice",
        FieldKey.DINNER     to "Dinner choice",
        FieldKey.SKIP       to "Skip this field"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unknown Question") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("\"$label\"", fontWeight = FontWeight.Bold)
                Text("Map this question to a profile field:", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                options.forEach { (key, display) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = selected == key, onClick = { selected = key })
                        Text(display, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }) { Text("Save Mapping") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
