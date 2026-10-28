package com.example.smartinventorypos.controller;

import com.example.smartinventorypos.dto.SaleRequest;
import com.example.smartinventorypos.dto.SaleResponse;
import com.example.smartinventorypos.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public ResponseEntity<List<SaleResponse>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @PostMapping
    public ResponseEntity<SaleResponse> checkout(@Valid @RequestBody SaleRequest request) {
        SaleResponse sale = saleService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }
}
