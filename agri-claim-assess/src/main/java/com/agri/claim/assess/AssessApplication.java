package com.agri.claim.assess;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.agri.claim.assess.mapper")
@SpringBootApplication(scanBasePackages = {"com.agri.claim.assess", "com.agri.claim.common"})
public class AssessApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssessApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║   Agri Claim Assess Service Started Successfully    ║
                ║   农业保险快速定损系统 - 定损评估服务启动成功            ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
