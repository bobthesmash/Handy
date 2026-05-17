package cz.handy.feature.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cz.handy.core.persistence.OnboardingPreferences
import cz.handy.feature.ui.pipeline.HandyAssistantViewModel
import cz.handy.feature.ui.theme.HandyTheme

fun ComponentActivity.setHandyContent(showCommandPipelineUi: Boolean) {
    enableEdgeToEdge()
    setContent {
        HandyTheme {
            val activity = this@setHandyContent
            val app = application as Application
            val onboardingPrefs = remember { OnboardingPreferences(activity) }
            var wizardComplete by rememberSaveable {
                mutableStateOf(onboardingPrefs.isPermissionsWizardComplete())
            }

            if (!wizardComplete) {
                PermissionsOnboardingScreen(
                    onComplete = {
                        onboardingPrefs.setPermissionsWizardComplete(true)
                        wizardComplete = true
                    },
                )
            } else {
                val factory =
                    remember(app, showCommandPipelineUi) {
                        HandyAssistantViewModel.Factory(
                            application = app,
                            simulateVoicePipelineBypass = showCommandPipelineUi,
                        )
                    }
                var bootstrapError by remember { mutableStateOf<Throwable?>(null) }
                val assistant =
                    remember(factory) {
                        runCatching {
                            factory.create(HandyAssistantViewModel::class.java)
                        }.onFailure { bootstrapError = it }.getOrNull()
                    }

                if (assistant == null) {
                    HandyBootstrapErrorScreen(
                        error = bootstrapError,
                        onResetOnboarding = {
                            onboardingPrefs.setPermissionsWizardComplete(false)
                            wizardComplete = false
                        },
                    )
                } else {
                    HandyRootScreen(
                        assistant = assistant,
                        showCommandPipelineUi = showCommandPipelineUi,
                    )
                }
            }
        }
    }
}
