package com.agri.claim.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.agri.claim.ai.mapper")
@SpringBootApplication(scanBasePackages = {"com.agri.claim.ai", "com.agri.claim.common"})
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║   Agri Claim AI Service Started Successfully        ║
                ║   农业保险快速定损系统 - AI智能服务启动成功             ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
