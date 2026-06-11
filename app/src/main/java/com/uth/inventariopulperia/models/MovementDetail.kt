package com.uth.inventariopulperia.models

data class MovementDetail(
    var id: Int = 0,
    var productId: Int,
    var productName: String,
    var qty: Int,
    var type: String,
    var date: String,
    var notes: String
)