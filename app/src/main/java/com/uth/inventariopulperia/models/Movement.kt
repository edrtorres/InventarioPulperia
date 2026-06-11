package com.uth.inventariopulperia.models

data class Movement(
    var id: Int = 0,
    var productId: Int,
    var qty: Int,
    var type: String,
    var date: String,
    var notes: String
)