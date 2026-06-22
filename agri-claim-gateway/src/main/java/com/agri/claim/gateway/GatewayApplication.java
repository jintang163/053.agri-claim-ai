package com.agri.claim.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.agri.claim.gateway", "com.agri.claim.common"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║   Agri Claim Gateway Service Started Successfully   ║
                ║   农业保险快速定损系统 - API网关服务启动成功          ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
