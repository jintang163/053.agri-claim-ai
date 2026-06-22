package com.agri.claim.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.agri.claim.system.mapper")
@SpringBootApplication(scanBasePackages = {"com.agri.claim.system", "com.agri.claim.common"})
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║   Agri Claim System Service Started Successfully    ║
                ║   农业保险快速定损系统 - 系统管理服务启动成功          ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
