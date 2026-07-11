package com.vcsm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.annotation.Bean;
import org.springframework.context.annotation.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class BlockchainConfig {

    @Value("${blockchain.rpc.url:http://localhost:8545}")
    private String rpcUrl;

    @Value("${blockchain.chain.id:1337}")
    private Long chainId;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public Long getChainId() {
        return chainId;
    }
}