package com.example.smartinventorypos;

import com.example.smartinventorypos.dto.ProductRequest;
import com.example.smartinventorypos.dto.SaleLineRequest;
import com.example.smartinventorypos.dto.SaleRequest;
import com.example.smartinventorypos.dto.SaleResponse;
import com.example.smartinventorypos.exception.InsufficientStockException;
import com.example.smartinventorypos.model.Product;
import com.example.smartinventorypos.service.ProductService;
import com.example.smartinventorypos.service.SaleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SmartInventoryPosApplicationTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private SaleService saleService;

    @Test
    void testCreateProductAndStockValidation() {
        ProductRequest request = new ProductRequest("Wireless Mouse", "SKU-101", 25.50, 10);
        Product product = productService.createProduct(request);

        assertNotNull(product.getId());
        assertEquals("Wireless Mouse", product.getName());
        assertEquals("SKU-101", product.getSku());
        assertEquals(25.50, product.getPrice());
        assertEquals(10, product.getStock());

        // Validate duplicate SKU rejection
        assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct(new ProductRequest("Another Mouse", "SKU-101", 30.0, 5))
        );

        // Validate negative price rejection
        assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct(new ProductRequest("Bad Price", "SKU-102", -5.0, 5))
        );

        // Validate negative stock rejection
        assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct(new ProductRequest("Bad Stock", "SKU-103", 10.0, -2))
        );
    }

    @Test
    void testCheckoutAndAutomaticStockDeduction() {
        Product p1 = productService.createProduct(new ProductRequest("Keyboard", "SKU-KEY", 50.0, 15));
        Product p2 = productService.createProduct(new ProductRequest("Monitor", "SKU-MON", 200.0, 5));

        SaleRequest checkoutReq = new SaleRequest(List.of(
                new SaleLineRequest(p1.getId(), 2),
                new SaleLineRequest(p2.getId(), 1)
        ));

        SaleResponse saleResponse = saleService.checkout(checkoutReq);

        assertNotNull(saleResponse.getId());
        assertEquals(300.0, saleResponse.getTotal());
        assertEquals(2, saleResponse.getItems().size());

        // Verify stock was automatically updated
        Product updatedP1 = productService.getProductById(p1.getId());
        Product updatedP2 = productService.getProductById(p2.getId());

        assertEquals(13, updatedP1.getStock());
        assertEquals(4, updatedP2.getStock());
    }

    @Test
    void testInsufficientStockRejection() {
        Product p1 = productService.createProduct(new ProductRequest("Headset", "SKU-HD", 80.0, 2));

        SaleRequest checkoutReq = new SaleRequest(List.of(
                new SaleLineRequest(p1.getId(), 5)
        ));

        assertThrows(InsufficientStockException.class, () -> saleService.checkout(checkoutReq));

        // Ensure stock remains unchanged
        Product updatedP1 = productService.getProductById(p1.getId());
        assertEquals(2, updatedP1.getStock());
    }

    @Test
    void testSalesHistoryAndInvoice() {
        Product p1 = productService.createProduct(new ProductRequest("USB Cable", "SKU-USB", 10.0, 20));

        SaleResponse sale1 = saleService.checkout(new SaleRequest(List.of(new SaleLineRequest(p1.getId(), 3))));
        SaleResponse sale2 = saleService.checkout(new SaleRequest(List.of(new SaleLineRequest(p1.getId(), 1))));

        List<SaleResponse> salesHistory = saleService.getAllSales();
        assertTrue(salesHistory.size() >= 2);

        SaleResponse retrievedSale = saleService.getSaleById(sale1.getId());
        assertEquals(sale1.getId(), retrievedSale.getId());
        assertEquals(30.0, retrievedSale.getTotal());
        assertEquals(1, retrievedSale.getItems().size());
        assertEquals("USB Cable", retrievedSale.getItems().get(0).getProductName());
        assertEquals(3, retrievedSale.getItems().get(0).getQuantity());
        assertEquals(10.0, retrievedSale.getItems().get(0).getUnitPrice());
        assertEquals(30.0, retrievedSale.getItems().get(0).getSubtotal());
    }
}
