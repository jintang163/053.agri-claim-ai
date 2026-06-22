package com.agri.claim.image;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.agri.claim.image.mapper")
@SpringBootApplication(scanBasePackages = {"com.agri.claim.image", "com.agri.claim.common"})
public class ImageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║   Agri Claim Image Service Started Successfully     ║
                ║   农业保险快速定损系统 - 影像管理服务启动成功          ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
