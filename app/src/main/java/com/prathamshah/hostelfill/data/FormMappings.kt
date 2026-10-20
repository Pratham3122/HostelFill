package com.prathamshah.hostelfill.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.mappingsDataStore by preferencesDataStore(name = "form_mappings")

/**
 * Stores user-configured question-label → profile-field mappings.
 * Used when a new or renamed question is encountered.
 *
 * Key   : normalized question label (lowercase, trimmed)
 * Value : profile field key constant (e.g., "full_name", "email", …)
 */
class FormMappingsStore(private val context: Context) {

    /** All currently saved custom mappings as a Flow<Map<label, fieldKey>>. */
    val mappingsFlow: Flow<Map<String, String>> = context.mappingsDataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { prefs -> prefs.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() } }

    /** Save a single custom mapping. */
    suspend fun saveMapping(normalizedLabel: String, fieldKey: String) {
        val prefKey = stringPreferencesKey(normalizedLabel)
        context.mappingsDataStore.edit { it[prefKey] = fieldKey }
    }

    /** Remove a mapping by its label. */
    suspend fun removeMapping(normalizedLabel: String) {
        val prefKey = stringPreferencesKey(normalizedLabel)
        context.mappingsDataStore.edit { it.remove(prefKey) }
    }

    /** Clear all custom mappings. */
    suspend fun clearAll() {
        context.mappingsDataStore.edit { it.clear() }
    }
}

// ─── Profile field key constants ──────────────────────────────────────────────
object FieldKey {
    const val FULL_NAME   = "full_name"
    const val EMAIL       = "email"
    const val PHONE       = "phone_number"
    const val ROOM        = "room_number"
    const val BREAKFAST   = "breakfast"
    const val LUNCH       = "lunch"
    const val DINNER      = "dinner"
    const val SKIP        = "_skip"   // explicitly marked as "ignore this field"
    const val UNKNOWN     = "_unknown"
}

/**
 * Built-in question-label synonyms shipped with the app.
 * Keys are lowercase normalized labels; values are [FieldKey] constants.
 */
val BUILTIN_MAPPINGS: Map<String, String> = mapOf(
    // Email
    "email"                     to FieldKey.EMAIL,
    "email address"             to FieldKey.EMAIL,
    "e-mail"                    to FieldKey.EMAIL,
    "e-mail address"            to FieldKey.EMAIL,

    // Phone
    "phone number"              to FieldKey.PHONE,
    "phone"                     to FieldKey.PHONE,
    "mobile number"             to FieldKey.PHONE,
    "mobile"                    to FieldKey.PHONE,
    "contact number"            to FieldKey.PHONE,
    "contact"                   to FieldKey.PHONE,
    "phone no"                  to FieldKey.PHONE,
    "mobile no"                 to FieldKey.PHONE,

    // Room
    "room number"               to FieldKey.ROOM,
    "room no"                   to FieldKey.ROOM,
    "room no."                  to FieldKey.ROOM,
    "room"                      to FieldKey.ROOM,
    "hostel room number"        to FieldKey.ROOM,

    // Full name
    "full name"                 to FieldKey.FULL_NAME,
    "name"                      to FieldKey.FULL_NAME,
    "student name"              to FieldKey.FULL_NAME,
    "name of student"           to FieldKey.FULL_NAME,
    "your name"                 to FieldKey.FULL_NAME,
    "full name of student"      to FieldKey.FULL_NAME,

    // Meal
    "breakfast"                 to FieldKey.BREAKFAST,
    "lunch"                     to FieldKey.LUNCH,
    "dinner"                    to FieldKey.DINNER
)

/**
 * Normalize a question label for matching:
 * - lowercase
 * - collapse whitespace
 * - strip trailing punctuation
 */
fun normalizeLabel(raw: String): String =
    raw.trim().lowercase().replace(Regex("\\s+"), " ").trimEnd(':', '*', '?', '.')

/**
 * Resolve a question label to a [FieldKey] constant.
 * Custom mappings override built-in ones.
 */
fun resolveField(
    rawLabel: String,
    customMappings: Map<String, String>
): String {
    val norm = normalizeLabel(rawLabel)
    return customMappings[norm]
        ?: BUILTIN_MAPPINGS[norm]
        ?: FieldKey.UNKNOWN
}
