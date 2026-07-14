package com.kurumsal.wallet_api.integration;

import com.kurumsal.wallet_api.audit.service.AuditService;
import com.kurumsal.wallet_api.infrastructure.exception.InsufficientBalanceException;
import com.kurumsal.wallet_api.transaction.service.IdempotencyService;
import com.kurumsal.wallet_api.user.domain.User;
import com.kurumsal.wallet_api.user.repository.UserRepository;
import com.kurumsal.wallet_api.wallet.cache.WalletCacheService;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import com.kurumsal.wallet_api.wallet.repository.WalletRepository;
import com.kurumsal.wallet_api.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Postgres concurrency test for the pessimistic-lock code path in {@link WalletService}
 * (backed by {@code WalletRepository#findByIdWithPessimisticLock}). Loads a minimal, hand-picked
 * Spring context (datasource/JPA/Flyway + WalletService/AuditService only) instead of the full
 * application context, so the test never needs a live Redis/RabbitMQ broker — Rabbit/Redis/Cache
 * autoconfiguration is excluded and the two Redis-backed collaborators are mocked out.
 */
@Testcontainers
@SpringBootTest(
        classes = WalletIntegrationTest.TestConfig.class,
        properties = {
                "spring.datasource.hikari.maximum-pool-size=60",
                "spring.flyway.enabled=true"
        }
)
class WalletIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("pulsepay")
                    .withUsername("pulsepay_user")
                    .withPassword("changeme");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WalletService walletService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private WalletCacheService walletCacheService;
    @MockitoBean
    private IdempotencyService idempotencyService;

    private Long walletId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("concurrency-" + UUID.randomUUID() + "@pulsepay.test")
                .name("Concurrency Test User")
                .build());
        Wallet wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .build());
        walletId = wallet.getId();
    }

    @Test
    void concurrentDeposits_pessimisticLockPreventsLostUpdates() throws InterruptedException {
        int threadCount = 50;
        BigDecimal depositAmount = BigDecimal.valueOf(100);

        RaceResult result = runConcurrently(threadCount,
                () -> walletService.deposit(walletId, depositAmount, null, "127.0.0.1"));

        assertThat(result.completedInTime).isTrue();
        assertThat(result.errors).isEmpty();
        assertThat(result.successCount.get()).isEqualTo(threadCount);

        BigDecimal expectedBalance = depositAmount.multiply(BigDecimal.valueOf(threadCount));
        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void concurrentWithdrawals_pessimisticLockPreventsOverdraft() throws InterruptedException {
        int threadCount = 50;
        BigDecimal startingBalance = BigDecimal.valueOf(1000);
        BigDecimal withdrawAmount = BigDecimal.valueOf(100);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.credit(startingBalance);
        walletRepository.save(wallet);

        RaceResult result = runConcurrently(threadCount,
                () -> walletService.withdraw(walletId, withdrawAmount, null, "127.0.0.1"));

        assertThat(result.completedInTime).isTrue();

        long expectedSuccesses = startingBalance.divide(withdrawAmount).longValue();
        assertThat(result.successCount.get()).isEqualTo((int) expectedSuccesses);
        assertThat(result.errors).hasSize(threadCount - (int) expectedSuccesses);
        assertThat(result.errors).allMatch(t -> t instanceof InsufficientBalanceException);

        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Fires {@code threadCount} tasks at (as close to) the same instant via a start latch,
     * so the pessimistic lock — not thread scheduling — is what serializes access to the wallet row.
     */
    private RaceResult runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    task.run();
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completedInTime = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        RaceResult result = new RaceResult();
        result.completedInTime = completedInTime;
        result.successCount = successCount;
        result.errors = errors;
        return result;
    }

    private static class RaceResult {
        boolean completedInTime;
        AtomicInteger successCount;
        List<Throwable> errors;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            RabbitAutoConfiguration.class,
            DataRedisAutoConfiguration.class,
            DataRedisReactiveAutoConfiguration.class,
            CacheAutoConfiguration.class
    })
    @EntityScan(basePackages = "com.kurumsal.wallet_api")
    @EnableJpaRepositories(basePackages = "com.kurumsal.wallet_api")
    @Import({WalletService.class, AuditService.class})
    static class TestConfig {
    }
}
