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
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.handy.core.persistence.OnboardingPreferences
import cz.handy.feature.ui.pipeline.HandyAssistantViewModel
import cz.handy.feature.ui.theme.HandyTheme

fun ComponentActivity.setHandyContent(showCommandPipelineUi: Boolean) {
    enableEdgeToEdge()
    setContent {
        HandyTheme {
            val activity = this@setHandyContent
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
                val assistant: HandyAssistantViewModel =
                    viewModel(
                        factory =
                            HandyAssistantViewModel.Factory(
                                application = application as Application,
                                simulateVoicePipelineBypass = showCommandPipelineUi,
                            ),
                    )
                HandyRootScreen(
                    assistant = assistant,
                    showCommandPipelineUi = showCommandPipelineUi,
                )
            }
        }
    }
}
