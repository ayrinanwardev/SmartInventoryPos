package com.example.smartinventorypos.dto;

import java.util.List;

public class SaleResponse {

    private Long id;
    private double total;
    private String createdAt;
    private List<SaleLineResponse> items;

    public SaleResponse() {
    }

    public SaleResponse(Long id, double total, String createdAt, List<SaleLineResponse> items) {
        this.id = id;
        this.total = total;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<SaleLineResponse> getItems() {
        return items;
    }

    public void setItems(List<SaleLineResponse> items) {
        this.items = items;
    }
}
