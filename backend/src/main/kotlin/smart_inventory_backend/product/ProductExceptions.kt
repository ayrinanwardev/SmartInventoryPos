package smart_inventory_backend.product

import smart_inventory_backend.common.ConflictException
import smart_inventory_backend.common.NotFoundException

class ProductNotFoundException(id: Long) : NotFoundException("PRODUCT_NOT_FOUND", "Product $id not found")

class ProductUnavailableException(name: String) :
    ConflictException("PRODUCT_UNAVAILABLE", "Product no longer available: $name")

class InsufficientStockException(name: String) :
    ConflictException("INSUFFICIENT_STOCK", "Insufficient stock: $name")

class StockBelowZeroException(currentStock: Int) :
    ConflictException("STOCK_BELOW_ZERO", "Stock cannot go below 0 (currently $currentStock)")

class BarcodeInUseException : ConflictException("BARCODE_IN_USE", "Barcode already used")
class NameInUseException : ConflictException("NAME_IN_USE", "Another active product already has this name")
