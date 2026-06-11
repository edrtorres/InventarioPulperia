package com.uth.inventariopulperia

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.uth.inventariopulperia.database.InventoryDbHelper
import com.uth.inventariopulperia.models.Product
import java.text.DecimalFormat

class SummaryActivity : AppCompatActivity() {

    private lateinit var btnRegresarResumen: TextView

    private lateinit var txtValorTotalInventario: TextView
    private lateinit var txtTotalProductosResumen: TextView
    private lateinit var txtTotalUnidadesResumen: TextView
    private lateinit var txtProductosStockBajoResumen: TextView

    private lateinit var txtEntradasMovimientosResumen: TextView
    private lateinit var txtSalidasMovimientosResumen: TextView
    private lateinit var trackEntradaMovimientosResumen: LinearLayout
    private lateinit var trackSalidaMovimientosResumen: LinearLayout
    private lateinit var barEntradaMovimientosResumen: View
    private lateinit var barSalidaMovimientosResumen: View

    private lateinit var chartProductosValorResumen: LinearLayout
    private lateinit var contenedorTopProductosResumen: LinearLayout
    private lateinit var contenedorStockBajoResumen: LinearLayout

    private lateinit var dbHelper: InventoryDbHelper

    private val decimalMoneda = DecimalFormat("#,##0.00")
    private val decimalEntero = DecimalFormat("#,###")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        dbHelper = InventoryDbHelper(this)

        btnRegresarResumen = findViewById(R.id.btnRegresarResumen)

        txtValorTotalInventario = findViewById(R.id.txtValorTotalInventario)
        txtTotalProductosResumen = findViewById(R.id.txtTotalProductosResumen)
        txtTotalUnidadesResumen = findViewById(R.id.txtTotalUnidadesResumen)
        txtProductosStockBajoResumen = findViewById(R.id.txtProductosStockBajoResumen)

        txtEntradasMovimientosResumen = findViewById(R.id.txtEntradasMovimientosResumen)
        txtSalidasMovimientosResumen = findViewById(R.id.txtSalidasMovimientosResumen)
        trackEntradaMovimientosResumen = findViewById(R.id.trackEntradaMovimientosResumen)
        trackSalidaMovimientosResumen = findViewById(R.id.trackSalidaMovimientosResumen)
        barEntradaMovimientosResumen = findViewById(R.id.barEntradaMovimientosResumen)
        barSalidaMovimientosResumen = findViewById(R.id.barSalidaMovimientosResumen)

        chartProductosValorResumen = findViewById(R.id.chartProductosValorResumen)
        contenedorTopProductosResumen = findViewById(R.id.contenedorTopProductosResumen)
        contenedorStockBajoResumen = findViewById(R.id.contenedorStockBajoResumen)

        btnRegresarResumen.setOnClickListener {
            finish()
        }

