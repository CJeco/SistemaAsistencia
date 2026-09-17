package com.example.sistemadeasistencia

import android.graphics.Color
import android.graphics.Typeface
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
import androidx.cardview.widget.CardView
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
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_registro_docente, null)

        val etDni = dialogView.findViewById<EditText>(R.id.etDniDialog)
        val etNombres = dialogView.findViewById<EditText>(R.id.etNombresDialog)
        val etApellidos = dialogView.findViewById<EditText>(R.id.etApellidosDialog)

        builder.setView(dialogView)
        builder.setPositiveButton("Guardar y Generar QR") { _, _ ->
            val dni = etDni.text.toString().trim()
            val txtNombres = etNombres.text.toString().trim()
            val txtApellidos = etApellidos.text.toString().trim()

            if (dni.isNotEmpty() && txtNombres.isNotEmpty() && txtApellidos.isNotEmpty()) {
                val exito = dbHelper.registrarDocente(dni, txtNombres, txtApellidos)
                if (exito) {
                    registrarYMostrarQR(dni, txtNombres, txtApellidos)
                } else {
                    Toast.makeText(this, "El DNI ya está registrado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
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
        builder.setTitle("Historial de Asistencias")

        val scrollView = ScrollView(this)
        val contenedorPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }

        if (cursor != null && cursor.moveToFirst()) {
            val nombresIndex = cursor.getColumnIndex("nombres")
            val apellidosIndex = cursor.getColumnIndex("apellidos")
            val fechaIndex = cursor.getColumnIndex("fecha")
            val entradaIndex = cursor.getColumnIndex("hora_entrada")
            val salidaIndex = cursor.getColumnIndex("hora_salida")

            do {
                val nombre = if (nombresIndex != -1) cursor.getString(nombresIndex) else ""
                val apellido = if (apellidosIndex != -1) cursor.getString(apellidosIndex) else ""
                val fecha = if (fechaIndex != -1) cursor.getString(fechaIndex) else ""
                val entrada = if (entradaIndex != -1) cursor.getString(entradaIndex) else "--:--"
                val salida = if (salidaIndex != -1 && !cursor.isNull(salidaIndex)) cursor.getString(salidaIndex) else "En curso"

                val itemView = layoutInflater.inflate(R.layout.item_reporte_asistencia, contenedorPrincipal, false)
                val tvNombreReporte = itemView.findViewById<TextView>(R.id.tvNombreReporte)
                val tvDetalleReporte = itemView.findViewById<TextView>(R.id.tvDetalleReporte)

                tvNombreReporte.text = "$nombre $apellido"
                tvDetalleReporte.text = "📅 Fecha: $fecha\n🟢 Entrada: $entrada  |  🔴 Salida: $salida"

                contenedorPrincipal.addView(itemView)
            } while (cursor.moveToNext())
            cursor.close()
        } else {
            val tvVacio = TextView(this).apply {
                text = "No hay registros de asistencia aún."
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            contenedorPrincipal.addView(tvVacio)
        }

        scrollView.addView(contenedorPrincipal)
        builder.setView(scrollView)
        builder.setPositiveButton("Cerrar", null)
        builder.show()
    }
}