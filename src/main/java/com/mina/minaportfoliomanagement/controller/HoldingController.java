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

    /**
     * Creates the controller with the holding service dependency.
     */
    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    /**
     * Returns all holdings for the selected portfolio.
     */
    @GetMapping
    public List<HoldingView> getAllItems(@RequestParam(required = false) Long portfolioId){
        return holdingService.getAllItems(portfolioId);
    }

    /**
     * Returns a single holding by id, using default portfolio id when not provided.
     */
    @GetMapping("/{id}")
    public HoldingView getItem(@PathVariable long id, @RequestParam(required = false) Long portfolioId){
        long resolvedPortfolioId = portfolioId == null ? 1L : portfolioId;
        return holdingService.getItem(id, resolvedPortfolioId);
    }

    /**
     * Creates or merges a holding from a buy request.
     */
    @PostMapping
    public ResponseEntity<HoldingView> createItem(@RequestBody BuyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdingService.createItem(request));
    }

    /**
     * Deposits cash into the selected portfolio.
     */
    @PostMapping("/cash")
    public ResponseEntity<HoldingView> addCash(@RequestBody CashDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdingService.addCash(request));
    }

    /**
     * Sells part or all of a holding and returns HTTP 204 on success.
     */
    @PostMapping("/{id}/sell")
    public ResponseEntity<Void> sellItem(@PathVariable long id, @RequestParam(required = false) Long portfolioId,
                                         @RequestBody SellRequest request) {
        holdingService.sellItem(id, portfolioId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a holding by liquidating its full remaining quantity.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable long id, @RequestParam(required = false) Long portfolioId) {
        holdingService.deleteItem(id, portfolioId);
        return ResponseEntity.noContent().build();
    }

}
