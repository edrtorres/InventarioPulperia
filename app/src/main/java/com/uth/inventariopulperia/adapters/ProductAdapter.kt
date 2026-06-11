package com.uth.inventariopulperia.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.uth.inventariopulperia.R
import com.uth.inventariopulperia.models.Product

class ProductAdapter(
    private val context: Context,
    private var productList: ArrayList<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onProductLongClick: (Product) -> Unit,
    private val onSelectionChanged: () -> Unit = {}
) : BaseAdapter() {

    var selectionMode: Boolean = false
    private val selectedProducts = HashSet<Int>()

    override fun getCount(): Int {
        return productList.size
    }

    override fun getItem(position: Int): Any {
        return productList[position]
    }

    override fun getItemId(position: Int): Long {
        return productList[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_product, parent, false)

        val product = productList[position]

        val chkSeleccionProducto = view.findViewById<CheckBox>(R.id.chkSeleccionProducto)
        val imgProductoItem = view.findViewById<ImageView>(R.id.imgProductoItem)
        val txtNombreProductoItem = view.findViewById<TextView>(R.id.txtNombreProductoItem)
        val txtPrecioProductoItem = view.findViewById<TextView>(R.id.txtPrecioProductoItem)
        val txtStockProductoItem = view.findViewById<TextView>(R.id.txtStockProductoItem)
        val txtBarcodeProductoItem = view.findViewById<TextView>(R.id.txtBarcodeProductoItem)
        val txtAlertaProductoItem = view.findViewById<TextView>(R.id.txtAlertaProductoItem)

        txtNombreProductoItem.text = product.name
        txtPrecioProductoItem.text = "Precio: L. %.2f".format(product.price)
        txtStockProductoItem.text = "Stock: ${product.stock}"
        txtBarcodeProductoItem.text =
            "Código: ${if (product.barcode.isNotEmpty()) product.barcode else "---"}"

        if (product.stock <= product.minStock) {
            txtAlertaProductoItem.visibility = View.VISIBLE
            txtAlertaProductoItem.text = "Stock bajo: mínimo ${product.minStock}"
        } else {
            txtAlertaProductoItem.visibility = View.GONE
        }

        if (product.imageBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(product.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imgProductoItem.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imgProductoItem.setImageResource(R.mipmap.ic_launcher)
            }
        } else {
            imgProductoItem.setImageResource(R.mipmap.ic_launcher)
        }

        if (selectionMode) {
            chkSeleccionProducto.visibility = View.VISIBLE
            chkSeleccionProducto.isChecked = selectedProducts.contains(product.id)
        } else {
            chkSeleccionProducto.visibility = View.GONE
            chkSeleccionProducto.isChecked = false
        }

        chkSeleccionProducto.setOnClickListener {
            if (selectionMode) {
                toggleSelection(product.id)
            }
        }

        view.setOnClickListener {
            onProductClick(product)
        }

        view.setOnLongClickListener {
            onProductLongClick(product)
            true
        }

        return view
    }

    fun updateList(newList: ArrayList<Product>) {
        productList = newList
        notifyDataSetChanged()
    }

    fun enableSelectionMode() {
        selectionMode = true
        notifyDataSetChanged()
    }

    fun disableSelectionMode() {
        selectionMode = false
        selectedProducts.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(productId: Int) {
        if (selectedProducts.contains(productId)) {
            selectedProducts.remove(productId)
        } else {
            selectedProducts.add(productId)
        }

        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun getSelectedProducts(): ArrayList<Int> {
        return ArrayList(selectedProducts)
    }

    fun hasSelectedProducts(): Boolean {
        return selectedProducts.isNotEmpty()
    }

    fun selectAllProducts() {
        selectedProducts.clear()

        for (product in productList) {
            selectedProducts.add(product.id)
        }

        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun clearSelectedProducts() {
        selectedProducts.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun areAllProductsSelected(): Boolean {
        return productList.isNotEmpty() && selectedProducts.size == productList.size
    }

    fun getSelectedCount(): Int {
        return selectedProducts.size
    }
}