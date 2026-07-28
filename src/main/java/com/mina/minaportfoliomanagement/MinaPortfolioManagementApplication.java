package com.mina.minaportfoliomanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MinaPortfolioManagementApplication {

    public static void main(String[] args) {
        // 开启 Spring Boot，并允许 MarketPriceService 的定时同步任务执行。
        SpringApplication.run(MinaPortfolioManagementApplication.class, args);
    }

}
