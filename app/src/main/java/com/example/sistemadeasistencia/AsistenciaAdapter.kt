package com.example.sistemadeasistencia

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AsistenciaAdapter(private val cursor: Cursor) : RecyclerView.Adapter<AsistenciaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreReporte)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleReporte)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_asistencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (cursor.moveToPosition(position)) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombres"))
            val apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellidos"))
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"))
            val entrada = cursor.getString(cursor.getColumnIndexOrThrow("hora_entrada"))
            val salida = cursor.getString(cursor.getColumnIndexOrThrow("hora_salida")) ?: "En curso..."

            holder.tvNombre.text = "$nombre $apellido"
            holder.tvDetalle.text = "📅 Fecha: $fecha\n🟢 Entrada: $entrada  |  🔴 Salida: $salida"
        }
    }

    override fun getItemCount(): Int = cursor.count
}
