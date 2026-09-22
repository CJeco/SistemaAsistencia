package com.example.sistemadeasistencia

import android.R
import android.database.Cursor
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AsistenciaAdapter(private val cursor: Cursor) : RecyclerView.Adapter<AsistenciaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInfo: TextView = view.findViewById(R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (cursor.moveToPosition(position)) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombres"))
            val apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellidos"))
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"))
            val entrada = cursor.getString(cursor.getColumnIndexOrThrow("hora_entrada"))
            val salida = cursor.getString(cursor.getColumnIndexOrThrow("hora_salida")) ?: "--:--:--"

            holder.tvInfo.text = "$fecha | $nombre $apellido\nEntrada: $entrada | Salida: $salida"
            holder.tvInfo.setTextColor(Color.WHITE)
        }
    }

    override fun getItemCount(): Int = cursor.count
}
