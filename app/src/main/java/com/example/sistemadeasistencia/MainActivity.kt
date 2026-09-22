package com.example.sistemadeasistencia

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvResultado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        probarConexionDB()

        val btnEscanear = findViewById<Button>(R.id.btnEscanear)
        val btnAdmin = findViewById<ImageButton>(R.id.btnReportes) // La tuerquita
        tvResultado = findViewById(R.id.tvResultado)

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13
            )
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        btnEscanear.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val codigo = barcode.rawValue
                    if (!codigo.isNullOrEmpty()) {
                        procesarAsistenciaDocente(codigo.trim())
                    }
                }
                .addOnCanceledListener {
                    Toast.makeText(this, "Escaneo Cancelado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(this, "Error de escaneo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Navegación al apartado de administración (reemplaza el diálogo flotante)
        btnAdmin.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun probarConexionDB() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM docentes", null)

        if (cursor.moveToFirst()) {
            val totalDocentes = cursor.getInt(0)
            Log.d("PRUEBA_DB", "Conexión Exitosa, Cantidad de docentes: $totalDocentes")
        }
        cursor.close()
    }

    private fun procesarAsistenciaDocente(dniOcodigo: String) {
        val cursor = dbHelper.obtenerDocentePorDni(dniOcodigo)

        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex("id")
            val nombresIndex = cursor.getColumnIndex("nombres")
            val apellidosIndex = cursor.getColumnIndex("apellidos")

            if (idIndex != -1 && nombresIndex != -1 && apellidosIndex != -1) {
                val docenteId = cursor.getInt(idIndex)
                val nombreCompleto = "${cursor.getString(nombresIndex)} ${cursor.getString(apellidosIndex)}"

                val exitoEntrada = dbHelper.registrarEntrada(docenteId)

                if (exitoEntrada) {
                    tvResultado.text = "Último marcaje:\n$nombreCompleto"
                    mostrarConfirmacionExito(nombreCompleto, "Entrada")
                } else {
                    val exitoSalida = dbHelper.registrarSalida(docenteId)
                    if (exitoSalida) {
                        tvResultado.text = "Último marcaje:\n$nombreCompleto"
                        mostrarConfirmacionExito(nombreCompleto, "Salida")
                    } else {
                        Toast.makeText(this, "El docente ya registró entrada y salida hoy", Toast.LENGTH_LONG).show()
                    }
                }
            }
            cursor.close()
        } else {
            tvResultado.text = "No encontrado: $dniOcodigo"
            Toast.makeText(this, "Docente no registrado en la BD", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarConfirmacionExito(nombre: String, tipo: String) {
        val mensajeView = TextView(this).apply {
            text = "✔\n\n¡$tipo Registrada!\n$nombre"
            textSize = 20f
            setTextColor(Color.parseColor("#2E7D32"))
            gravity = Gravity.CENTER
            setPadding(40, 60, 40, 60)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(mensajeView)
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing && !isFinishing) {
                dialog.dismiss()
            }
        }, 2000)
    }
}
