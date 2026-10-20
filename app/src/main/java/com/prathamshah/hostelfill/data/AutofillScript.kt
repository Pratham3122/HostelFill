package com.prathamshah.hostelfill.data

/**
 * Generates the JavaScript injected into the Google Forms WebView
 * to detect questions and fill them using the saved profile.
 *
 * How Google Forms works (as of 2024):
 *  - Each question is a <div role="listitem"> containing a heading <span> with the label.
 *  - Text/email/phone inputs are <input type="text"> or <input type="email">.
 *  - Radio groups: each option is a <div role="radio" aria-label="..."> or a <label>.
 *  - The form uses data-params attributes but labels in <span> are reliable for detection.
 *
 * Authentication notes:
 *  - If Google requires sign-in, the page URL will be accounts.google.com.
 *    We detect this in WebViewClient.onPageStarted (in Kotlin) and pause autofill.
 *  - When the signed-in user's email is auto-collected by the form, an editable
 *    "Email" input may be absent — we detect this too and skip that field.
 *  - We never touch authentication pages or attempt to fill passwords.
 *
 * The script returns a JSON string to the Android layer via
 *   Android.onAutofillResult(jsonString)
 *
 * Result JSON shape:
 * {
 *   "filled"   : ["Full Name", "Room Number", ...],
 *   "skipped"  : ["Email (auto-collected)"],
 *   "unknown"  : ["Question text that had no mapping"],
 *   "errors"   : ["Descriptive error messages"]
 * }
 */
object AutofillScript {

