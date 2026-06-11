package com.uth.inventariopulperia.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.uth.inventariopulperia.R
import com.uth.inventariopulperia.models.MovementDetail

class MovementAdapter(
    private val context: Context,
    private var movementList: ArrayList<MovementDetail>
) : BaseAdapter() {

    override fun getCount(): Int {
        return movementList.size
    }

    override fun getItem(position: Int): Any {
        return movementList[position]
    }

    override fun getItemId(position: Int): Long {
        return movementList[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_movement, parent, false)

        val movement = movementList[position]

        val txtProducto = view.findViewById<TextView>(R.id.txtProductoMovimientoItem)
        val txtTipoCantidad = view.findViewById<TextView>(R.id.txtTipoCantidadMovimientoItem)
        val txtFecha = view.findViewById<TextView>(R.id.txtFechaMovimientoItem)
        val txtNota = view.findViewById<TextView>(R.id.txtNotaMovimientoItem)

        txtProducto.text = movement.productName
        val tipoTexto = when (movement.type) {
            "ENTRADA" -> "ENTRADA"
            "SALIDA" -> "SALIDA"
            "SALDO_INICIAL" -> "SALDO INICIAL"
            else -> movement.type
        }

        txtTipoCantidad.text = "$tipoTexto: ${movement.qty} unidades"

        txtFecha.text = "Fecha: ${movement.date}"

        txtNota.text = if (movement.notes.isNotEmpty()) {
            "Nota: ${movement.notes}"
        } else {
            "Nota: Sin nota"
        }

        when (movement.type) {
            "ENTRADA" -> {
                txtTipoCantidad.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
            }

            "SALIDA" -> {
                txtTipoCantidad.setTextColor(ContextCompat.getColor(context, R.color.warning_color))
            }

            "SALDO_INICIAL" -> {
                txtTipoCantidad.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            else -> {
                txtTipoCantidad.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        return view
    }

    fun updateList(newList: ArrayList<MovementDetail>) {
        movementList = newList
        notifyDataSetChanged()
    }
}