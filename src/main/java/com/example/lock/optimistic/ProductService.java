package com.example.lock.optimistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void updateProductPriceV1(Long productId, Double newPrice) {
        // 기존 데이터 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        // 가격 변경
        product.setPrice(newPrice);

        // 저장 시 버전 충돌이 발생하면 예외 발생
        productRepository.save(product);
    }

    @Transactional
    @Retryable(value = ObjectOptimisticLockingFailureException.class, maxAttempts = 1, backoff = @Backoff(delay = 100))
    public void updateProductPriceV2(Long productId, Double newPrice) {
        // 기존 데이터 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        // 가격 변경
        product.setPrice(newPrice);

        // 저장 시 버전 충돌이 발생하면 예외 발생
        productRepository.save(product);
    }

    @Recover
    public void recover(ObjectOptimisticLockingFailureException e, Long productId, Double newPrice) {
        log.error("최대 재시도 횟수를 초과했습니다. 예외 메시지: {}", e.getMessage());
        log.error("상품 {}를 {}원으로 변경하지 못 했습니다.", productId, newPrice);
    }
}
