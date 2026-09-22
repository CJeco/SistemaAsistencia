package com.example.sistemadeasistencia

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "asistencia_docentes.db"
        private const val DB_VERSION = 1
    }

    private val dbFile: File = context.getDatabasePath(DB_NAME)

    init {
        copiarBaseDeDatosSiEsNecesario()
    }

    private fun copiarBaseDeDatosSiEsNecesario() {
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            try {
                val inputStream: InputStream = context.assets.open("databases/$DB_NAME")
                val outputStream: OutputStream = FileOutputStream(dbFile)

                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE IF NOT EXISTS docentes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dni TEXT NOT NULL UNIQUE,
                nombres TEXT NOT NULL,
                apellidos TEXT NOT NULL
            );
        """.trimIndent())

        db?.execSQL("""
            CREATE TABLE IF NOT EXISTS asistencia_docentes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                docente_id INTEGER NOT NULL,
                fecha TEXT NOT NULL,
                hora_entrada TEXT NOT NULL,
                hora_salida TEXT,
                FOREIGN KEY(docente_id) REFERENCES docentes(id) ON DELETE CASCADE
            );
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // para migración futura
    }

    fun docenteExiste(dni: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM docentes WHERE TRIM(dni) = ?",
            arrayOf(dni.trim())
        )
        val existe = cursor != null && cursor.moveToFirst()
        cursor?.close()
        return existe
    }

    fun registrarDocente(dni: String, nombres: String, apellidos: String): Boolean {
        val dniLimpio = dni.trim()
        if (docenteExiste(dniLimpio)) {
            return false // Ya existe un docente con este DNI
        }

        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("dni", dniLimpio)
            put("nombres", nombres.trim())
            put("apellidos", apellidos.trim())
        }
        val resultado = db.insert("docentes", null, values)
        return resultado != -1L
    }

    /* búsqueda de un docente por su dni */
    fun obtenerDocentePorDni(dni: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, nombres, apellidos FROM docentes WHERE TRIM(dni) = ?",
            arrayOf(dni.trim())
        )
    }

    /* obtener la asistencia del día actual para un docente */
    fun obtenerAsistenciaHoy(docenteId: Int, fecha: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, hora_entrada, hora_salida FROM asistencia_docentes WHERE docente_id = ? AND fecha = ? ORDER BY id DESC LIMIT 1",
            arrayOf(docenteId.toString(), fecha)
        )
    }

    fun registrarEntrada(docenteId: Int): Boolean {
        val db = this.writableDatabase
        val sdFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val ahora = Date()
        val fechaActual = sdFecha.format(ahora)
        val horaActual = sdHora.format(ahora)

        val values = ContentValues().apply {
            put("docente_id", docenteId)
            put("fecha", fechaActual)
            put("hora_entrada", horaActual)
        }

        val resultado = db.insert("asistencia_docentes", null, values)
        return resultado != -1L
    }

    fun registrarSalida(docenteId: Int): Boolean {
        val db = this.writableDatabase
        val sdFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val ahora = Date()
        val fechaActual = sdFecha.format(ahora)
        val horaActual = sdHora.format(ahora)

        val values = ContentValues().apply {
            put("hora_salida", horaActual)
        }

        val filasAfectadas = db.update(
            "asistencia_docentes",
            values,
            "docente_id = ? AND fecha = ? AND hora_salida IS NULL",
            arrayOf(docenteId.toString(), fechaActual)
        )
        return filasAfectadas > 0
    }

    fun obtenerReporteAsistencia(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery(
            """
            SELECT d.nombres, d.apellidos, a.fecha, a.hora_entrada, a.hora_salida 
            FROM asistencia_docentes a 
            INNER JOIN docentes d ON a.docente_id = d.id 
            ORDER BY a.fecha DESC, a.hora_entrada DESC
            """.trimIndent(), null
        )
    }
}
