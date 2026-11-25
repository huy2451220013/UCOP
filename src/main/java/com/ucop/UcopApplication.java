package com.ucop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UcopApplication {
    public static void main(String[] args) {
        SpringApplication.run(UcopApplication.class, args);
        System.out.println("\n==============================================================");
        System.out.println("  UCOP Platform da khoi dong thanh cong!");
        System.out.println("  Truy cap ngay tai: http://localhost:8088");
        System.out.println("  Tai khoan mau: admin/123456 | staff/123456 | customer1/123456");
        System.out.println("==============================================================\n");
    }
}
