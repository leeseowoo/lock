package com.example.lock.pessimistic;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ItemServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ItemServiceTest.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void verifyPessimisticLockWithConcurrency() throws InterruptedException {
        //  초기 데이터 설정
        logger.info("초기 아이템 데이터를 설정합니다.");
        Item item = new Item();
        item.setName("Item 1");
        item.setQuantity(50);
        itemRepository.save(item);

        ExecutorService executor = Executors.newFixedThreadPool(2); // 스레드 풀에 2개의 스레드 생성

        CountDownLatch startLatch = new CountDownLatch(1);  // 모든 스레드가 준비될 때까지 대기
        CountDownLatch endLatch = new CountDownLatch(2);    // 두 개의 스레드가 종료될 때까지 대기

        executor.execute(() -> {
            try {
                startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                logger.info("스레드 1: -30 수량 감소 시도");
                itemService.updateItemQuantity(item.getId(), -30);
                logger.info("스레드 1: -30 수량 감소 완료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IllegalArgumentException e) {
                logger.error("스레드 1: {}", e.getMessage());
            } finally {
                endLatch.countDown();   // 스레드 1 종료 알림
            }
        });

        executor.execute(() -> {
            try {
                startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                logger.info("스레드 2: -25 수량 감소 시도");
                itemService.updateItemQuantity(item.getId(), -25);
                logger.info("스레드 2: -25 수량 감소 완료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IllegalArgumentException e) {
                logger.error("스레드 2: {}", e.getMessage());
            }finally {
                endLatch.countDown();   // 스레드 2 종료 알림
            }
        });

        startLatch.countDown(); // 모든 스레드가 준비되었음을 알리고 동시에 실행
        endLatch.await();   // 모든 스레드가 종료될 때까지 대기, 스레드가 종료될 때까지 기다리지 않으면 정확한 결과를 보장할 수 없음. 왜냐하면 워킹 스레드가 종료되기 전에 메인 스레드가 실행될 수 있기 때문
        executor.shutdown();    // 스레드 풀 종료

        //  최종 재고 확인
        Item updatedItem = itemService.findItemById(item.getId());
        logger.info("최종 아이템 수량: " + updatedItem.getQuantity());

        //  초기 재고 50에서 -30 또는 -25만 반영
        //  비관적 락을 사용하여 동시 실행 시, 하나의 스레드는 실패하고 예외 발생
        assertTrue(updatedItem.getQuantity() == 20 || updatedItem.getQuantity() == 25, "최종 재고는 20 또는 25 입니다.");
    }

}

/*

1. startLatch.await(): 각 스레드는 startLatch가 카운트가 0이 될 때까지 대기합니다.

2. startLatch.countDown(): 모든 스레드가 준비되면 startLatch.countDown()을 호출하여 각 스레드가 동시에 실행되도록 합니다.

3. endLatch.await(): 메인 스레드는 endLatch.await()을 호출하여 두 개의 스레드가 종료될 때까지 기다립니다.

4. 각 스레드에서 endLatch.countDown() 호출: 각 스레드는 작업을 완료한 후 endLatch.countDown()을 호출하여 종료를 알립니다.

5. endLatch.countDown()이 두 번 호출되면 endLatch.await()이 끝나고 메인 스레드는 이후 검증을 진행합니다.

 */