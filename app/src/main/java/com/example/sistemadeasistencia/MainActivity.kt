package com.example.sistemadeasistencia

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
        val btnReportes = findViewById<ImageButton>(R.id.btnReportes)
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

        btnReportes.setOnClickListener {
            solicitarPinSeguridad()
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

    private fun solicitarPinSeguridad() {
        val inputPIN = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN (Ejemplo: 1234)"
        }

        AlertDialog.Builder(this)
            .setTitle("Acceso Administrador")
            .setMessage("Ingrese el PIN de seguridad:")
            .setView(inputPIN)
            .setPositiveButton("Ingresar") { _, _ ->
                val pin = inputPIN.text.toString()
                if (pin == "1234") {
                    mostrarOpcionesAdmin()
                } else {
                    Toast.makeText(this, "PIN Incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarOpcionesAdmin() {
        val opciones = arrayOf("Registrar Nuevo Docente", "Ver Reportes")
        AlertDialog.Builder(this)
            .setTitle("Panel de Administración")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarFormularioRegistroDocente()
                    1 -> mostrarReportes()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarFormularioRegistroDocente() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val inputDni = EditText(this).apply { hint = "DNI / Código" }
        val inputNombres = EditText(this).apply { hint = "Nombres" }
        val inputApellidos = EditText(this).apply { hint = "Apellidos" }

        layout.addView(inputDni)
        layout.addView(inputNombres)
        layout.addView(inputApellidos)

        AlertDialog.Builder(this)
            .setTitle("Nuevo Docente")
            .setView(layout)
            .setPositiveButton("Guardar y Generar QR") { _, _ ->
                val dni = inputDni.text.toString().trim()
                val nombres = inputNombres.text.toString().trim()
                val apellidos = inputApellidos.text.toString().trim()

                if (dni.isNotEmpty() && nombres.isNotEmpty() && apellidos.isNotEmpty()) {
                    registrarYMostrarQR(dni, nombres, apellidos)
                } else {
                    Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarYMostrarQR(dni: String, nombres: String, apellidos: String) {
        val exito = dbHelper.registrarDocente(dni, nombres, apellidos)

        if (exito) {
            val bitmapQR = QRUtils.generarCodigoQR(dni)
            if (bitmapQR != null) {
                val imgView = ImageView(this).apply {
                    setImageBitmap(bitmapQR)
                    setPadding(40, 40, 40, 40)
                }

                AlertDialog.Builder(this)
                    .setTitle("Código QR de $nombres $apellidos")
                    .setMessage("DNI / Código: $dni")
                    .setView(imgView)
                    .setPositiveButton("Descargar / Guardar") { _, _ ->
                        val guardado = QRUtils.guardarImagenEnGaleria(this, bitmapQR, "$dni-$nombres")
                        if (guardado) {
                            Toast.makeText(this, "QR guardado en la Galería", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()

                Toast.makeText(this, "Docente registrado con éxito", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error: El DNI ya está registrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarReportes() {
        val cursor = dbHelper.obtenerReporteAsistencia()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reporte de Asistencia")

        val reportLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val nombreIndex = cursor.getColumnIndex("nombres")
                val apellidoIndex = cursor.getColumnIndex("apellidos")
                val fechaIndex = cursor.getColumnIndex("fecha")
                val entradaIndex = cursor.getColumnIndex("hora_entrada")
                val salidaIndex = cursor.getColumnIndex("hora_salida")

                if (nombreIndex != -1 && apellidoIndex != -1 && fechaIndex != -1 && entradaIndex != -1 && salidaIndex != -1) {
                    val nombre = cursor.getString(nombreIndex)
                    val apellido = cursor.getString(apellidoIndex)
                    val fecha = cursor.getString(fechaIndex)
                    val entrada = cursor.getString(entradaIndex)
                    val salida = cursor.getString(salidaIndex) ?: "--:--:--"

                    val itemText = TextView(this).apply {
                        text = "$fecha | $nombre $apellido\nEntrada: $entrada | Salida: $salida\n-----------------------------------"
                        textSize = 14f
                        setPadding(0, 10, 0, 10)
                    }
                    reportLayout.addView(itemText)
                }
            } while (cursor.moveToNext())
            cursor.close()
        } else {
            val emptyText = TextView(this).apply {
                text = "No hay registros de asistencia."
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            reportLayout.addView(emptyText)
        }

        val scrollView = ScrollView(this).apply {
            addView(reportLayout)
        }

        builder.setView(scrollView)
        builder.setPositiveButton("Cerrar", null)
        builder.show()
    }
}
