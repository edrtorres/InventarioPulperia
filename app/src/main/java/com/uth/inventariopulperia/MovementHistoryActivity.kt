package com.uth.inventariopulperia

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.uth.inventariopulperia.adapters.MovementAdapter
import com.uth.inventariopulperia.database.InventoryDbHelper
import com.uth.inventariopulperia.models.MovementDetail
import com.uth.inventariopulperia.models.Product

class MovementHistoryActivity : AppCompatActivity() {

    private lateinit var btnRegresarHistorial: TextView
    private lateinit var spinnerFiltroProducto: Spinner
    private lateinit var spinnerFiltroTipo: Spinner
    private lateinit var txtCantidadHistorialGeneral: TextView
    private lateinit var listViewHistorialGeneral: ListView
    private lateinit var dbHelper: InventoryDbHelper
    private lateinit var movementAdapter: MovementAdapter
    private var productList = ArrayList<Product>()
    private var movementList = ArrayList<MovementDetail>()
    private var selectedProductId: Int = 0
    private var selectedType: String = "TODOS"
    private lateinit var spinnerFiltroFecha: Spinner
    private var selectedDateFilter: String = "TODAS"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movement_history)

        btnRegresarHistorial = findViewById(R.id.btnRegresarHistorial)
        spinnerFiltroProducto = findViewById(R.id.spinnerFiltroProducto)
        spinnerFiltroTipo = findViewById(R.id.spinnerFiltroTipo)
        txtCantidadHistorialGeneral = findViewById(R.id.txtCantidadHistorialGeneral)
        listViewHistorialGeneral = findViewById(R.id.listViewHistorialGeneral)
        spinnerFiltroFecha = findViewById(R.id.spinnerFiltroFecha)


        dbHelper = InventoryDbHelper(this)

        movementAdapter = MovementAdapter(this, movementList)
        listViewHistorialGeneral.adapter = movementAdapter

        btnRegresarHistorial.setOnClickListener {
            finish()
        }

        cargarFiltroTipo()
        cargarFiltroFecha()
        cargarFiltroProductos()
        cargarHistorialFiltrado()
    }

    override fun onResume() {
        super.onResume()
        cargarFiltroProductos()
        cargarHistorialFiltrado()
    }

    private fun cargarFiltroTipo() {
        val tipos = arrayListOf("Todos", "Entrada", "Salida")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tipos
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltroTipo.adapter = adapter

        spinnerFiltroTipo.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedType = when (position) {
                        1 -> "ENTRADA"
                        2 -> "SALIDA"
                        else -> "TODOS"
                    }

                    cargarHistorialFiltrado()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedType = "TODOS"
                    cargarHistorialFiltrado()
                }
            }
    }

    private fun cargarFiltroProductos() {
        productList = dbHelper.getAllProducts()

        val productNames = ArrayList<String>()
        productNames.add("Todos los productos")

        for (product in productList) {
            productNames.add(product.name)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            productNames
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltroProducto.adapter = adapter

        spinnerFiltroProducto.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedProductId = if (position == 0) {
                        0
                    } else {
                        productList[position - 1].id
                    }

                    cargarHistorialFiltrado()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedProductId = 0
                    cargarHistorialFiltrado()
                }
            }
    }

    private fun cargarHistorialFiltrado() {
        movementList = dbHelper.getMovementHistoryFiltered(
            productId = selectedProductId,
            type = selectedType,
            dateFilter = selectedDateFilter
        )

        movementAdapter.updateList(movementList)

        txtCantidadHistorialGeneral.text = if (movementList.isEmpty()) {
            "Movimientos registrados: vacío"
        } else {
            "Movimientos registrados: ${movementList.size}"
        }
    }

    private fun cargarFiltroFecha() {
        val fechas = arrayListOf(
            "Todas las fechas",
            "Hoy",
            "Ayer",
            "Esta semana",
            "Este mes"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            fechas
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltroFecha.adapter = adapter

        spinnerFiltroFecha.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedDateFilter = when (position) {
                        1 -> "HOY"
                        2 -> "AYER"
                        3 -> "SEMANA"
                        4 -> "MES"
                        else -> "TODAS"
                    }

                    cargarHistorialFiltrado()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedDateFilter = "TODAS"
                    cargarHistorialFiltrado()
                }
            }
    }
}