package com.prathamshah.hostelfill.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.prathamshah.hostelfill.data.AutofillScript
import com.prathamshah.hostelfill.viewmodel.AutofillStatus
import com.prathamshah.hostelfill.viewmodel.MainViewModel

/**
 * Hosts a WebView that loads the Google Form and coordinates autofill.
 *
 * Authentication handling:
 *  - If the URL resolves to accounts.google.com (Google sign-in), we detect it
 *    in onPageStarted and call viewModel.onLoginRequired().
 *  - The user signs in normally inside the WebView — no credentials are captured.
 *  - Once back on forms.google.com the user can tap "Start Autofill" again.
 *
 * JavaScript bridge:
 *  - Android.onAutofillResult(json) — called by the injected JS with results.
 *
 * Limitations documented:
 *  - Cannot autofill while a sign-in page is showing.
 *  - If Google changes the Forms DOM structure, JS selectors may need updating.
 *  - CAPTCHA and file-upload fields are not automatable and require manual action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FormWebViewScreen(
    initialUrl: String,
    autoSubmit: Boolean,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val profile        by viewModel.profile.collectAsState()
    val customMappings by viewModel.customMappings.collectAsState()
    val status         by viewModel.autofillStatus.collectAsState()

    var webView        by remember { mutableStateOf<WebView?>(null) }
    var currentUrl     by remember { mutableStateOf(initialUrl) }
    var pageTitle      by remember { mutableStateOf("Loading…") }
    var isOnLoginPage  by remember { mutableStateOf(false) }
    var isPageReady    by remember { mutableStateOf(false) }
    var autofillTriggered by remember { mutableStateOf(false) }

    fun isLoginUrl(url: String?) =
        url != null && (url.contains("accounts.google.com") || url.contains("signin"))

    fun isFormUrl(url: String?) =
        url != null && (url.contains("docs.google.com/forms") || url.contains("forms.gle"))

    fun injectAutofill() {
        val wv = webView ?: return
        val effectiveProfile = viewModel.effectiveProfile()
        val script = AutofillScript.build(effectiveProfile, customMappings, autoSubmit)
        viewModel.setStatus(AutofillStatus.Running("Filling form fields…"))
        wv.evaluateJavascript(script, null)   // result comes back via the JS bridge
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                title = {
                    Column {
                        Text(pageTitle, fontSize = 14.sp, maxLines = 1)
                        Text(
                            if (isOnLoginPage) "Sign in required" else "Google Form",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { webView?.reload(); isPageReady = false; autofillTriggered = false }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    // Start Autofill — disabled on login page or while page is loading
                    IconButton(
                        enabled = isPageReady && !isOnLoginPage,
                        onClick = { injectAutofill() }
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Start Autofill",
                            tint = if (isPageReady && !isOnLoginPage)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Login banner
            if (isOnLoginPage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text(
                            "Google sign-in required. Sign in below, then tap the ✦ button once the form loads.",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Status banner (errors / results preview)
            when (val s = status) {
                is AutofillStatus.Running -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is AutofillStatus.Error   -> {
                    Text(
                        "⚠ ${s.message}",
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                is AutofillStatus.Complete -> {
                    val color = if (s.errors.isEmpty() && s.unknown.isEmpty())
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val icon = if (s.submitted) "✓ Submitted" else "Filled ${s.filled.size} field(s)"
                            Text(icon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (s.skipped.isNotEmpty())
                                Text("Skipped: ${s.skipped.joinToString(", ")}", fontSize = 11.sp)
                            if (s.unknown.isNotEmpty())
                                Text("Unknown: ${s.unknown.joinToString(", ")} — tap Back to map them.", fontSize = 11.sp)
                            if (s.errors.isNotEmpty())
                                Text("Errors: ${s.errors.joinToString("; ")}", fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                else -> {}
            }

            // ── WebView ───────────────────────────────────────────────────────
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled      = true
                            domStorageEnabled       = true
                            // Allow Google sign-in redirects within the WebView
                            setSupportMultipleWindows(false)
                            useWideViewPort         = true
                            loadWithOverviewMode    = true
                            // Cookies needed for Google authentication
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this@apply, true)
                        }

                        // JavaScript → Android bridge
                        addJavascriptInterface(
                            AutofillBridge(
                                onResult = { json ->
                                    (ctx as? android.app.Activity)?.runOnUiThread {
                                        viewModel.onAutofillResult(json)
                                    }
                                }
                            ),
                            "Android"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isPageReady   = false
                                autofillTriggered = false
                                currentUrl    = url ?: ""
                                isOnLoginPage = isLoginUrl(url)
                                if (isOnLoginPage) viewModel.onLoginRequired()
                                else viewModel.onFormLoading()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageTitle  = view?.title ?: "Form"
                                currentUrl = url ?: ""
                                isOnLoginPage = isLoginUrl(url)
                                isPageReady = !isOnLoginPage

                                if (isPageReady) {
                                    viewModel.onFormReady()
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    viewModel.setStatus(
                                        AutofillStatus.Error(
                                            "Page load error: ${error?.description ?: "Unknown error"}. " +
                                            "Check your internet connection."
                                        )
                                    )
                                }
                            }

                            // Allow navigation within Google's auth and forms domains only
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                val host = request.url?.host?.lowercase() ?: ""
                                val allowedHosts = setOf(
                                    "docs.google.com",
                                    "forms.gle",
                                    "accounts.google.com",
                                    "www.google.com",
                                    "google.com"
                                )
                                val allowed = allowedHosts.any { host.endsWith(it) }
                                return !allowed   // true = block navigation (open externally); false = allow
                            }
                        }

                        webView = this
                        loadUrl(initialUrl)
                    }
                }
            )

            // Bottom bar: Autofill button (accessible without scrolling)
            if (isPageReady && !isOnLoginPage) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(48.dp),
                    onClick = { injectAutofill() }
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Autofill")
                }
            }
        }
    }
}

/** Receives JS results on a background thread; must post to main thread. */
class AutofillBridge(private val onResult: (String) -> Unit) {
    @JavascriptInterface
    fun onAutofillResult(json: String) {
        onResult(json)
    }
}
