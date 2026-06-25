package com.vcsm;

import com.vcsm.service.BlockchainService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VcsmApplication {

    @Autowired(required = false)
    private BlockchainService blockchainService;

    @PostConstruct
    public void initBlockchain() {
        if (blockchainService != null) {
            blockchainService.initBlockchain();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(VcsmApplication.class, args);
    }
}
