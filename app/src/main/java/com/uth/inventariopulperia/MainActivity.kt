package com.uth.inventariopulperia

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.uth.inventariopulperia.database.InventoryDbHelper

class MainActivity : AppCompatActivity() {

    private lateinit var cardProductos: LinearLayout
    private lateinit var cardMovimientos: LinearLayout
    private lateinit var cardResumen: LinearLayout
    private lateinit var cardAlertas: LinearLayout
    private lateinit var cardHistorialGeneral: LinearLayout

    private lateinit var txtCantidadAlertasMenu: TextView
    private lateinit var btnSeedProductos: ImageView

    private lateinit var dbHelper: InventoryDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardProductos = findViewById(R.id.cardProductos)
        cardMovimientos = findViewById(R.id.cardMovimientos)
        cardResumen = findViewById(R.id.cardResumen)
        cardAlertas = findViewById(R.id.cardAlertas)
        cardHistorialGeneral = findViewById(R.id.cardHistorialGeneral)

        txtCantidadAlertasMenu = findViewById(R.id.txtCantidadAlertasMenu)
        btnSeedProductos = findViewById(R.id.btnSeedProductos)

        dbHelper = InventoryDbHelper(this)

        cardProductos.setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }

        cardMovimientos.setOnClickListener {
            startActivity(Intent(this, MovementsActivity::class.java))
        }

        cardResumen.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        cardAlertas.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        cardHistorialGeneral.setOnClickListener {
            startActivity(Intent(this, MovementHistoryActivity::class.java))
        }

        btnSeedProductos.setOnClickListener {
            mostrarConfirmacionSeed()
        }

        cargarCantidadAlertas()
    }

    override fun onResume() {
        super.onResume()
        cargarCantidadAlertas()
    }

    private fun cargarCantidadAlertas() {
        val cantidadAlertas = dbHelper.getLowStockProductsCount()

        if (cantidadAlertas == 0) {
            txtCantidadAlertasMenu.text = "Sin alertas activas"
            txtCantidadAlertasMenu.setTextColor(
                ContextCompat.getColor(this, R.color.green_primary_dark)
            )
            txtCantidadAlertasMenu.setBackgroundResource(R.drawable.bg_alert_badge_ok)
        } else if (cantidadAlertas == 1) {
            txtCantidadAlertasMenu.text = "1 alerta activa"
            txtCantidadAlertasMenu.setTextColor(
                ContextCompat.getColor(this, R.color.warning_color)
            )
            txtCantidadAlertasMenu.setBackgroundResource(R.drawable.bg_alert_badge)
        } else {
            txtCantidadAlertasMenu.text = "$cantidadAlertas alertas activas"
            txtCantidadAlertasMenu.setTextColor(
                ContextCompat.getColor(this, R.color.warning_color)
            )
            txtCantidadAlertasMenu.setBackgroundResource(R.drawable.bg_alert_badge)
        }
    }

    private fun mostrarConfirmacionSeed() {
        val vistaDialogo = layoutInflater.inflate(R.layout.dialog_seed_data, null)

        val dialogo = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        val btnCancelar = vistaDialogo.findViewById<TextView>(R.id.btnDialogCancelarSeed)
        val btnCargar = vistaDialogo.findViewById<TextView>(R.id.btnDialogCargarSeed)

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        btnCargar.setOnClickListener {
            val resultado = dbHelper.seedDemoData()

            if (resultado) {
                Toast.makeText(this, "Seed cargado correctamente", Toast.LENGTH_SHORT).show()
                cargarCantidadAlertas()
            } else {
                Toast.makeText(this, "No se pudo cargar el seed", Toast.LENGTH_SHORT).show()
            }

            dialogo.dismiss()
        }

        dialogo.show()
        dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}