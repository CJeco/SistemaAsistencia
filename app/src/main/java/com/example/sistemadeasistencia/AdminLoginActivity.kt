package com.example.sistemadeasistencia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AdminLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_login)


        val etPin = findViewById<EditText>(R.id.etPin)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)

        btnIngresar.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin == "1234") {
                val intent = Intent(this, AdminPanelActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "PIN Incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }
}
