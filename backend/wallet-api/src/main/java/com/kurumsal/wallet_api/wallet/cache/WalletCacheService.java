package com.kurumsal.wallet_api.wallet.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletCacheService {

    private static final String PREFIX = "wallet:balance:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    public Optional<BigDecimal> getBalance(Long walletId) {
        String value = redisTemplate.opsForValue().get(PREFIX + walletId);
        return value == null ? Optional.empty() : Optional.of(new BigDecimal(value));
    }

    public void cacheBalance(Long walletId, BigDecimal balance) {
        redisTemplate.opsForValue().set(PREFIX + walletId, balance.toPlainString(), TTL);
    }

    public void evict(Long walletId) {
        redisTemplate.delete(PREFIX + walletId);
    }
}
