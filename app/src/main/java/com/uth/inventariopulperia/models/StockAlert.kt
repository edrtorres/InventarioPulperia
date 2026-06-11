package com.uth.inventariopulperia.models

data class StockAlert(
    var id: Int = 0,
    var productId: Int,
    var alertType: String,
    var message: String,
    var date: String,
    var status: String
)