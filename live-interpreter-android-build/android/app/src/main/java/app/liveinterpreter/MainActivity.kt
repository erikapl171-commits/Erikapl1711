package app.liveinterpreter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.liveinterpreter.ui.InterpreterScreen
import app.liveinterpreter.ui.InterpreterViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: InterpreterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            model.state.collect { state ->
                if (state.keepScreenOn && state.phase == app.liveinterpreter.core.DemoPhase.RUNNING) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
        setContent { InterpreterScreen(model) }
    }

    // Demo stops on background. Foreground microphone service continues independently.
    override fun onStop() {
        model.stopDemo()
        super.onStop()
    }
}
