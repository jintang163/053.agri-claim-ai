package com.agri.claim.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.agri.claim.auth.feign")
@MapperScan("com.agri.claim.auth.mapper")
@SpringBootApplication(scanBasePackages = {"com.agri.claim.auth", "com.agri.claim.common"})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║   Agri Claim Auth Service Started Successfully      ║
                ║   农业保险快速定损系统 - 认证鉴权服务启动成功          ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
