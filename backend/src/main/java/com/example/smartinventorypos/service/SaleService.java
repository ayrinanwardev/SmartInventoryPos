package com.example.smartinventorypos.service;

import com.example.smartinventorypos.dto.SaleLineRequest;
import com.example.smartinventorypos.dto.SaleLineResponse;
import com.example.smartinventorypos.dto.SaleRequest;
import com.example.smartinventorypos.dto.SaleResponse;
import com.example.smartinventorypos.exception.InsufficientStockException;
import com.example.smartinventorypos.model.Product;
import com.example.smartinventorypos.model.Sale;
import com.example.smartinventorypos.model.SaleLine;
import com.example.smartinventorypos.repository.ProductRepository;
import com.example.smartinventorypos.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getAllSales() {
        return saleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SaleResponse getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found with id: " + id));
        return mapToResponse(sale);
    }

    @Transactional
    public SaleResponse checkout(SaleRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        Sale sale = new Sale(0.0);
        double total = 0.0;
        List<SaleLine> saleLines = new ArrayList<>();

        for (SaleLineRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid product or quantity in cart");
            }

            // Lock product row during stock deduction to prevent race conditions
            Product product = productRepository.findByIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + itemReq.getProductId()));

            if (product.getStock() < itemReq.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for '" + product.getName() + "'. Available: " + product.getStock() + ", requested: " + itemReq.getQuantity());
            }

            // Deduct stock
            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);

            double lineSubtotal = product.getPrice() * itemReq.getQuantity();
            total += lineSubtotal;

            SaleLine line = new SaleLine(product, product.getName(), itemReq.getQuantity(), product.getPrice(), lineSubtotal);
            sale.addItem(line);
        }

        sale.setTotal(total);
        Sale savedSale = saleRepository.save(sale);
        return mapToResponse(savedSale);
    }

    private SaleResponse mapToResponse(Sale sale) {
        List<SaleLineResponse> lineResponses = sale.getItems().stream()
                .map(line -> new SaleLineResponse(
                        line.getProduct() != null ? line.getProduct().getId() : null,
                        line.getProductName(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getSubtotal()
                ))
                .collect(Collectors.toList());

        String formattedDate = sale.getCreatedAt() != null ? sale.getCreatedAt().format(ISO_FORMATTER) : "";
        return new SaleResponse(sale.getId(), sale.getTotal(), formattedDate, lineResponses);
    }
}
