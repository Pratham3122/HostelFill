package com.prathamshah.hostelfill.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathamshah.hostelfill.data.*
import com.prathamshah.hostelfill.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: MainViewModel,
    isEditing: Boolean = false,
    onComplete: () -> Unit
) {
    val existing by viewModel.profile.collectAsState()

    var fullName    by remember(existing) { mutableStateOf(existing.fullName) }
    var email       by remember(existing) { mutableStateOf(existing.email) }
    var phone       by remember(existing) { mutableStateOf(existing.phoneNumber) }
    var room        by remember(existing) { mutableStateOf(existing.roomNumber) }
    var breakfast   by remember(existing) { mutableStateOf(existing.breakfastChoice) }
    var lunch       by remember(existing) { mutableStateOf(existing.lunchChoice) }
    var dinner      by remember(existing) { mutableStateOf(existing.dinnerChoice) }
    var label0      by remember(existing) { mutableStateOf(existing.mealOption0Label) }
    var label1      by remember(existing) { mutableStateOf(existing.mealOption1Label) }

    var nameError   by remember { mutableStateOf<String?>(null) }
    var emailError  by remember { mutableStateOf<String?>(null) }
    var phoneError  by remember { mutableStateOf<String?>(null) }
    var roomError   by remember { mutableStateOf<String?>(null) }

    var showMealInfo by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HostelFill", fontWeight = FontWeight.Bold)
                        Text(
                            if (isEditing) "Edit Profile" else "Profile Setup",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            if (!isEditing) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Enter your details once. HostelFill will use them to fill every hostel form automatically.",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ── Personal Details ──────────────────────────────────────────────
            Text("Personal Details", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; nameError = null },
                label = { Text("Full Name *") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text("Email Address *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = { Text("Phone Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = room,
                onValueChange = { room = it; roomError = null },
                label = { Text("Room Number *") },
                isError = roomError != null,
                supportingText = roomError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(4.dp))
            Divider()
            Spacer(Modifier.height(4.dp))

            // ── Meal Preferences ──────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Meal Preferences", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { showMealInfo = !showMealInfo }) {
                    Icon(Icons.Default.Info, contentDescription = "Meal info", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (showMealInfo) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "The hostel form uses options \"0\" and \"1\" for each meal. " +
                               "The exact meaning (e.g., No/Yes, Skip/Take) depends on your hostel. " +
                               "Set custom labels below so you can remember which is which. " +
                               "The actual value sent to the form is always 0 or 1.",
                        fontSize = 12.sp
                    )
                }
            }

            // Label configuration
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label0,
                    onValueChange = { label0 = it },
                    label = { Text("Label for \"0\"") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("e.g., No meal") }
                )
                OutlinedTextField(
                    value = label1,
                    onValueChange = { label1 = it },
                    label = { Text("Label for \"1\"") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("e.g., Take meal") }
                )
            }

            MealSelector("Breakfast", breakfast, label0, label1) { breakfast = it }
            MealSelector("Lunch",     lunch,     label0, label1) { lunch     = it }
            MealSelector("Dinner",    dinner,    label0, label1) { dinner    = it }

            Spacer(Modifier.height(8.dp))

            // ── Save Button ───────────────────────────────────────────────────
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = {
                    // Validate
                    var ok = true
                    if (fullName.isBlank()) { nameError  = "Required"; ok = false }
                    if (!email.isValidEmail()) { emailError = "Enter a valid email"; ok = false }
                    if (!phone.isValidPhone()) { phoneError = "Enter a valid phone number"; ok = false }
                    if (room.isBlank()) { roomError  = "Required"; ok = false }

                    if (ok) {
                        viewModel.saveProfile(
                            UserProfile(
                                fullName        = fullName,
                                email           = email,
                                phoneNumber     = phone,
                                roomNumber      = room,
                                breakfastChoice = breakfast,
                                lunchChoice     = lunch,
                                dinnerChoice    = dinner,
                                mealOption0Label = label0.ifBlank { "Option 0" },
                                mealOption1Label = label1.ifBlank { "Option 1" }
                            )
                        )
                        onComplete()
                    }
                }
            ) {
                Text(if (isEditing) "Save Changes" else "Save Profile", fontSize = 16.sp)
            }

            if (isEditing) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.clearProfile()
                        onComplete()
                    }
                ) {
                    Text("Clear All Data")
                }
            }

            Text(
                "By Pratham Shah",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MealSelector(
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
        Text(label, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value == "0",
                onClick  = { onSelect("0") },
                label    = { Text("0 — ${label0.ifBlank { "Option 0" }}") }
            )
            FilterChip(
                selected = value == "1",
                onClick  = { onSelect("1") },
                label    = { Text("1 — ${label1.ifBlank { "Option 1" }}") }
            )
        }
    }
}
