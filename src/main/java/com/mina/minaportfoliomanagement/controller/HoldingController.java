package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.dto.BuyRequest;
import com.mina.minaportfoliomanagement.dto.CashDepositRequest;
import com.mina.minaportfoliomanagement.dto.HoldingView;
import com.mina.minaportfoliomanagement.dto.SellRequest;
import com.mina.minaportfoliomanagement.service.HoldingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/portfolio-items")
public class HoldingController {
    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping
    public List<HoldingView> getAllItems(){
        return holdingService.getAllItems();
    }

    @GetMapping("/{id}")
    public HoldingView getItem(@PathVariable long id){
        return holdingService.getItem(id);
    }

    @PostMapping
    public ResponseEntity<HoldingView> createItem(@RequestBody BuyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdingService.createItem(request));
    }

    @PostMapping("/cash")
    public ResponseEntity<HoldingView> addCash(@RequestBody CashDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdingService.addCash(request));
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<Void> sellItem(@PathVariable long id, @RequestBody SellRequest request) {
        holdingService.sellItem(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable long id) {
        holdingService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
