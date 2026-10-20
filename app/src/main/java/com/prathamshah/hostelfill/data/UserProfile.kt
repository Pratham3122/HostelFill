package com.prathamshah.hostelfill.data

/**
 * Represents the locally-stored user profile for form autofill.
 * All fields map directly to the seven hostel form questions.
 *
 * Meal values "0" and "1" are stored as-is; their meaning
 * (e.g., "No/Yes" or "Skip/Take") is configured and labelled by the user.
 */
data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val roomNumber: String = "",

    // Meal preferences — stored as the raw option value the form expects ("0" or "1")
    val breakfastChoice: String = "1",
    val lunchChoice: String = "1",
    val dinnerChoice: String = "1",

    // Human-readable labels for the values, configurable by the user
    val mealOption0Label: String = "Option 0",
    val mealOption1Label: String = "Option 1"
)

/** True when all mandatory form fields are filled in. */
fun UserProfile.isComplete(): Boolean =
    fullName.isNotBlank() &&
    email.isNotBlank() &&
    phoneNumber.isNotBlank() &&
    roomNumber.isNotBlank()

/** Validate email format minimally. */
fun String.isValidEmail(): Boolean =
    isNotBlank() && contains('@') && contains('.')

/** Validate phone: digits only, 7–15 chars. */
fun String.isValidPhone(): Boolean =
    isNotBlank() && all { it.isDigit() || it == '+' || it == '-' || it == ' ' } &&
    count { it.isDigit() } in 7..15
