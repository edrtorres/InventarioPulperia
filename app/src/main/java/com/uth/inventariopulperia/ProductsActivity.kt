package com.uth.inventariopulperia

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.uth.inventariopulperia.adapters.ProductAdapter
import com.uth.inventariopulperia.database.InventoryDbHelper
import com.uth.inventariopulperia.models.Product

class ProductsActivity : AppCompatActivity() {

    private lateinit var btnAgregarProducto: TextView
    private lateinit var txtCantidadProductos: TextView

    private lateinit var panelSeleccionSuperior: LinearLayout
    private lateinit var txtModoSeleccion: TextView
    private lateinit var btnSeleccionarTodosProductos: TextView

    private lateinit var listViewProductos: ListView
    private lateinit var panelEliminar: LinearLayout
    private lateinit var btnCancelarSeleccion: TextView
    private lateinit var btnEliminarSeleccionados: TextView
    private lateinit var btnRegresarProductos: TextView

    private lateinit var dbHelper: InventoryDbHelper
    private lateinit var productAdapter: ProductAdapter
    private var productList = ArrayList<Product>()

    private var modoSeleccionActivo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        btnAgregarProducto = findViewById(R.id.btnAgregarProducto)
        txtCantidadProductos = findViewById(R.id.txtCantidadProductos)

        panelSeleccionSuperior = findViewById(R.id.panelSeleccionSuperior)
        txtModoSeleccion = findViewById(R.id.txtModoSeleccion)
        btnSeleccionarTodosProductos = findViewById(R.id.btnSeleccionarTodosProductos)

        listViewProductos = findViewById(R.id.listViewProductos)
        panelEliminar = findViewById(R.id.panelEliminar)
        btnCancelarSeleccion = findViewById(R.id.btnCancelarSeleccion)
        btnEliminarSeleccionados = findViewById(R.id.btnEliminarSeleccionados)
        btnRegresarProductos = findViewById(R.id.btnRegresarProductos)

        dbHelper = InventoryDbHelper(this)

        productAdapter = ProductAdapter(
            this,
            productList,
            onProductClick = { product ->
                if (modoSeleccionActivo) {
                    productAdapter.toggleSelection(product.id)
                } else {
                    val intent = Intent(this, ProductFormActivity::class.java)
                    intent.putExtra("PRODUCT_ID", product.id)
                    startActivity(intent)
                }
            },
            onProductLongClick = { product ->
                if (!modoSeleccionActivo) {
                    activarModoSeleccion()
                }

                productAdapter.toggleSelection(product.id)

                Toast.makeText(this, "Modo selección activado", Toast.LENGTH_SHORT).show()
            },
            onSelectionChanged = {
                actualizarTextoBotonSeleccion()
            }
        )

        listViewProductos.adapter = productAdapter

        btnAgregarProducto.setOnClickListener {
            val intent = Intent(this, ProductFormActivity::class.java)
            startActivity(intent)
        }

        btnRegresarProductos.setOnClickListener {
            finish()
        }

        btnCancelarSeleccion.setOnClickListener {
            desactivarModoSeleccion()
        }

        btnEliminarSeleccionados.setOnClickListener {
            confirmarEliminacion()
        }

        btnSeleccionarTodosProductos.setOnClickListener {
            if (productAdapter.areAllProductsSelected()) {
                productAdapter.clearSelectedProducts()
            } else {
                productAdapter.selectAllProducts()
            }

            actualizarTextoBotonSeleccion()
        }

        cargarProductos()
    }

    override fun onResume() {
        super.onResume()
        cargarProductos()
    }

    private fun cargarProductos() {
        productList = dbHelper.getAllProducts()
        productAdapter.updateList(productList)

        txtCantidadProductos.text = if (productList.isEmpty()) {
            "No hay productos registrados"
        } else {
            "Productos registrados: ${productList.size}"
        }

        if (modoSeleccionActivo) {
            actualizarTextoBotonSeleccion()
        }
    }

    private fun activarModoSeleccion() {
        modoSeleccionActivo = true
        productAdapter.enableSelectionMode()

        panelSeleccionSuperior.visibility = View.VISIBLE
        panelEliminar.visibility = View.VISIBLE
        btnAgregarProducto.visibility = View.GONE

        actualizarTextoBotonSeleccion()
    }

    private fun desactivarModoSeleccion() {
        modoSeleccionActivo = false
        productAdapter.disableSelectionMode()

        panelSeleccionSuperior.visibility = View.GONE
        panelEliminar.visibility = View.GONE
        btnAgregarProducto.visibility = View.VISIBLE
    }

    private fun actualizarTextoBotonSeleccion() {
        val seleccionados = productAdapter.getSelectedCount()

        txtModoSeleccion.text = when (seleccionados) {
            0 -> "Ningún producto seleccionado"
            1 -> "1 seleccionado"
            else -> "$seleccionados seleccionados"
        }

        btnSeleccionarTodosProductos.text = if (productAdapter.areAllProductsSelected()) {
            "Quitar selección"
        } else {
            "Seleccionar todos"
        }
    }

    private fun confirmarEliminacion() {
        val productosSeleccionados = productAdapter.getSelectedProducts()

        if (productosSeleccionados.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un producto", Toast.LENGTH_SHORT).show()
            return
        }

        val vistaDialogo = LayoutInflater.from(this).inflate(R.layout.dialog_delete_products, null)

        val dialogo = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        val btnCancelar = vistaDialogo.findViewById<TextView>(R.id.btnDialogCancelarEliminar)
        val btnEliminar = vistaDialogo.findViewById<TextView>(R.id.btnDialogConfirmarEliminar)

        val txtMensaje = vistaDialogo.findViewById<TextView>(R.id.txtMensajeEliminar)

        if (txtMensaje != null) {
            txtMensaje.text = if (productosSeleccionados.size == 1) {
                "¿Desea eliminar el producto seleccionado?"
            } else {
                "¿Desea eliminar los ${productosSeleccionados.size} productos seleccionados?"
            }
        }

        aplicarFondoRedondeado(
            view = btnCancelar,
            color = ContextCompat.getColor(this, R.color.green_primary),
            strokeColor = null
        )

        aplicarFondoRedondeado(
            view = btnEliminar,
            color = ContextCompat.getColor(this, R.color.warning_color),
            strokeColor = null
        )

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        btnEliminar.setOnClickListener {
            dialogo.dismiss()
            eliminarProductos(productosSeleccionados)
        }

        dialogo.show()
        dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun aplicarFondoRedondeado(view: TextView, color: Int, strokeColor: Int?) {
        val radius = 18f * resources.displayMetrics.density

        val fondo = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)

            if (strokeColor != null) {
                setStroke(
                    (1f * resources.displayMetrics.density).toInt(),
                    strokeColor
                )
            }
        }

        view.background = fondo
    }

    private fun eliminarProductos(productosSeleccionados: ArrayList<Int>) {
        val eliminados = dbHelper.deleteProducts(productosSeleccionados)

        if (eliminados > 0) {
            Toast.makeText(this, "Productos eliminados correctamente", Toast.LENGTH_SHORT).show()
            desactivarModoSeleccion()
            cargarProductos()
        } else {
            Toast.makeText(this, "No se pudieron eliminar los productos", Toast.LENGTH_SHORT).show()
        }
    }
}