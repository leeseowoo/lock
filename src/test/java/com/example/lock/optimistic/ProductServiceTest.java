package com.example.lock.optimistic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("버전 충돌이 발생하여 한 스레드는 실패하고 예외가 발생한다.")
    public void verifyOptimisticLockWithConcurrencyV1() throws InterruptedException {
        // 초기 데이터 설정
        Product product = new Product();
        product.setName("Product 1");
        product.setPrice(100.0);
        productRepository.save(product);

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

        Thread thread1 = new Thread(() -> {
            try {
                productService.updateProductPriceV1(product.getId(), 200.0);
            } catch (Exception e) {
                exceptionHolder.set(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                productService.updateProductPriceV1(product.getId(), 300.0);
            } catch (Exception e) {
                exceptionHolder.set(e);
            }
        });

        thread1.start();    // 스레드 1 실행
        thread2.start();    // 스레드 2 실행

        thread1.join();     // 스레드 1 종료 대기
        thread2.join();     // 스레드 2 종료 대기

        // 예외 발생 검증
        assertInstanceOf(ObjectOptimisticLockingFailureException.class, exceptionHolder.get(), "ObjectOptimisticLockingFailureException이 발생해야 합니다.");
    }

    @Test
    @DisplayName("버전 충돌이 발생하여 재시도 후 재시도 횟수 초과로 실패 처리를 위한 Recover 메서드가 호출된다.")
    public void verifyOptimisticLockWithConcurrencyV2() throws InterruptedException {
        // 초기 데이터 설정
        Product product = new Product();
        product.setName("Product 1");
        product.setPrice(100.0);
        productRepository.save(product);

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

        Thread thread1 = new Thread(() -> {
            try {
                productService.updateProductPriceV2(product.getId(), 200.0);
            } catch (Exception e) {
                exceptionHolder.set(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                productService.updateProductPriceV2(product.getId(), 300.0);
            } catch (Exception e) {
                exceptionHolder.set(e);
            }
        });

        thread1.start();    // 스레드 1 실행
        thread2.start();    // 스레드 2 실행

        thread1.join();     // 스레드 1 종료 대기
        thread2.join();     // 스레드 2 종료 대기

        // log 확인
    }
}

