package com.sukajee.docscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.sukajee.docscanner.ui.theme.DocScannerTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_PDF, RESULT_FORMAT_JPEG)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocScannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var imageUris by remember {
                            mutableStateOf<List<Uri>>(emptyList())
                        }

                        val scannerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult(),
                            onResult = {
                                when (it.resultCode) {
                                    RESULT_OK -> {
                                        val result =
                                            GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                                        imageUris = result?.pages?.map { page ->
                                            page.imageUri
                                        } ?: emptyList()


                                        result?.pdf?.uri?.path?.let { path ->
                                            val externalUri = FileProvider.getUriForFile(
                                                this@MainActivity,
                                                "$packageName.provider",
                                                File(path)
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_STREAM, externalUri)
                                                type = "application/pdf"
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    "share pdf"
                                                )
                                            )
                                        }
                                    }
                                    RESULT_CANCELED -> {
                                        Toast.makeText(
                                            this@MainActivity.baseContext,
                                            "Cancelled",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    else -> {
                                        Toast.makeText(
                                            this@MainActivity.baseContext,
                                            "There was some error, Please try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(imageUris) {
                                AsyncImage(
                                    model = it,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            input = IntentSenderRequest.Builder(it).build()
                                        )
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this@MainActivity.baseContext,
                                            it.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        ) {
                            Text(text = "Scan Docs")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DocScannerTheme {
        Greeting("Android")
    }
}