    /**
     * Build the JS to inject.
     *
     * @param profile       The user's saved profile.
     * @param customMappings User-configured label → fieldKey overrides.
     * @param autoSubmit    If true the script clicks the submit button after filling.
     */
    fun build(
        profile: UserProfile,
        customMappings: Map<String, String>,
        autoSubmit: Boolean
    ): String {
        // Escape strings for safe JS embedding
        val jsName      = profile.fullName.jsEscape()
        val jsEmail     = profile.email.jsEscape()
        val jsPhone     = profile.phoneNumber.jsEscape()
        val jsRoom      = profile.roomNumber.jsEscape()
        val jsBreakfast = profile.breakfastChoice.jsEscape()
        val jsLunch     = profile.lunchChoice.jsEscape()
        val jsDinner    = profile.dinnerChoice.jsEscape()

        // Build the combined mapping table (builtin + custom) as a JS object literal
        val allMappings: Map<String, String> = BUILTIN_MAPPINGS + customMappings
        val jsMappings = allMappings.entries.joinToString(",\n") { (k, v) ->
            "  ${k.jsQuote()}: ${v.jsQuote()}"
        }

        // Build the profile values table as a JS object literal
        val jsProfile = """
  "${FieldKey.FULL_NAME}" : "$jsName",
  "${FieldKey.EMAIL}"     : "$jsEmail",
  "${FieldKey.PHONE}"     : "$jsPhone",
  "${FieldKey.ROOM}"      : "$jsRoom",
  "${FieldKey.BREAKFAST}" : "$jsBreakfast",
  "${FieldKey.LUNCH}"     : "$jsLunch",
  "${FieldKey.DINNER}"    : "$jsDinner"
""".trimIndent()

        return """
(function() {
  'use strict';

  var MAPPINGS = {
$jsMappings
  };

  var PROFILE = {
    $jsProfile
  };

  var filled   = [];
  var skipped  = [];
  var unknown  = [];
  var errors   = [];

  // ── Helpers ────────────────────────────────────────────────────────────────

  function normalizeLabel(s) {
    return s.trim().toLowerCase().replace(/\s+/g, ' ').replace(/[:*?.]+${'$'}/, '');
  }

  function resolveField(label) {
    var norm = normalizeLabel(label);
    return MAPPINGS[norm] || '_unknown';
  }

  function fireInputEvent(el) {
    ['input', 'change', 'blur'].forEach(function(type) {
      el.dispatchEvent(new Event(type, { bubbles: true }));
    });
  }

  function setInputValue(input, value) {
    // React / Angular compatible value setter
    var nativeInputValueSetter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype, 'value'
    );
    if (nativeInputValueSetter && nativeInputValueSetter.set) {
      nativeInputValueSetter.set.call(input, value);
    } else {
      input.value = value;
    }
    fireInputEvent(input);
  }

  function setTextareaValue(ta, value) {
    var nativeSetter = Object.getOwnPropertyDescriptor(
      window.HTMLTextAreaElement.prototype, 'value'
    );
    if (nativeSetter && nativeSetter.set) {
      nativeSetter.set.call(ta, value);
    } else {
      ta.value = value;
    }
    fireInputEvent(ta);
  }

  // ── Email auto-collection detection ─────────────────────────────────────────
  // When the form is set to "Collect email addresses automatically",
  // a static text shows the signed-in email with no editable input.
  function isEmailAutoCollected() {
    var autoMsg = document.querySelector('[data-email-display]');
    if (autoMsg) return true;
    // Also check for "Email will be recorded" static notice
    var notices = document.querySelectorAll('div, span, p');
    for (var i = 0; i < notices.length; i++) {
      var t = notices[i].textContent || '';
      if (t.match(/email.*recorded|email.*auto|automatically collect/i)
          && !notices[i].querySelector('input')) {
        return true;
      }
    }
    return false;
  }

  // ── Question discovery ────────────────────────────────────────────────────
  // Google Forms renders each question as a role="listitem" block.
  // The heading/label is in a nested <span> or <div> with role="heading",
  // or as the first text-bearing <span> in the block.
  function getQuestionBlocks() {
    // Primary selector (modern Google Forms)
    var blocks = Array.from(document.querySelectorAll('[role="listitem"]'));
    if (blocks.length === 0) {
      // Fallback: older Forms layout
      blocks = Array.from(document.querySelectorAll('.freebirdFormviewerComponentsQuestionBaseRoot'));
    }
    return blocks;
  }

  function getLabelFromBlock(block) {
    // Try role=heading first
    var heading = block.querySelector('[role="heading"]');
    if (heading) return (heading.textContent || '').trim();
    // Try aria-label on the block itself
    var aria = block.getAttribute('aria-label');
    if (aria) return aria.trim();
    // Fall back to first significant span
    var spans = block.querySelectorAll('span');
    for (var i = 0; i < spans.length; i++) {
      var t = (spans[i].textContent || '').trim();
      if (t.length > 1 && !t.match(/^\*$/)) return t;
    }
    return '';
  }

  // ── Text field filling ────────────────────────────────────────────────────
  function fillTextField(block, value) {
    var input = block.querySelector('input[type="text"], input[type="email"], input[type="tel"], input:not([type])');
    if (input && !input.disabled && !input.readOnly) {
      setInputValue(input, value);
      return true;
    }
    var ta = block.querySelector('textarea');
    if (ta && !ta.disabled && !ta.readOnly) {
      setTextareaValue(ta, value);
      return true;
    }
    return false;
  }

  // ── Radio button filling ──────────────────────────────────────────────────
  // Google Forms radio options have role="radio" with aria-label = option text.
  function fillRadio(block, targetValue) {
    // Try role=radio elements
    var radios = Array.from(block.querySelectorAll('[role="radio"]'));
    for (var i = 0; i < radios.length; i++) {
      var r = radios[i];
      var label = (r.getAttribute('aria-label') || r.textContent || '').trim();
      if (label === targetValue) {
        r.click();
        return true;
      }
    }
    // Fallback: <input type="radio"> with adjacent label
    var inputs = Array.from(block.querySelectorAll('input[type="radio"]'));
    for (var i = 0; i < inputs.length; i++) {
      var inp = inputs[i];
      var lbl = block.querySelector('label[for="' + inp.id + '"]');
      var lblText = lbl ? lbl.textContent.trim() : '';
      if (lblText === targetValue || inp.value === targetValue) {
        inp.click();
        return true;
      }
    }
    return false;
  }

  // ── Email field special handling ──────────────────────────────────────────
  function handleEmailField(block, emailValue) {
    if (isEmailAutoCollected()) {
      skipped.push('Email (automatically collected by Google — not overridden)');
      return;
    }
    // Check if the input is readonly (auto-filled by Google sign-in)
    var input = block.querySelector('input[type="text"], input[type="email"]');
    if (input && (input.readOnly || input.disabled)) {
      skipped.push('Email (read-only — set by your signed-in Google account)');
      return;
    }
    if (fillTextField(block, emailValue)) {
      filled.push('Email');
    } else {
      errors.push('Email field found but could not be filled.');
    }
  }

  // ── Main fill loop ────────────────────────────────────────────────────────
  var blocks = getQuestionBlocks();

  if (blocks.length === 0) {
    errors.push('No form questions detected. The form may still be loading — try again in a moment.');
    Android.onAutofillResult(JSON.stringify({ filled: filled, skipped: skipped, unknown: unknown, errors: errors }));
    return;
  }

  blocks.forEach(function(block) {
    var label = getLabelFromBlock(block);
    if (!label) return; // no label = probably a section header or image

    var fieldKey = resolveField(label);

    if (fieldKey === '_unknown') {
      unknown.push(label);
      return;
    }
    if (fieldKey === '_skip') {
      skipped.push(label + ' (configured to skip)');
      return;
    }

    var value = PROFILE[fieldKey];
    if (!value && value !== '0') {
      skipped.push(label + ' (no value in profile)');
      return;
    }

    var ok = false;

    if (fieldKey === '${FieldKey.EMAIL}') {
      handleEmailField(block, value);
      return; // handleEmailField manages its own result reporting
    }

    // Meal fields use radio buttons
    if (fieldKey === '${FieldKey.BREAKFAST}' || fieldKey === '${FieldKey.LUNCH}' || fieldKey === '${FieldKey.DINNER}') {
      ok = fillRadio(block, value);
      if (!ok) {
        // Retry: try clicking the option by index (0 = first option, 1 = second)
        var idx = parseInt(value, 10);
        var radios = Array.from(block.querySelectorAll('[role="radio"]'));
        if (!isNaN(idx) && idx >= 0 && idx < radios.length) {
          radios[idx].click();
          ok = true;
        }
      }
    } else {
      ok = fillTextField(block, value);
    }

    if (ok) {
      filled.push(label);
    } else {
      errors.push('Could not fill: ' + label);
    }
  });

  // ── Auto-submit ───────────────────────────────────────────────────────────
  if (${if (autoSubmit) "true" else "false"}) {
    var submitBtn = document.querySelector('[role="button"][jsname="M2UYVd"]')
      || document.querySelector('div[jscontroller][jsaction*="submit"]')
      || Array.from(document.querySelectorAll('[role="button"]'))
           .find(function(el) { return /submit|send/i.test(el.textContent); });
    if (submitBtn) {
      if (errors.length === 0 && unknown.length === 0) {
        submitBtn.click();
        filled.push('__SUBMITTED__');
      } else {
        errors.push('Auto-submit skipped: ' + errors.length + ' error(s) and ' + unknown.length + ' unknown question(s) must be resolved first.');
      }
    } else {
      errors.push('Submit button not found — please tap it manually.');
    }
  }

  Android.onAutofillResult(JSON.stringify({
    filled  : filled,
    skipped : skipped,
    unknown : unknown,
    errors  : errors
  }));

})();
""".trimIndent()
    }

    private fun String.jsEscape(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")

    private fun String.jsQuote(): String = "\"${jsEscape()}\""
}
