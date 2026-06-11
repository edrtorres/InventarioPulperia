package com.uth.inventariopulperia.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uth.inventariopulperia.models.Movement
import com.uth.inventariopulperia.models.MovementDetail
import com.uth.inventariopulperia.models.Product

class InventoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "inventory_pulperia.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_PRODUCTS = "products"
        const val TABLE_MOVEMENTS = "movements"
        const val TABLE_ALERTS = "alerts"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProductsTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                stock INTEGER NOT NULL,
                barcode TEXT,
                min_stock INTEGER NOT NULL,
                image_base64 TEXT,
                created_at TEXT NOT NULL
            )
        """.trimIndent()

        val createMovementsTable = """
            CREATE TABLE $TABLE_MOVEMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                qty INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                notes TEXT,
                FOREIGN KEY(product_id) REFERENCES $TABLE_PRODUCTS(id)
            )
        """.trimIndent()

        val createAlertsTable = """
            CREATE TABLE $TABLE_ALERTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                alert_type TEXT NOT NULL,
                message TEXT NOT NULL,
                date TEXT NOT NULL,
                status TEXT NOT NULL,
                FOREIGN KEY(product_id) REFERENCES $TABLE_PRODUCTS(id)
            )
        """.trimIndent()

        db.execSQL(createProductsTable)
        db.execSQL(createMovementsTable)
        db.execSQL(createAlertsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVEMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    fun insertProduct(product: Product): Long {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("name", product.name)
            put("price", product.price)
            put("stock", product.stock)
            put("barcode", product.barcode)
            put("min_stock", product.minStock)
            put("image_base64", product.imageBase64)
            put("created_at", product.createdAt)
        }

        return db.insert(TABLE_PRODUCTS, null, values)
    }

    fun getAllProducts(): ArrayList<Product> {
        val productList = ArrayList<Product>()
        val db = readableDatabase

        val query = """
            SELECT id, name, price, stock, barcode, min_stock, image_base64, created_at
            FROM $TABLE_PRODUCTS
            ORDER BY id DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode")) ?: "",
                    minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock")),
                    imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow("image_base64")) ?: "",
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                )

                productList.add(product)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return productList
    }

    fun getProductById(productId: Int): Product? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            """
                SELECT id, name, price, stock, barcode, min_stock, image_base64, created_at
                FROM $TABLE_PRODUCTS
                WHERE id = ?
            """.trimIndent(),
            arrayOf(productId.toString())
        )

        var product: Product? = null

        if (cursor.moveToFirst()) {
            product = Product(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode")) ?: "",
                minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock")),
                imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow("image_base64")) ?: "",
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
            )
        }

        cursor.close()
        return product
    }

    fun updateProduct(product: Product): Int {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("name", product.name)
            put("price", product.price)
            put("barcode", product.barcode)
            put("min_stock", product.minStock)
            put("image_base64", product.imageBase64)
        }

        return db.update(
            TABLE_PRODUCTS,
            values,
            "id = ?",
            arrayOf(product.id.toString())
        )
    }

    fun deleteProducts(productIds: ArrayList<Int>): Int {
        if (productIds.isEmpty()) {
            return 0
        }

        val db = writableDatabase
        var deletedRows = 0

        db.beginTransaction()
        try {
            for (productId in productIds) {
                db.delete(TABLE_ALERTS, "product_id = ?", arrayOf(productId.toString()))
                db.delete(TABLE_MOVEMENTS, "product_id = ?", arrayOf(productId.toString()))
                deletedRows += db.delete(TABLE_PRODUCTS, "id = ?", arrayOf(productId.toString()))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return deletedRows
    }

    fun updateProductStock(productId: Int, newStock: Int): Int {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("stock", newStock)
        }

        return db.update(
            TABLE_PRODUCTS,
            values,
            "id = ?",
            arrayOf(productId.toString())
        )
    }

    fun insertMovement(movement: Movement): Long {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("product_id", movement.productId)
            put("qty", movement.qty)
            put("type", movement.type)
            put("date", movement.date)
            put("notes", movement.notes)
        }

        return db.insert(TABLE_MOVEMENTS, null, values)
    }

    fun registerMovementAndUpdateStock(
        product: Product,
        qty: Int,
        type: String,
        notes: String,
        date: String
    ): Boolean {
        val db = writableDatabase

        val newStock = if (type == "ENTRADA") {
            product.stock + qty
        } else {
            product.stock - qty
        }

        db.beginTransaction()

        return try {
            val stockValues = ContentValues().apply {
                put("stock", newStock)
            }

            val updatedRows = db.update(
                TABLE_PRODUCTS,
                stockValues,
                "id = ?",
                arrayOf(product.id.toString())
            )

            val movementValues = ContentValues().apply {
                put("product_id", product.id)
                put("qty", qty)
                put("type", type)
                put("date", date)
                put("notes", notes)
            }

            val insertedMovement = db.insert(TABLE_MOVEMENTS, null, movementValues)

            if (updatedRows > 0 && insertedMovement > 0) {
                db.setTransactionSuccessful()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getMovementHistory(): ArrayList<MovementDetail> {
        val movementList = ArrayList<MovementDetail>()
        val db = readableDatabase

        val query = """
            SELECT 
                m.id,
                m.product_id,
                p.name AS product_name,
                m.qty,
                m.type,
                m.date,
                m.notes
            FROM $TABLE_MOVEMENTS m
            INNER JOIN $TABLE_PRODUCTS p ON m.product_id = p.id
            ORDER BY m.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val movement = MovementDetail(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    productId = cursor.getInt(cursor.getColumnIndexOrThrow("product_id")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    qty = cursor.getInt(cursor.getColumnIndexOrThrow("qty")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                )

                movementList.add(movement)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return movementList
    }

    fun getMovementHistoryByProduct(productId: Int): ArrayList<MovementDetail> {
        val movementList = ArrayList<MovementDetail>()
        val db = readableDatabase

        val query = """
            SELECT 
                m.id,
                m.product_id,
                p.name AS product_name,
                m.qty,
                m.type,
                m.date,
                m.notes
            FROM $TABLE_MOVEMENTS m
            INNER JOIN $TABLE_PRODUCTS p ON m.product_id = p.id
            WHERE m.product_id = ?
            ORDER BY m.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(productId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val movement = MovementDetail(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    productId = cursor.getInt(cursor.getColumnIndexOrThrow("product_id")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    qty = cursor.getInt(cursor.getColumnIndexOrThrow("qty")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                )

                movementList.add(movement)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return movementList
    }

    fun getMovementHistoryFiltered(
        productId: Int,
        type: String,
        dateFilter: String
    ): ArrayList<MovementDetail> {
        val movementList = ArrayList<MovementDetail>()
        val db = readableDatabase

        val whereConditions = ArrayList<String>()
        val args = ArrayList<String>()

        if (productId > 0) {
            whereConditions.add("m.product_id = ?")
            args.add(productId.toString())
        }

        if (type != "TODOS") {
            whereConditions.add("m.type = ?")
            args.add(type)
        }

        when (dateFilter) {
            "HOY" -> {
                whereConditions.add("date(m.date) = date('now', 'localtime')")
            }

            "AYER" -> {
                whereConditions.add("date(m.date) = date('now', '-1 day', 'localtime')")
            }

            "SEMANA" -> {
                whereConditions.add("date(m.date) >= date('now', 'weekday 1', '-7 days', 'localtime')")
                whereConditions.add("date(m.date) <= date('now', 'localtime')")
            }

            "MES" -> {
                whereConditions.add("strftime('%Y-%m', m.date) = strftime('%Y-%m', 'now', 'localtime')")
            }
        }

        val whereClause = if (whereConditions.isNotEmpty()) {
            "WHERE " + whereConditions.joinToString(" AND ")
        } else {
            ""
        }

        val query = """
            SELECT 
                m.id,
                m.product_id,
                p.name AS product_name,
                m.qty,
                m.type,
                m.date,
                m.notes
            FROM $TABLE_MOVEMENTS m
            INNER JOIN $TABLE_PRODUCTS p ON m.product_id = p.id
            $whereClause
            ORDER BY m.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, args.toTypedArray())

        if (cursor.moveToFirst()) {
            do {
                val movement = MovementDetail(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    productId = cursor.getInt(cursor.getColumnIndexOrThrow("product_id")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    qty = cursor.getInt(cursor.getColumnIndexOrThrow("qty")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                )

                movementList.add(movement)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return movementList
    }

    fun getTotalInventoryValue(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(price * stock) FROM $TABLE_PRODUCTS",
            null
        )

        var total = 0.0

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        return total
    }

    fun getTotalProductsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PRODUCTS",
            null
        )

        var total = 0

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }

        cursor.close()
        return total
    }

    fun getTotalStockUnits(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(stock) FROM $TABLE_PRODUCTS",
            null
        )

        var total = 0

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }

        cursor.close()
        return total
    }

    fun getLowStockProductsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PRODUCTS WHERE stock <= min_stock",
            null
        )

        var total = 0

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }

        cursor.close()
        return total
    }

    fun getTopProductsByValue(): ArrayList<Product> {
        val productList = ArrayList<Product>()
        val db = readableDatabase

        val query = """
            SELECT id, name, price, stock, barcode, min_stock, image_base64, created_at
            FROM $TABLE_PRODUCTS
            ORDER BY (price * stock) DESC
            LIMIT 5
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode")) ?: "",
                    minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock")),
                    imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow("image_base64")) ?: "",
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                )

                productList.add(product)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return productList
    }

    fun getLowStockProducts(): ArrayList<Product> {
        val productList = ArrayList<Product>()
        val db = readableDatabase

        val query = """
            SELECT id, name, price, stock, barcode, min_stock, image_base64, created_at
            FROM $TABLE_PRODUCTS
            WHERE stock <= min_stock
            ORDER BY stock ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode")) ?: "",
                    minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock")),
                    imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow("image_base64")) ?: "",
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                )

                productList.add(product)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return productList
    }

    fun getMovementQuantityByType(type: String): Int {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT SUM(qty) FROM $TABLE_MOVEMENTS WHERE type = ?",
            arrayOf(type)
        )

        var total = 0

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }

        cursor.close()
        return total
    }

    fun seedDemoData(): Boolean {
        val db = writableDatabase

        db.beginTransaction()

        return try {
            val seedCursor = db.rawQuery(
                """
                SELECT id 
                FROM $TABLE_PRODUCTS 
                WHERE barcode LIKE ? 
                   OR name LIKE ?
            """.trimIndent(),
                arrayOf("SEED%", "DEMO - %")
            )

            val seedIds = ArrayList<Int>()

            if (seedCursor.moveToFirst()) {
                do {
                    seedIds.add(seedCursor.getInt(0))
                } while (seedCursor.moveToNext())
            }

            seedCursor.close()

            for (productId in seedIds) {
                db.delete(TABLE_ALERTS, "product_id = ?", arrayOf(productId.toString()))
                db.delete(TABLE_MOVEMENTS, "product_id = ?", arrayOf(productId.toString()))
                db.delete(TABLE_PRODUCTS, "id = ?", arrayOf(productId.toString()))
            }

            fun insertarProducto(
                name: String,
                price: Double,
                minStock: Int,
                barcode: String
            ): Long {
                val values = ContentValues().apply {
                    put("name", name)
                    put("price", price)
                    put("stock", 0)
                    put("barcode", barcode)
                    put("min_stock", minStock)
                    put("image_base64", "")
                    put("created_at", "2026-06-10 08:00:00")
                }

                return db.insert(TABLE_PRODUCTS, null, values)
            }

            fun registrarMovimiento(
                productId: Long,
                qty: Int,
                type: String,
                notes: String,
                date: String
            ) {
                val currentCursor = db.rawQuery(
                    "SELECT stock FROM $TABLE_PRODUCTS WHERE id = ?",
                    arrayOf(productId.toString())
                )

                var currentStock = 0

                if (currentCursor.moveToFirst()) {
                    currentStock = currentCursor.getInt(0)
                }

                currentCursor.close()

                val newStock = if (type == "ENTRADA") {
                    currentStock + qty
                } else {
                    currentStock - qty
                }

                val stockValues = ContentValues().apply {
                    put("stock", newStock)
                }

                db.update(
                    TABLE_PRODUCTS,
                    stockValues,
                    "id = ?",
                    arrayOf(productId.toString())
                )

                val movementValues = ContentValues().apply {
                    put("product_id", productId)
                    put("qty", qty)
                    put("type", type)
                    put("date", date)
                    put("notes", notes)
                }

                db.insert(TABLE_MOVEMENTS, null, movementValues)
            }

            val coca = insertarProducto("Coca Cola 2L", 45.0, 8, "SEED001")
            val arroz = insertarProducto("Arroz 5 lb", 120.0, 5, "SEED002")
            val aceite = insertarProducto("Aceite 1L", 95.0, 6, "SEED003")
            val azucar = insertarProducto("Azúcar 2 lb", 38.0, 10, "SEED004")
            val cafe = insertarProducto("Café 250g", 85.0, 4, "SEED005")
            val frijoles = insertarProducto("Frijoles 5 lb", 110.0, 5, "SEED006")
            val leche = insertarProducto("Leche 1L", 32.0, 12, "SEED007")
            val pan = insertarProducto("Pan molde", 55.0, 6, "SEED008")
            val huevos = insertarProducto("Huevos cartón", 145.0, 4, "SEED009")
            val papel = insertarProducto("Papel higiénico 4 rollos", 78.0, 7, "SEED010")
            val jabon = insertarProducto("Jabón de baño", 28.0, 10, "SEED011")
            val pasta = insertarProducto("Pasta dental", 65.0, 5, "SEED012")
            val agua = insertarProducto("Agua 600ml", 18.0, 20, "SEED013")
            val galletas = insertarProducto("Galletas paquete", 25.0, 15, "SEED014")
            val detergente = insertarProducto("Detergente 1kg", 98.0, 4, "SEED015")

            // Coca Cola: stock normal, compra y ventas
            registrarMovimiento(coca, 40, "ENTRADA", "Compra", "2026-06-08 09:15:00")
            registrarMovimiento(coca, 12, "SALIDA", "Venta", "2026-06-09 15:40:00")
            registrarMovimiento(coca, 5, "SALIDA", "Venta", "2026-06-10 10:10:00")

            // Arroz: stock normal, compra y venta
            registrarMovimiento(arroz, 20, "ENTRADA", "Compra", "2026-06-07 11:00:00")
            registrarMovimiento(arroz, 4, "SALIDA", "Venta", "2026-06-10 12:20:00")

            // Aceite: escenario de daño / pérdida
            registrarMovimiento(aceite, 18, "ENTRADA", "Compra", "2026-06-06 08:45:00")
            registrarMovimiento(aceite, 3, "SALIDA", "Daño / pérdida", "2026-06-10 13:25:00")

            // Azúcar: queda con stock bajo
            registrarMovimiento(azucar, 8, "ENTRADA", "Compra", "2026-06-08 14:30:00")
            registrarMovimiento(azucar, 3, "SALIDA", "Venta", "2026-06-10 16:00:00")

            // Café: queda con stock bajo
            registrarMovimiento(cafe, 10, "ENTRADA", "Compra", "2026-06-09 10:00:00")
            registrarMovimiento(cafe, 7, "SALIDA", "Venta", "2026-06-10 17:15:00")

            // Frijoles: devolución de cliente
            registrarMovimiento(frijoles, 15, "ENTRADA", "Compra", "2026-06-07 10:30:00")
            registrarMovimiento(frijoles, 5, "SALIDA", "Venta", "2026-06-08 13:00:00")
            registrarMovimiento(frijoles, 2, "ENTRADA", "Devolución de cliente", "2026-06-10 09:40:00")

            // Leche: devolución a proveedor
            registrarMovimiento(leche, 25, "ENTRADA", "Compra", "2026-06-08 07:50:00")
            registrarMovimiento(leche, 6, "SALIDA", "Venta", "2026-06-09 18:10:00")
            registrarMovimiento(leche, 4, "SALIDA", "Devolución a proveedor", "2026-06-10 11:30:00")

            // Pan: producto en cero
            registrarMovimiento(pan, 10, "ENTRADA", "Compra", "2026-06-09 06:45:00")
            registrarMovimiento(pan, 10, "SALIDA", "Venta", "2026-06-10 07:20:00")

            // Huevos: alto valor de inventario
            registrarMovimiento(huevos, 12, "ENTRADA", "Compra", "2026-06-06 09:10:00")
            registrarMovimiento(huevos, 2, "SALIDA", "Venta", "2026-06-10 14:00:00")

            // Papel: ajuste positivo
            registrarMovimiento(papel, 9, "ENTRADA", "Compra", "2026-06-08 10:00:00")
            registrarMovimiento(papel, 3, "ENTRADA", "Ajuste positivo", "2026-06-10 15:10:00")

            // Jabón: ajuste negativo y stock bajo
            registrarMovimiento(jabon, 14, "ENTRADA", "Compra", "2026-06-07 12:35:00")
            registrarMovimiento(jabon, 5, "SALIDA", "Venta", "2026-06-09 16:10:00")
            registrarMovimiento(jabon, 2, "SALIDA", "Ajuste negativo", "2026-06-10 09:05:00")

            // Pasta: stock suficiente
            registrarMovimiento(pasta, 16, "ENTRADA", "Compra", "2026-06-08 13:45:00")
            registrarMovimiento(pasta, 4, "SALIDA", "Venta", "2026-06-10 18:25:00")

            // Agua: muchas unidades, stock normal
            registrarMovimiento(agua, 60, "ENTRADA", "Compra", "2026-06-09 08:00:00")
            registrarMovimiento(agua, 22, "SALIDA", "Venta", "2026-06-10 11:00:00")

            // Galletas: ventas y daño
            registrarMovimiento(galletas, 30, "ENTRADA", "Compra", "2026-06-07 14:00:00")
            registrarMovimiento(galletas, 8, "SALIDA", "Venta", "2026-06-08 17:30:00")
            registrarMovimiento(galletas, 4, "SALIDA", "Daño / pérdida", "2026-06-10 19:00:00")

            // Detergente: devolución cliente y stock alto por valor
            registrarMovimiento(detergente, 7, "ENTRADA", "Compra", "2026-06-06 10:20:00")
            registrarMovimiento(detergente, 1, "SALIDA", "Venta", "2026-06-09 15:15:00")
            registrarMovimiento(detergente, 1, "ENTRADA", "Devolución de cliente", "2026-06-10 16:45:00")

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }
}