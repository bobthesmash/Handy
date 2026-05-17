package cz.handy.wear

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Kostra Wear / companion APK ([F5-T05]) — bez sdíleného wake řetězce zatím.
 * Primární aplikace zůstává `:app`.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = getString(R.string.wear_placeholder_body)
                setPadding(PADDING_DP, PADDING_DP_VERTICAL, PADDING_DP, PADDING_DP_VERTICAL)
            },
        )
    }

    private companion object {
        const val PADDING_DP = 32
        const val PADDING_DP_VERTICAL = 48
    }
}