        cargarDashboard()
    }

    override fun onResume() {
        super.onResume()
        cargarDashboard()
    }

    private fun cargarDashboard() {
        val valorTotal = dbHelper.getTotalInventoryValue()
        val totalProductos = dbHelper.getTotalProductsCount()
        val totalUnidades = dbHelper.getTotalStockUnits()
        val totalStockBajo = dbHelper.getLowStockProductsCount()

        txtValorTotalInventario.text = "L. ${formatearMoneda(valorTotal)}"
        txtTotalProductosResumen.text = formatearEntero(totalProductos)
        txtTotalUnidadesResumen.text = formatearEntero(totalUnidades)
        txtProductosStockBajoResumen.text = formatearEntero(totalStockBajo)

        val totalEntradas = dbHelper.getMovementQuantityByType("ENTRADA")
        val totalSalidas = dbHelper.getMovementQuantityByType("SALIDA")

        txtEntradasMovimientosResumen.text =
            "Entradas: ${formatearEntero(totalEntradas)} unidades"

        txtSalidasMovimientosResumen.text =
            "Salidas: ${formatearEntero(totalSalidas)} unidades"

        val maxMovimiento = maxOf(totalEntradas, totalSalidas)

        actualizarBarra(
            barEntradaMovimientosResumen,
            trackEntradaMovimientosResumen,
            totalEntradas,
            maxMovimiento
        )

        actualizarBarra(
            barSalidaMovimientosResumen,
            trackSalidaMovimientosResumen,
            totalSalidas,
            maxMovimiento
        )

        val topProductos = dbHelper.getTopProductsByValue()
        val productosStockBajo = dbHelper.getLowStockProducts()

        cargarGraficoProductosPorValor(topProductos)
        cargarTopProductos(topProductos)
        cargarStockBajo(productosStockBajo)
    }

    private fun cargarGraficoProductosPorValor(productos: ArrayList<Product>) {
        chartProductosValorResumen.removeAllViews()

        if (productos.isEmpty()) {
            agregarTextoVacio(chartProductosValorResumen, "No hay productos registrados")
            return
        }

        val maxValor = productos.maxOf { it.price * it.stock }

        for ((index, producto) in productos.withIndex()) {
            val valorProducto = producto.price * producto.stock

            val txtProducto = TextView(this)
            txtProducto.text =
                "${index + 1}. ${producto.name}  •  L. ${formatearMoneda(valorProducto)}"
            txtProducto.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            txtProducto.textSize = 14f
            txtProducto.setTypeface(null, Typeface.BOLD)

            val paramsTexto = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            if (index > 0) {
                paramsTexto.topMargin = dp(14)
            }

            chartProductosValorResumen.addView(txtProducto, paramsTexto)

            val track = LinearLayout(this)
            track.orientation = LinearLayout.HORIZONTAL
            track.setBackgroundResource(R.drawable.bg_chart_track_soft)

            val paramsTrack = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(16)
            )
            paramsTrack.topMargin = dp(7)

            val bar = View(this)
            bar.setBackgroundResource(R.drawable.bg_chart_dark_green)

            val paramsBar = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            track.addView(bar, paramsBar)
            chartProductosValorResumen.addView(track, paramsTrack)

            track.post {
                val porcentaje = if (maxValor <= 0) {
                    0.0
                } else {
                    valorProducto / maxValor
                }

                val nuevoAncho = (track.width * porcentaje).toInt()

                val layoutParams = bar.layoutParams
                layoutParams.width = if (valorProducto > 0) {
                    nuevoAncho.coerceAtLeast(dp(8))
                } else {
                    0
                }

                bar.layoutParams = layoutParams
            }
        }
    }

    private fun cargarTopProductos(productos: ArrayList<Product>) {
        contenedorTopProductosResumen.removeAllViews()

        if (productos.isEmpty()) {
            agregarTextoVacio(contenedorTopProductosResumen, "No hay productos registrados")
            return
        }

        for ((index, producto) in productos.withIndex()) {
            val valorProducto = producto.price * producto.stock

            val fila = crearCardProducto(
                titulo = "${index + 1}. ${producto.name}",
                detalle = "Stock: ${formatearEntero(producto.stock)}  |  Valor: L. ${formatearMoneda(valorProducto)}",
                esAlerta = false
            )

            contenedorTopProductosResumen.addView(fila)
        }
    }

    private fun cargarStockBajo(productos: ArrayList<Product>) {
        contenedorStockBajoResumen.removeAllViews()

        if (productos.isEmpty()) {
            agregarTextoVacio(contenedorStockBajoResumen, "No hay productos con stock bajo")
            return
        }

        for (producto in productos) {
            val fila = crearCardProducto(
                titulo = producto.name,
                detalle = "Stock actual: ${formatearEntero(producto.stock)}  |  Mínimo: ${formatearEntero(producto.minStock)}",
                esAlerta = true
            )

            contenedorStockBajoResumen.addView(fila)
        }
    }

    private fun crearCardProducto(
        titulo: String,
        detalle: String,
        esAlerta: Boolean
    ): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(14), dp(12), dp(14), dp(12))

        if (esAlerta) {
            card.setBackgroundResource(R.drawable.bg_dashboard_row_warning)
        } else {
            card.setBackgroundResource(R.drawable.bg_dashboard_row)
        }

        val paramsCard = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        paramsCard.bottomMargin = dp(10)
        card.layoutParams = paramsCard

        val txtTitulo = TextView(this)
        txtTitulo.text = titulo
        txtTitulo.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        txtTitulo.textSize = 16f
        txtTitulo.setTypeface(null, Typeface.BOLD)

        val txtDetalle = TextView(this)
        txtDetalle.text = detalle
        txtDetalle.setTextColor(
            ContextCompat.getColor(
                this,
                if (esAlerta) R.color.warning_color else R.color.text_secondary
            )
        )
        txtDetalle.textSize = 13f

        val paramsDetalle = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        paramsDetalle.topMargin = dp(5)

        card.addView(txtTitulo)
        card.addView(txtDetalle, paramsDetalle)

        return card
    }

    private fun agregarTextoVacio(contenedor: LinearLayout, mensaje: String) {
        val txtVacio = TextView(this)
        txtVacio.text = mensaje
        txtVacio.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        txtVacio.textSize = 14f
        txtVacio.setPadding(dp(14), dp(14), dp(14), dp(14))
        txtVacio.setBackgroundResource(R.drawable.bg_dashboard_row)

        contenedor.addView(txtVacio)
    }

    private fun actualizarBarra(
        bar: View,
        track: LinearLayout,
        valor: Int,
        maximo: Int
    ) {
        track.post {
            val porcentaje = if (maximo <= 0) {
                0.0
            } else {
                valor.toDouble() / maximo.toDouble()
            }

            val nuevoAncho = (track.width * porcentaje).toInt()

            val params = bar.layoutParams
            params.width = if (valor > 0) {
                nuevoAncho.coerceAtLeast(dp(8))
            } else {
                0
            }

            bar.layoutParams = params
        }
    }

    private fun formatearMoneda(valor: Double): String {
        return decimalMoneda.format(valor)
    }

    private fun formatearEntero(valor: Int): String {
        return decimalEntero.format(valor)
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}