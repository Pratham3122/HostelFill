package com.prathamshah.hostelfill.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prathamshah.hostelfill.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Autofill status shown to the user. */
sealed class AutofillStatus {
    object Idle : AutofillStatus()
    object Loading : AutofillStatus()
    data class WaitingForLogin(val message: String) : AutofillStatus()
    data class Running(val message: String) : AutofillStatus()
    data class Complete(
        val filled: List<String>,
        val skipped: List<String>,
        val unknown: List<String>,
        val errors: List<String>,
        val submitted: Boolean
    ) : AutofillStatus()
    data class Error(val message: String) : AutofillStatus()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val profileStore   = ProfileDataStore(app)
    private val mappingsStore  = FormMappingsStore(app)

    // ── Profile ───────────────────────────────────────────────────────────────
    val profile: StateFlow<UserProfile> = profileStore.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val isProfileSaved: StateFlow<Boolean> = profileStore.isProfileSavedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val customMappings: StateFlow<Map<String, String>> = mappingsStore.mappingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun saveProfile(p: UserProfile) = viewModelScope.launch { profileStore.saveProfile(p) }
    fun clearProfile() = viewModelScope.launch {
        profileStore.clearProfile()
        mappingsStore.clearAll()
    }

    // ── URL / form state ──────────────────────────────────────────────────────
    private val _formUrl = MutableStateFlow("")
    val formUrl: StateFlow<String> = _formUrl.asStateFlow()

    private val _urlError = MutableStateFlow<String?>(null)
    val urlError: StateFlow<String?> = _urlError.asStateFlow()

    /** Validated URL ready to load in the WebView; null if not yet validated. */
    private val _validatedUrl = MutableStateFlow<String?>(null)
    val validatedUrl: StateFlow<String?> = _validatedUrl.asStateFlow()

    fun setFormUrl(url: String) {
        _formUrl.value = url
        _urlError.value = null
        _validatedUrl.value = null
    }

    fun validateAndOpenForm() {
        when (val result = UrlValidator.validate(_formUrl.value)) {
            is UrlValidationResult.Valid   -> {
                _urlError.value = null
                _validatedUrl.value = result.resolvedUrl
                _autofillStatus.value = AutofillStatus.Idle
            }
            is UrlValidationResult.Invalid -> {
                _urlError.value = result.reason
                _validatedUrl.value = null
            }
        }
    }

    fun clearValidatedUrl() {
        _validatedUrl.value = null
    }

    // ── Autofill mode ─────────────────────────────────────────────────────────
    private val _autoSubmitEnabled = MutableStateFlow(false)
    val autoSubmitEnabled: StateFlow<Boolean> = _autoSubmitEnabled.asStateFlow()

    fun toggleAutoSubmit(enabled: Boolean) { _autoSubmitEnabled.value = enabled }

    // ── Autofill status ───────────────────────────────────────────────────────
    private val _autofillStatus = MutableStateFlow<AutofillStatus>(AutofillStatus.Idle)
    val autofillStatus: StateFlow<AutofillStatus> = _autofillStatus.asStateFlow()

    fun setStatus(status: AutofillStatus) { _autofillStatus.value = status }

    /**
     * Called by the WebView's JavaScript interface when the autofill script
     * completes.  Parses the JSON result string and updates status.
     *
     * Expected JSON:
     * { "filled": [...], "skipped": [...], "unknown": [...], "errors": [...] }
     */
    fun onAutofillResult(jsonResult: String) {
        try {
            val filled   = parseJsonArray(jsonResult, "filled")
            val skipped  = parseJsonArray(jsonResult, "skipped")
            val unknown  = parseJsonArray(jsonResult, "unknown")
            val errors   = parseJsonArray(jsonResult, "errors")
            val submitted = filled.contains("__SUBMITTED__")
            val cleanFilled = filled.filter { it != "__SUBMITTED__" }

            _autofillStatus.value = AutofillStatus.Complete(
                filled    = cleanFilled,
                skipped   = skipped,
                unknown   = unknown,
                errors    = errors,
                submitted = submitted
            )

            // If there are unknown questions, save them for the user to configure
            // (the UI will prompt the user)
        } catch (e: Exception) {
            _autofillStatus.value = AutofillStatus.Error(
                "Failed to parse autofill result: ${e.message}"
            )
        }
    }

    fun onLoginRequired() {
        _autofillStatus.value = AutofillStatus.WaitingForLogin(
            "Google sign-in required. Please sign in to your Google account in the form below, " +
            "then tap 'Start Autofill' again after the form loads."
        )
    }

    fun onFormLoading() { _autofillStatus.value = AutofillStatus.Loading }
    fun onFormReady()   { _autofillStatus.value = AutofillStatus.Idle }

    // ── Custom mappings ───────────────────────────────────────────────────────
    fun saveMappingForUnknown(normalizedLabel: String, fieldKey: String) =
        viewModelScope.launch { mappingsStore.saveMapping(normalizedLabel, fieldKey) }

    fun removeMapping(normalizedLabel: String) =
        viewModelScope.launch { mappingsStore.removeMapping(normalizedLabel) }

    // ── JS override for per-submission meal override ──────────────────────────
    /** Temporary meal overrides for the current submission only. */
    private val _mealOverride = MutableStateFlow<Triple<String,String,String>?>(null)

    /** Returns the effective profile for this submission (with any meal overrides). */
    fun effectiveProfile(): UserProfile {
        val base = profile.value
        val ov   = _mealOverride.value ?: return base
        return base.copy(breakfastChoice = ov.first, lunchChoice = ov.second, dinnerChoice = ov.third)
    }

    fun setMealOverride(breakfast: String, lunch: String, dinner: String) {
        _mealOverride.value = Triple(breakfast, lunch, dinner)
    }

    fun clearMealOverride() { _mealOverride.value = null }

    // ── Simple JSON array parser (no external lib) ────────────────────────────
    private fun parseJsonArray(json: String, key: String): List<String> {
        val pattern = Regex("\"$key\"\\s*:\\s*\\[([^]]*)]", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(json) ?: return emptyList()
        val inner = match.groupValues[1].trim()
        if (inner.isBlank()) return emptyList()
        return Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
            .findAll(inner)
            .map { it.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n") }
            .toList()
    }
}
