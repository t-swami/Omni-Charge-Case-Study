package com.omnicharge.payment_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @GetMapping("/api/users/profile/wallet")
    BigDecimal getWalletBalance(@RequestHeader("Authorization") String token);

    @PostMapping("/api/users/profile/wallet/update")
    Map<String, String> updateWalletBalance(
            @RequestHeader("Authorization") String token,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("isTopUp") boolean isTopUp);
}
