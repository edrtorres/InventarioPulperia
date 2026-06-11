package com.uth.inventariopulperia

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.uth.inventariopulperia.adapters.ProductAdapter
import com.uth.inventariopulperia.database.InventoryDbHelper
import com.uth.inventariopulperia.models.Product


class AlertsActivity : AppCompatActivity() {

    private lateinit var btnRegresarAlertas: TextView
    private lateinit var txtCantidadAlertas: TextView
    private lateinit var txtMensajeAlertas: TextView
    private lateinit var txtTituloListaAlertas: TextView
    private lateinit var listViewAlertas: ListView

    private lateinit var dbHelper: InventoryDbHelper
    private lateinit var alertasAdapter: ProductAdapter

    private var lowStockList = ArrayList<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        btnRegresarAlertas = findViewById(R.id.btnRegresarAlertas)
        txtCantidadAlertas = findViewById(R.id.txtCantidadAlertas)
        txtMensajeAlertas = findViewById(R.id.txtMensajeAlertas)
        txtTituloListaAlertas = findViewById(R.id.txtTituloListaAlertas)
        listViewAlertas = findViewById(R.id.listViewAlertas)

        dbHelper = InventoryDbHelper(this)

        alertasAdapter = ProductAdapter(
            this,
            lowStockList,
            onProductClick = { product ->
                mostrarDialogoIrAMovimientos(product)
            },
            onProductLongClick = {},
            onSelectionChanged = {}
        )

        listViewAlertas.adapter = alertasAdapter

        btnRegresarAlertas.setOnClickListener {
            finish()
        }

        cargarAlertas()
    }

    override fun onResume() {
        super.onResume()
        cargarAlertas()
    }

    private fun cargarAlertas() {
        lowStockList = dbHelper.getLowStockProducts()
        alertasAdapter.updateList(lowStockList)

        txtCantidadAlertas.text = lowStockList.size.toString()

        if (lowStockList.isEmpty()) {
            txtMensajeAlertas.text = "No hay productos con stock mínimo alcanzado"
            txtTituloListaAlertas.text = "Productos en alerta de stock"
        } else {
            txtMensajeAlertas.text = "Hay productos que requieren revisión"
            txtTituloListaAlertas.text = "Productos en alerta de stock: ${lowStockList.size}"
        }
    }

    private fun mostrarDialogoIrAMovimientos(product: Product) {
        val vistaDialogo = layoutInflater.inflate(R.layout.dialog_alert_to_movement, null)

        val dialogo = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        val txtProducto = vistaDialogo.findViewById<TextView>(R.id.txtDialogProductoAlerta)
        val txtMensaje = vistaDialogo.findViewById<TextView>(R.id.txtDialogMensajeAlerta)
        val txtStockActual = vistaDialogo.findViewById<TextView>(R.id.txtDialogStockActual)
        val txtStockMinimo = vistaDialogo.findViewById<TextView>(R.id.txtDialogStockMinimo)

        val btnCancelar = vistaDialogo.findViewById<TextView>(R.id.btnDialogCancelarMovimiento)
        val btnIr = vistaDialogo.findViewById<TextView>(R.id.btnDialogIrMovimiento)

        txtProducto.text = product.name
        txtMensaje.text = "Este producto alcanzó el stock mínimo y requiere revisión."
        txtStockActual.text = "Stock actual: ${product.stock}"
        txtStockMinimo.text = "Stock mínimo: ${product.minStock}"

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        btnIr.setOnClickListener {
            dialogo.dismiss()

            val intent = Intent(this, MovementsActivity::class.java)
            intent.putExtra("PRODUCT_ID", product.id)
            startActivity(intent)
        }

        dialogo.show()
        dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}