package com.example.smartinventorypos.service;

import com.example.smartinventorypos.dto.ProductRequest;
import com.example.smartinventorypos.model.Product;
import com.example.smartinventorypos.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        if (productRepository.existsBySku(request.getSku().trim())) {
            throw new IllegalArgumentException("Product with SKU '" + request.getSku().trim() + "' already exists");
        }

        Product product = new Product(
                request.getName().trim(),
                request.getSku().trim(),
                request.getPrice(),
                request.getStock()
        );
        return productRepository.save(product);
    }
}
