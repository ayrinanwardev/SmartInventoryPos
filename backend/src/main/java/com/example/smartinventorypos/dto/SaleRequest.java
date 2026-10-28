package com.example.smartinventorypos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SaleRequest {

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<SaleLineRequest> items;

    public SaleRequest() {
    }

    public SaleRequest(List<SaleLineRequest> items) {
        this.items = items;
    }

    public List<SaleLineRequest> getItems() {
        return items;
    }

    public void setItems(List<SaleLineRequest> items) {
        this.items = items;
    }
}
