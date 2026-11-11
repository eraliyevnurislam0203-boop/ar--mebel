package uz.mebelar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

/** Katalog. `size` — modelning eng katta o'lchami (metrda), chunki GLB birliklari turlicha. */
data class Furniture(val name: String, val asset: String, val size: Float)

val catalog = listOf(
    Furniture("Divan", "models/sofa_pink.glb", 1.8f),
    Furniture("Stul", "models/chair.glb", 0.9f),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@Composable
fun App() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (granted) {
        ArScreen()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AR uchun kamera ruxsati kerak")
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("Ruxsat berish") }
            }
        }
    }
}

@Composable
fun ArScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)

    var selected by remember { mutableStateOf(catalog.first()) }
    var showPlanes by remember { mutableStateOf(true) }
    var frame by remember { mutableStateOf<Frame?>(null) }
    var tracking by remember { mutableStateOf<TrackingFailureReason?>(null) }

    Box(Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            sessionConfiguration = { session, config ->
                config.depthMode =
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                        Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            cameraNode = cameraNode,
            planeRenderer = showPlanes,
            onTrackingFailureChanged = { tracking = it },
            onSessionUpdated = { _, updated -> frame = updated },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, node ->
                    if (node == null) {
                        val hit = frame?.hitTest(motionEvent.x, motionEvent.y)
                            ?.firstOrNull { it.isValid(depthPoint = false, point = false) }
                        hit?.createAnchorOrNull()?.let { anchor ->
                            val item = selected
                            val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                            val modelNode = ModelNode(
                                modelInstance = modelLoader.createModelInstance(item.asset),
                                scaleToUnits = item.size,
                                // modelning tagi (pastki qismi) yerga tegib tursin
                                centerOrigin = Position(x = 0f, y = -1f, z = 0f)
                            ).apply { isEditable = true } // siljitish, burish, kattalashtirish
                            anchorNode.addChildNode(modelNode)
                            childNodes += anchorNode
                        }
                    }
                }
            )
        )

        // Yuqori ko'rsatma
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Text(
                text = when {
                    tracking != null -> "Telefonni sekin aylantiring, pol/devor yaxshi ko'rinsin…"
                    childNodes.isEmpty() -> "Tekis joyga bosing — mebel qo'yiladi"
                    else -> "Mebelni suring, ikki barmoq bilan buring/kattalashtiring"
                },
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // Pastki panel: katalog + boshqaruv
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                catalog.forEach { item ->
                    val isSel = item == selected
                    if (isSel) {
                        Button(onClick = { selected = item }, modifier = Modifier.weight(1f)) {
                            Text(item.name)
                        }
                    } else {
                        FilledTonalButton(onClick = { selected = item }, modifier = Modifier.weight(1f)) {
                            Text(item.name)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        childNodes.lastOrNull()?.let { childNodes.remove(it); it.destroy() }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Orqaga", color = Color.White) }
                OutlinedButton(
                    onClick = {
                        childNodes.toList().forEach { it.destroy() }
                        childNodes.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Tozalash", color = Color.White) }
                OutlinedButton(
                    onClick = { showPlanes = !showPlanes },
                    modifier = Modifier.weight(1f)
                ) { Text(if (showPlanes) "To'rni yashir" else "To'rni ko'rsat", color = Color.White) }
            }
        }
    }
}
