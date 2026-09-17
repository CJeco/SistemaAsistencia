package com.example.sistemadeasistencia

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DB_name, null, DB_VERSION){

    companion object{
        private const val DB_name = "asistencia_docentes.db"
        private const val DB_VERSION = 1
    }

    private val dbPath: String = context.getDatabasePath(DB_name).path

    init {
        if (!checkDatabase()){
            this.readableDatabase.close()
            copyDatabase()
        }
    }
    private fun checkDatabase(): Boolean{
        val dbFile = context.getDatabasePath(DB_name)
        return dbFile.exists()
    }

    private fun copyDatabase(){
        val inputStream: InputStream = context.assets.open("databases/$DB_name")
        val outputStream: OutputStream = FileOutputStream(dbPath)

        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0){
            outputStream.write(buffer, 0, length)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()

    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //para migracion futura
    }


    fun registrarDocente(dni: String, nombres: String, apellidos: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("dni", dni)
            put("nombres", nombres)
            put("apellidos", apellidos)
        }
        val resultado = db.insert("docentes", null, values)
        return resultado != -1L
    }


    /*busqueda de un docente por su dni activo*/
    fun obtenerDocentePorDni(dni: String): Cursor?{
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, nombres, apellidos FROM docentes WHERE dni = ?",
            arrayOf(dni)
        )
    }


    fun registrarEntrada(docenteId: Int): Boolean{
        val db = this.writableDatabase
        val sdFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fechaActual = sdFecha.format(Date())
        val horaActual = sdHora.format(Date())

        val values = ContentValues().apply {
            put("docente_id", docenteId)
            put("fecha", fechaActual)
            put("hora_entrada", horaActual)
        }

        val resultado = db.insert("asistencia_docentes", null, values)
        return resultado != -1L
    }



    fun registrarSalida(docenteId: Int): Boolean{
        val db = this.writableDatabase
        val sdFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fechaActual = sdFecha.format(Date())
        val horaActual = sdHora.format(Date())

        val values = ContentValues().apply {
            put("hora_salida", horaActual)
        }

        val filasAfectadas = db.update(
            "asistencia_docentes",
            values,
            "docente_id = ? AND hora_salida IS NULL",
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
