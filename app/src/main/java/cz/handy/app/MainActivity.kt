package cz.handy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import cz.handy.app.BuildConfig
import cz.handy.feature.ui.setHandyContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHandyContent(showCommandPipelineUi = BuildConfig.DEBUG)
    }
}
