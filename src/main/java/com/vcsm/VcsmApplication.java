package com.vcsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@Autowired
private BlockchainService blockchainService;

@PostConstruct
public void initBlockchain() {
    blockchainService.initBlockchain();
}
@SpringBootApplication
@EnableScheduling
public class VcsmApplication {
    public static void main(String[] args) {
        SpringApplication.run(VcsmApplication.class, args);
    }
}
