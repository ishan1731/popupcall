package com.example.popupcall

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.popupcall.ui.theme.PopupcallTheme

class MainActivity : ComponentActivity() {

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Overlay permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        Toast.makeText(
            this,
            if (granted) "Contacts permissions granted!" else "Contacts permissions denied!",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestOverlayPermission()
        checkAndRequestContactsPermission()

        setContent {
            PopupcallTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermissionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            requestOverlayPermissionLauncher.launch(intent)
        }
    }

    private fun checkAndRequestContactsPermission() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.WRITE_CONTACTS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestContactsPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }
}

@Composable
fun PermissionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Permission Management")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val popupService = Intent(context, PopupService::class.java).apply {
                putExtra("number", "9876543210") // Simulate incoming call
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(popupService)
            } else {
                context.startService(popupService)
            }
        }) {
            Text("Simulate Incoming Call Popup")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    PopupcallTheme {
        PermissionScreen()
    }
}
