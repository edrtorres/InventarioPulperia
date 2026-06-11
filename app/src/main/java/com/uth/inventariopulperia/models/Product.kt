package com.uth.inventariopulperia.models

data class Product(
    var id: Int = 0,
    var name: String,
    var price: Double,
    var stock: Int,
    var barcode: String,
    var minStock: Int,
    var imageBase64: String,
    var createdAt: String
)