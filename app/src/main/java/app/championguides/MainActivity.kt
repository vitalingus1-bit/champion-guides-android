package app.championguides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.championguides.data.catalog.AssetCatalogRepository
import app.championguides.ui.GuidesScreen
import app.championguides.ui.GuidesViewModel

private val guidesColors = darkColorScheme(
    primary = Color(0xFFF3BB86),
    onPrimary = Color(0xFF382000),
    primaryContainer = Color(0xFF52371F),
    onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFA9D5C8),
    background = Color(0xFF10151E),
    surface = Color(0xFF151D28),
    surfaceContainer = Color(0xFF1B2633),
    surfaceContainerHigh = Color(0xFF232F3D),
    onSurface = Color(0xFFF0F3FA),
    onSurfaceVariant = Color(0xFFBFC9D6),
    outline = Color(0xFF798A9C),
)

class MainActivity : ComponentActivity() {
    private val guides by viewModels<GuidesViewModel> {
        viewModelFactory {
            initializer {
                val app = application as GuidesApplication
                GuidesViewModel(
                    catalogRepository = AssetCatalogRepository(app.assets),
                    favorites = app.favorites,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(0xFF10151E.toInt()),
        )
        setContent {
            MaterialTheme(colorScheme = guidesColors) {
                GuidesScreen(guides)
            }
        }
    }
}
