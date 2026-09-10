package com.example.sistemadeasistencia

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // declaracion de la variable Helper como propiedad de la clase

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //inicializacion de la base de datos y pruebas
        dbHelper = DatabaseHelper(this)
        probarConexionDB()
    }
    //metodo auxiliar para verificar la lectura de datos
    private fun probarConexionDB(){
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM docentes", null)

        if (cursor.moveToFirst()){
            val totalDocentes = cursor.getInt(0)
            Log.d("PRUEBA_DB", "Conexion Exitosa, Cantidad de docentes: $totalDocentes")
        }
        cursor.close()
    }
}