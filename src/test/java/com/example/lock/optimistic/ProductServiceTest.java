package com.example.lock.optimistic;

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
    public void verifyOptimisticLockWithConcurrency() throws InterruptedException {
        // 초기 데이터 설정
        Product product = new Product();
        product.setName("Product 1");
        product.setPrice(100.0);
        productRepository.save(product);

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

        Thread thread1 = new Thread(() -> {
            try {
                productService.updateProductPrice(product.getId(), 200.0);
            } catch (Exception e) {
                exceptionHolder.set(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                productService.updateProductPrice(product.getId(), 300.0);
            } catch (Exception e) {
                exceptionHolder.set(e);
            }
        });

        // 두 스레드를 동시에 실행
        thread1.start();
        thread2.start();

        // 두 스레드가 종료될 때까지 대기
        thread1.join();
        thread2.join();

        // 예외 발생 검증
        assertInstanceOf(ObjectOptimisticLockingFailureException.class, exceptionHolder.get(), "ObjectOptimisticLockingFailureException이 발생해야 합니다.");
    }
}

