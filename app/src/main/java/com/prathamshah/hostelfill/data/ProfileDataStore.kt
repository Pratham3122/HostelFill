package com.prathamshah.hostelfill.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property for single DataStore instance per process
val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

/**
 * Reads and writes the UserProfile to Android DataStore (Preferences).
 * All data stays on the device — no network calls.
 */
class ProfileDataStore(private val context: Context) {

    companion object {
        val KEY_FULL_NAME       = stringPreferencesKey("full_name")
        val KEY_EMAIL           = stringPreferencesKey("email")
        val KEY_PHONE           = stringPreferencesKey("phone_number")
        val KEY_ROOM            = stringPreferencesKey("room_number")
        val KEY_BREAKFAST       = stringPreferencesKey("breakfast_choice")
        val KEY_LUNCH           = stringPreferencesKey("lunch_choice")
        val KEY_DINNER          = stringPreferencesKey("dinner_choice")
        val KEY_MEAL_0_LABEL    = stringPreferencesKey("meal_option_0_label")
        val KEY_MEAL_1_LABEL    = stringPreferencesKey("meal_option_1_label")
        val KEY_PROFILE_SAVED   = booleanPreferencesKey("profile_saved")
    }

    /** Emits the current profile whenever DataStore changes. */
    val profileFlow: Flow<UserProfile> = context.profileDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            UserProfile(
                fullName       = prefs[KEY_FULL_NAME]    ?: "",
                email          = prefs[KEY_EMAIL]        ?: "",
                phoneNumber    = prefs[KEY_PHONE]        ?: "",
                roomNumber     = prefs[KEY_ROOM]         ?: "",
                breakfastChoice = prefs[KEY_BREAKFAST]   ?: "1",
                lunchChoice    = prefs[KEY_LUNCH]        ?: "1",
                dinnerChoice   = prefs[KEY_DINNER]       ?: "1",
                mealOption0Label = prefs[KEY_MEAL_0_LABEL] ?: "Option 0",
                mealOption1Label = prefs[KEY_MEAL_1_LABEL] ?: "Option 1"
            )
        }

    /** True once the user has completed the first-time profile setup. */
    val isProfileSavedFlow: Flow<Boolean> = context.profileDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_PROFILE_SAVED] ?: false }

    /** Persist the profile to DataStore. */
    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[KEY_FULL_NAME]      = profile.fullName.trim()
            prefs[KEY_EMAIL]          = profile.email.trim()
            prefs[KEY_PHONE]          = profile.phoneNumber.trim()
            prefs[KEY_ROOM]           = profile.roomNumber.trim()
            prefs[KEY_BREAKFAST]      = profile.breakfastChoice
            prefs[KEY_LUNCH]          = profile.lunchChoice
            prefs[KEY_DINNER]         = profile.dinnerChoice
            prefs[KEY_MEAL_0_LABEL]   = profile.mealOption0Label.trim()
            prefs[KEY_MEAL_1_LABEL]   = profile.mealOption1Label.trim()
            prefs[KEY_PROFILE_SAVED]  = true
        }
    }

    /** Clear all stored profile data. */
    suspend fun clearProfile() {
        context.profileDataStore.edit { it.clear() }
    }
}
