package com.uth.inventariopulperia

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.uth.inventariopulperia.adapters.MovementAdapter
import com.uth.inventariopulperia.database.InventoryDbHelper
import com.uth.inventariopulperia.models.MovementDetail
import com.uth.inventariopulperia.models.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MovementsActivity : AppCompatActivity() {

    private lateinit var btnRegresarMovimientos: TextView
    private lateinit var spinnerProductosMovimiento: Spinner
    private lateinit var txtStockActualMovimiento: TextView
    private lateinit var radioEntrada: RadioButton
    private lateinit var radioSalida: RadioButton
    private lateinit var edtCantidadMovimiento: EditText
    private lateinit var spinnerMotivoMovimiento: Spinner
    private lateinit var btnGuardarMovimiento: TextView
    private lateinit var listViewMovimientos: ListView
    private lateinit var txtTituloHistorial: TextView

    private lateinit var dbHelper: InventoryDbHelper
    private lateinit var movementAdapter: MovementAdapter

    private var productList = ArrayList<Product>()
    private var selectedProduct: Product? = null
    private var movementList = ArrayList<MovementDetail>()

    private var selectedMotivo: String = "Compra"
    private var productIdFromAlert: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movements)

        productIdFromAlert = intent.getIntExtra("PRODUCT_ID", 0)

        btnRegresarMovimientos = findViewById(R.id.btnRegresarMovimientos)
        spinnerProductosMovimiento = findViewById(R.id.spinnerProductosMovimiento)
        txtStockActualMovimiento = findViewById(R.id.txtStockActualMovimiento)
        radioEntrada = findViewById(R.id.radioEntrada)
        radioSalida = findViewById(R.id.radioSalida)
        edtCantidadMovimiento = findViewById(R.id.edtCantidadMovimiento)
        spinnerMotivoMovimiento = findViewById(R.id.spinnerMotivoMovimiento)
        btnGuardarMovimiento = findViewById(R.id.btnGuardarMovimiento)
        listViewMovimientos = findViewById(R.id.listViewMovimientos)
        txtTituloHistorial = findViewById(R.id.txtTituloHistorial)

        dbHelper = InventoryDbHelper(this)

        movementAdapter = MovementAdapter(this, movementList)
        listViewMovimientos.adapter = movementAdapter

        btnRegresarMovimientos.setOnClickListener {
            finish()
        }

        btnGuardarMovimiento.setOnClickListener {
            guardarMovimiento()
        }

        radioEntrada.setOnClickListener {
            cargarMotivosMovimiento()
        }

        radioSalida.setOnClickListener {
            cargarMotivosMovimiento()
        }

        cargarMotivosMovimiento()
        cargarProductosEnSpinner()
    }

    override fun onResume() {
        super.onResume()
        cargarProductosEnSpinner()
    }

    private fun cargarMotivosMovimiento() {
        val motivos = if (radioEntrada.isChecked) {
            arrayListOf(
                "Compra",
                "Devolución de cliente",
                "Ajuste positivo"
            )
        } else {
            arrayListOf(
                "Venta",
                "Devolución a proveedor",
                "Daño / pérdida",
                "Ajuste negativo"
            )
        }

        selectedMotivo = motivos[0]

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            motivos
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMotivoMovimiento.adapter = adapter

        spinnerMotivoMovimiento.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedMotivo = motivos[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedMotivo = motivos[0]
                }
            }
    }

    private fun cargarProductosEnSpinner() {
        productList = dbHelper.getAllProducts()

        if (productList.isEmpty()) {
            val emptyAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                arrayListOf("No hay productos registrados")
            )

            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerProductosMovimiento.adapter = emptyAdapter

            selectedProduct = null
            txtStockActualMovimiento.text = "Stock actual: --"
            cargarHistorialMovimientos()
            return
        }

        val productNames = productList.map { product ->
            product.name
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            productNames
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProductosMovimiento.adapter = adapter

        spinnerProductosMovimiento.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position >= 0 && position < productList.size) {
                        selectedProduct = productList[position]
                        txtStockActualMovimiento.text = "Stock actual: ${selectedProduct?.stock}"
                        cargarHistorialMovimientos()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedProduct = null
                    txtStockActualMovimiento.text = "Stock actual: --"
                    cargarHistorialMovimientos()
                }
            }

        val selectedIndex = if (productIdFromAlert > 0) {
            productList.indexOfFirst { it.id == productIdFromAlert }
        } else {
            0
        }

        val finalIndex = if (selectedIndex >= 0) selectedIndex else 0

        selectedProduct = productList[finalIndex]
        txtStockActualMovimiento.text = "Stock actual: ${selectedProduct?.stock}"
        spinnerProductosMovimiento.setSelection(finalIndex)
        cargarHistorialMovimientos()
    }

    private fun guardarMovimiento() {
        val producto = selectedProduct

        if (producto == null) {
            Toast.makeText(this, "No hay producto seleccionado", Toast.LENGTH_SHORT).show()
            return
        }

        val cantidadTexto = edtCantidadMovimiento.text.toString().trim()

        if (cantidadTexto.isEmpty()) {
            edtCantidadMovimiento.error = "Ingrese la cantidad"
            edtCantidadMovimiento.requestFocus()
            return
        }

        val cantidad = cantidadTexto.toIntOrNull()

        if (cantidad == null || cantidad <= 0) {
            edtCantidadMovimiento.error = "Ingrese una cantidad válida"
            edtCantidadMovimiento.requestFocus()
            return
        }

        val tipoMovimiento = if (radioEntrada.isChecked) {
            "ENTRADA"
        } else {
            "SALIDA"
        }

        if (tipoMovimiento == "SALIDA" && cantidad > producto.stock) {
            Toast.makeText(
                this,
                "Stock insuficiente. Disponible: ${producto.stock}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val resultado = dbHelper.registerMovementAndUpdateStock(
            product = producto,
            qty = cantidad,
            type = tipoMovimiento,
            notes = selectedMotivo,
            date = obtenerFechaActual()
        )

        if (resultado) {
            Toast.makeText(this, "Movimiento registrado correctamente", Toast.LENGTH_SHORT).show()

            edtCantidadMovimiento.text.clear()
            radioEntrada.isChecked = true
            cargarMotivosMovimiento()

            productIdFromAlert = producto.id
            cargarProductosEnSpinner()
            cargarHistorialMovimientos()
        } else {
            Toast.makeText(this, "Error al registrar movimiento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarHistorialMovimientos() {
        val producto = selectedProduct

        if (producto == null) {
            movementList = ArrayList()
            movementAdapter.updateList(movementList)
            txtTituloHistorial.text = "Historial de movimientos: vacío"
            return
        }

        movementList = dbHelper.getMovementHistoryByProduct(producto.id)
        movementAdapter.updateList(movementList)

        txtTituloHistorial.text = if (movementList.isEmpty()) {
            "Historial de ${producto.name}: vacío"
        } else {
            "Historial de ${producto.name}: ${movementList.size}"
        }
    }

    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formato.format(Date())
    }
}