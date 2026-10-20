package com.prathamshah.hostelfill.data

import android.net.Uri

/**
 * Result of validating a URL pasted by the user.
 */
sealed class UrlValidationResult {
    data class Valid(val resolvedUrl: String) : UrlValidationResult()
    data class Invalid(val reason: String) : UrlValidationResult()
}

/**
 * Validates and normalizes a Google Forms URL.
 *
 * Accepts:
 *   https://forms.gle/...
 *   https://docs.google.com/forms/d/e/.../viewform
 *   https://docs.google.com/forms/d/.../viewform
 *
 * Rejects everything else.
 */
object UrlValidator {

    private val ALLOWED_HOSTS = setOf(
        "forms.gle",
        "docs.google.com"
    )

    fun validate(raw: String): UrlValidationResult {
        val trimmed = raw.trim()

        if (trimmed.isBlank()) {
            return UrlValidationResult.Invalid("Please paste a Google Forms link.")
        }

        val uri = try {
            Uri.parse(trimmed)
        } catch (e: Exception) {
            return UrlValidationResult.Invalid("The link could not be parsed.")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "https") {
            return UrlValidationResult.Invalid(
                "Only secure HTTPS links are accepted. Received: $scheme"
            )
        }

        val host = uri.host?.lowercase() ?: ""
        if (host !in ALLOWED_HOSTS) {
            return UrlValidationResult.Invalid(
                "Link must be from forms.gle or docs.google.com. Received: $host"
            )
        }

        // docs.google.com links must contain /forms/ in the path
        if (host == "docs.google.com") {
            val path = uri.path ?: ""
            if (!path.contains("/forms/")) {
                return UrlValidationResult.Invalid(
                    "The link doesn't appear to be a Google Form URL."
                )
            }
        }

        return UrlValidationResult.Valid(trimmed)
    }
}
