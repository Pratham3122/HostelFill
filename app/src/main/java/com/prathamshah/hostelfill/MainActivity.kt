package com.prathamshah.hostelfill

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prathamshah.hostelfill.ui.HostelFillTheme
import com.prathamshah.hostelfill.ui.screens.FormWebViewScreen
import com.prathamshah.hostelfill.ui.screens.MainScreen
import com.prathamshah.hostelfill.ui.screens.ProfileSetupScreen
import com.prathamshah.hostelfill.viewmodel.MainViewModel

object Nav {
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_EDIT  = "profile_edit"
    const val MAIN          = "main"
    const val FORM          = "form"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle URL shared via Android Sharesheet
        handleSharedIntent(intent)

        setContent {
            HostelFillTheme {
                val navController   = rememberNavController()
                val isProfileSaved  by viewModel.isProfileSaved.collectAsState()
                val validatedUrl    by viewModel.validatedUrl.collectAsState()
                var formAutoSubmit  by remember { mutableStateOf(false) }

                // Determine start destination once profile state is known
                val startDest = remember(isProfileSaved) {
                    if (isProfileSaved) Nav.MAIN else Nav.PROFILE_SETUP
                }

                NavHost(navController = navController, startDestination = startDest) {

                    composable(Nav.PROFILE_SETUP) {
                        ProfileSetupScreen(
                            viewModel  = viewModel,
                            isEditing  = false,
                            onComplete = {
                                navController.navigate(Nav.MAIN) {
                                    popUpTo(Nav.PROFILE_SETUP) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Nav.PROFILE_EDIT) {
                        ProfileSetupScreen(
                            viewModel  = viewModel,
                            isEditing  = true,
                            onComplete = { navController.popBackStack() }
                        )
                    }

                    composable(Nav.MAIN) {
                        MainScreen(
                            viewModel     = viewModel,
                            onEditProfile = { navController.navigate(Nav.PROFILE_EDIT) },
                            onOpenForm    = { url, autoSubmit ->
                                formAutoSubmit = autoSubmit
                                navController.navigate(Nav.FORM)
                            }
                        )
                    }

                    composable(Nav.FORM) {
                        val url = validatedUrl
                        if (url == null) {
                            navController.popBackStack()
                        } else {
                            FormWebViewScreen(
                                initialUrl  = url,
                                autoSubmit  = formAutoSubmit,
                                viewModel   = viewModel,
                                onBack      = {
                                    viewModel.clearValidatedUrl()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Re-handle share intents when activity is already running. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleSharedIntent(it) }
    }

    /**
     * Extract a URL from an Android Sharesheet SEND intent.
     * The user can share a Google Forms link directly from WhatsApp
     * or any browser into HostelFill.
     */
    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            // Extract the first URL-like token from the shared text
            val urlRegex = Regex("https?://\\S+")
            val url = urlRegex.find(sharedText)?.value ?: sharedText.trim()
            if (url.isNotBlank()) {
                viewModel.setFormUrl(url)
            }
        }
    }
}
