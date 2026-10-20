package com.prathamshah.hostelfill

import com.prathamshah.hostelfill.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for URL validation, field mapping, label normalization,
 * and profile validation helpers.
 * Run with: ./gradlew :app:test
 */
class HostelFillUnitTests {

    // ── URL Validation ────────────────────────────────────────────────────────

    @Test fun `valid forms_gle URL accepted`() {
        val r = UrlValidator.validate("https://forms.gle/abc123XYZ")
        assertTrue(r is UrlValidationResult.Valid)
    }

    @Test fun `valid docs_google forms URL accepted`() {
        val r = UrlValidator.validate("https://docs.google.com/forms/d/e/abc/viewform")
        assertTrue(r is UrlValidationResult.Valid)
    }

    @Test fun `empty URL rejected`() {
        val r = UrlValidator.validate("")
        assertTrue(r is UrlValidationResult.Invalid)
    }

    @Test fun `http URL rejected`() {
        val r = UrlValidator.validate("http://forms.gle/abc")
        assertTrue(r is UrlValidationResult.Invalid)
    }

    @Test fun `arbitrary HTTPS URL rejected`() {
        val r = UrlValidator.validate("https://malicious.com/phish")
        assertTrue(r is UrlValidationResult.Invalid)
    }

    @Test fun `docs_google non-forms URL rejected`() {
        val r = UrlValidator.validate("https://docs.google.com/document/d/abc")
        assertTrue(r is UrlValidationResult.Invalid)
    }

    @Test fun `URL with leading whitespace accepted after trim`() {
        val r = UrlValidator.validate("  https://forms.gle/xyz  ")
        assertTrue(r is UrlValidationResult.Valid)
    }

    // ── Label Normalization ───────────────────────────────────────────────────

    @Test fun `normalizeLabel lowercases and trims`() {
        assertEquals("full name", normalizeLabel("  Full Name  "))
    }

    @Test fun `normalizeLabel strips trailing colon and asterisk`() {
        assertEquals("email", normalizeLabel("EMAIL:*"))
    }

    @Test fun `normalizeLabel collapses multiple spaces`() {
        assertEquals("room number", normalizeLabel("Room   Number"))
    }

    // ── Field Mapping ─────────────────────────────────────────────────────────

    @Test fun `exact EMAIL label resolves to email field`() {
        assertEquals(FieldKey.EMAIL, resolveField("EMAIL", emptyMap()))
    }

    @Test fun `email address label resolves`() {
        assertEquals(FieldKey.EMAIL, resolveField("Email Address", emptyMap()))
    }

    @Test fun `PHONE NUMBER resolves to phone`() {
        assertEquals(FieldKey.PHONE, resolveField("PHONE NUMBER", emptyMap()))
    }

    @Test fun `Mobile Number resolves to phone`() {
        assertEquals(FieldKey.PHONE, resolveField("Mobile Number", emptyMap()))
    }

    @Test fun `Contact Number resolves to phone`() {
        assertEquals(FieldKey.PHONE, resolveField("Contact Number", emptyMap()))
    }

    @Test fun `ROOM NUMBER resolves to room`() {
        assertEquals(FieldKey.ROOM, resolveField("ROOM NUMBER", emptyMap()))
    }

    @Test fun `Room No resolves to room`() {
        assertEquals(FieldKey.ROOM, resolveField("Room No.", emptyMap()))
    }

    @Test fun `FULL NAME resolves to full_name`() {
        assertEquals(FieldKey.FULL_NAME, resolveField("FULL NAME", emptyMap()))
    }

    @Test fun `Student Name resolves to full_name`() {
        assertEquals(FieldKey.FULL_NAME, resolveField("Student Name", emptyMap()))
    }

    @Test fun `BREAKFAST resolves to breakfast`() {
        assertEquals(FieldKey.BREAKFAST, resolveField("BREAKFAST", emptyMap()))
    }

    @Test fun `LUNCH resolves to lunch`() {
        assertEquals(FieldKey.LUNCH, resolveField("LUNCH", emptyMap()))
    }

    @Test fun `DINNER resolves to dinner`() {
        assertEquals(FieldKey.DINNER, resolveField("DINNER", emptyMap()))
    }

    @Test fun `unknown label returns UNKNOWN`() {
        assertEquals(FieldKey.UNKNOWN, resolveField("Weekly Menu", emptyMap()))
    }

    @Test fun `custom mapping overrides builtin`() {
        val custom = mapOf("weekly menu" to FieldKey.SKIP)
        assertEquals(FieldKey.SKIP, resolveField("Weekly Menu", custom))
    }

    // ── Profile Validation ────────────────────────────────────────────────────

    @Test fun `complete profile passes isComplete check`() {
        val p = UserProfile("Ali", "ali@example.com", "9876543210", "A101")
        assertTrue(p.isComplete())
    }

    @Test fun `profile with blank name fails isComplete`() {
        val p = UserProfile("", "ali@example.com", "9876543210", "A101")
        assertFalse(p.isComplete())
    }

    @Test fun `valid email passes`() {
        assertTrue("test@example.com".isValidEmail())
    }

    @Test fun `email without at-sign fails`() {
        assertFalse("notanemail.com".isValidEmail())
    }

    @Test fun `valid 10-digit phone passes`() {
        assertTrue("9876543210".isValidPhone())
    }

    @Test fun `phone with country code passes`() {
        assertTrue("+91 98765 43210".isValidPhone())
    }

    @Test fun `three-digit phone fails`() {
        assertFalse("123".isValidPhone())
    }

    // ── JSON Array Parser (ViewModel internal, tested via reflection or duplicate) ──

    @Test fun `autofill script builds without exception`() {
        val profile = UserProfile("Test User", "test@example.com", "9999999999", "A101")
        val js = AutofillScript.build(profile, emptyMap(), autoSubmit = false)
        assertTrue(js.contains("Android.onAutofillResult"))
        assertTrue(js.contains("Test User"))
        assertTrue(js.contains("test@example.com"))
    }

    @Test fun `autofill script escapes double quotes in name`() {
        val profile = UserProfile("Ali \"The\" Shah", "a@b.com", "1234567890", "B2")
        val js = AutofillScript.build(profile, emptyMap(), false)
        // Should not break JS string syntax
        assertFalse(js.contains("\"The\"")) // raw unescaped quote should not appear mid-string
    }
}
