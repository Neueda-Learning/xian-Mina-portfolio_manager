package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.model.Portfolio;
import com.mina.minaportfoliomanagement.repository.PortfolioRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    public Portfolio createPortfolio(String portfolioName) {
        String name = portfolioName == null ? "" : portfolioName.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "portfolioName is required");
        }
        if (name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "portfolioName is too long");
        }
        try {
            return portfolioRepository.save(name);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portfolio name already exists");
        }
    }

    /**
     * For backward compatibility, use My Portfolio by default when portfolio id is not provided.
     */
    public long requirePortfolioId(Long portfolioId) {
        long id = portfolioId == null ? PortfolioRepository.DEFAULT_PORTFOLIO_ID : portfolioId;
        portfolioRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Portfolio not found: " + id));
        return id;
    }
}
