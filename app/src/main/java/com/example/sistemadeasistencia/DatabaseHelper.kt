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

    //metodos de consulta y registro de asistencia

    /*busqueda de un docente por su dni activo*/
    fun obtenerDocentePorDni(dni: String): Cursor?{
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT id, nombres, apellidos FROM docentes WHERE dni = ?",
            arrayOf(dni)
        )
    }

    /*registro de entrada del docente en la tabla asistencia*/

    fun registrarEntrada(
        docenteId: Int,
        estado: String = "PRESENTE",
        observacion: String = ""
    ):Boolean{
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val horaActual = SimpleDateFormat("HH-mm-ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put("docente_id", docenteId)
            put("fecha", fechaActual)
            put("hora_entrada", horaActual)
            put("estado", estado)
            put("observacion", observacion)
        }

        val resultado = db.insert("asistencia_docentes", null, values)
        return resultado != -1L

    }
    /*registra la hora de salida buscando el ultimo marcaje de dia*/
    fun registrarSalida(docenteId: Int): Boolean{
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyy-MM-dd", Locale.getDefault()).format(Date())
        val horaActual = SimpleDateFormat("HH-mm-ss", Locale.getDefault()).format(Date())
        val values = ContentValues().apply{
            put("hora_salida",horaActual)
        }

        val filasAfectadas = db.update(
            "asistencia_docentes",
            values,
            "docente_id = ? AND fecha = ? AND hora_salida IS NULL",
            arrayOf(docenteId.toString(), fechaActual)
        )
        return filasAfectadas > 0
    }
}