package com.example.testing.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChangeEmailScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    var newEmail by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {

        Text("Change Email", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            label = { Text("New Email") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(error!!, color = Color.Red, fontSize = 12.sp)
        }

        if (success) {
            Text("Email updated!", color = Color(0xFF4CAF50))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                loading = true
                error = null
                auth.currentUser?.updateEmail(newEmail)
                    ?.addOnSuccessListener {
                        loading = false
                        success = true
                    }
                    ?.addOnFailureListener { e ->
                        loading = false
                        error = e.message
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC))
        ) {
            if (loading)
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            else
                Text("Save", color = Color.White)
        }
    }
}
