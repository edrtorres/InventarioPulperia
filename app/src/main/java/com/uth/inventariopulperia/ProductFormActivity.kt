package com.uth.inventariopulperia

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.uth.inventariopulperia.database.InventoryDbHelper
import com.uth.inventariopulperia.models.Product
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductFormActivity : AppCompatActivity() {

    private lateinit var btnRegresarFormulario: TextView
    private lateinit var btnSeleccionarImagen: TextView
    private lateinit var btnGuardarProducto: TextView

    private lateinit var imgProductoPreview: ImageView

    private lateinit var edtNombreProducto: EditText
    private lateinit var edtPrecioProducto: EditText
    private lateinit var edtStockProducto: EditText
    private lateinit var edtCodigoBarra: EditText
    private lateinit var edtStockMinimo: EditText

    private lateinit var dbHelper: InventoryDbHelper

    private var imageBase64: String = ""
    private var productId: Int = 0
    private var editMode: Boolean = false

    private val solicitarPermisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permisoConcedido ->
        if (permisoConcedido) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val tomarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imgProductoPreview.setImageBitmap(bitmap)
            imageBase64 = convertirBitmapABase64(bitmap)
        } else {
            Toast.makeText(this, "No se capturó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private val seleccionarGaleriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            cargarImagenDesdeGaleria(uri)
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_form)

        dbHelper = InventoryDbHelper(this)

        btnRegresarFormulario = findViewById(R.id.btnRegresarFormulario)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)
        btnGuardarProducto = findViewById(R.id.btnGuardarProducto)

        imgProductoPreview = findViewById(R.id.imgProductoPreview)

        edtNombreProducto = findViewById(R.id.edtNombreProducto)
        edtPrecioProducto = findViewById(R.id.edtPrecioProducto)
        edtStockProducto = findViewById(R.id.edtStockProducto)
        edtCodigoBarra = findViewById(R.id.edtCodigoBarra)
        edtStockMinimo = findViewById(R.id.edtStockMinimo)

        bloquearCampoStock()
        edtStockProducto.setText("0")

        productId = intent.getIntExtra("PRODUCT_ID", 0)

        if (productId > 0) {
            editMode = true
            cargarProductoParaEditar(productId)
        }

        btnRegresarFormulario.setOnClickListener {
            finish()
        }

        btnSeleccionarImagen.setOnClickListener {
            mostrarOpcionesImagen()
        }

        btnGuardarProducto.setOnClickListener {
            guardarProducto()
        }
    }

    private fun bloquearCampoStock() {
        edtStockProducto.isEnabled = false
        edtStockProducto.isFocusable = false
        edtStockProducto.alpha = 0.7f
    }

    private fun cargarProductoParaEditar(id: Int) {
        val producto = dbHelper.getProductById(id)

        if (producto == null) {
            Toast.makeText(this, "No se encontró el producto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        edtNombreProducto.setText(producto.name)
        edtPrecioProducto.setText(producto.price.toString())
        edtStockProducto.setText(producto.stock.toString())
        edtCodigoBarra.setText(producto.barcode)
        edtStockMinimo.setText(producto.minStock.toString())

        bloquearCampoStock()

        imageBase64 = producto.imageBase64

        if (imageBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imgProductoPreview.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imgProductoPreview.setImageResource(R.mipmap.ic_launcher)
            }
        }

        val titulo = findViewById<TextView>(R.id.txtTituloFormulario)
        val subtitulo = findViewById<TextView>(R.id.txtSubtituloFormulario)

        titulo.text = "Editar producto"
        subtitulo.text = "Actualiza los datos del producto"

        btnGuardarProducto.text = "Actualizar producto"
    }

    private fun mostrarOpcionesImagen() {
        val vistaDialogo = LayoutInflater.from(this).inflate(R.layout.dialog_image_options, null)

        val dialogo = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        val btnDialogCamara = vistaDialogo.findViewById<TextView>(R.id.btnDialogCamara)
        val btnDialogGaleria = vistaDialogo.findViewById<TextView>(R.id.btnDialogGaleria)
        val btnDialogCancelar = vistaDialogo.findViewById<TextView>(R.id.btnDialogCancelar)

        btnDialogCamara.setOnClickListener {
            dialogo.dismiss()
            validarPermisoCamara()
        }

        btnDialogGaleria.setOnClickListener {
            dialogo.dismiss()
            abrirGaleria()
        }

        btnDialogCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        dialogo.show()
        dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun validarPermisoCamara() {
        val permiso = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (permiso == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        tomarFotoLauncher.launch(null)
    }

    private fun abrirGaleria() {
        seleccionarGaleriaLauncher.launch("image/*")
    }

    private fun cargarImagenDesdeGaleria(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmapOriginal = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmapOriginal != null) {
                val bitmapReducido = reducirBitmap(bitmapOriginal)
                imgProductoPreview.setImageBitmap(bitmapReducido)
                imageBase64 = convertirBitmapABase64(bitmapReducido)
            } else {
                Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al seleccionar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertirBitmapABase64(bitmap: Bitmap): String {
        val bitmapReducido = reducirBitmap(bitmap)

        val outputStream = ByteArrayOutputStream()
        bitmapReducido.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun reducirBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 600

        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun guardarProducto() {
        val nombre = edtNombreProducto.text.toString().trim()
        val precioTexto = edtPrecioProducto.text.toString().trim()
        val codigoBarra = edtCodigoBarra.text.toString().trim()
        val stockMinimoTexto = edtStockMinimo.text.toString().trim()

        if (nombre.isEmpty()) {
            edtNombreProducto.error = "Ingrese el nombre del producto"
            edtNombreProducto.requestFocus()
            return
        }

        if (precioTexto.isEmpty()) {
            edtPrecioProducto.error = "Ingrese el precio"
            edtPrecioProducto.requestFocus()
            return
        }

        if (stockMinimoTexto.isEmpty()) {
            edtStockMinimo.error = "Ingrese el stock mínimo"
            edtStockMinimo.requestFocus()
            return
        }

        val precio = precioTexto.toDoubleOrNull()
        val stockMinimo = stockMinimoTexto.toIntOrNull()

        if (precio == null || precio <= 0) {
            edtPrecioProducto.error = "Ingrese un precio válido"
            edtPrecioProducto.requestFocus()
            return
        }

        if (stockMinimo == null || stockMinimo < 0) {
            edtStockMinimo.error = "Ingrese un stock mínimo válido"
            edtStockMinimo.requestFocus()
            return
        }

        val stockActual = if (editMode) {
            edtStockProducto.text.toString().trim().toIntOrNull() ?: 0
        } else {
            0
        }

        val producto = Product(
            id = productId,
            name = nombre,
            price = precio,
            stock = stockActual,
            barcode = codigoBarra,
            minStock = stockMinimo,
            imageBase64 = imageBase64,
            createdAt = obtenerFechaActual()
        )

        if (editMode) {
            val resultado = dbHelper.updateProduct(producto)

            if (resultado > 0) {
                Toast.makeText(this, "Producto actualizado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar el producto", Toast.LENGTH_SHORT).show()
            }
        } else {
            val resultado = dbHelper.insertProduct(producto)

            if (resultado > 0) {
                Toast.makeText(this, "Producto guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar el producto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formato.format(Date())
    }
}