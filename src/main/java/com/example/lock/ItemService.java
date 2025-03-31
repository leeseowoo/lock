package com.example.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void updateItemQuantity(Long itemId, Integer quantityChange) {
        // 비관적 락을 사용하여 데이터 조회
        Item item = itemRepository.findByIdWithLock(itemId);

        // 현재 재고 수량이 변경하려는 수량보다 적은 경우 예외 발생
        if (item.getQuantity() + quantityChange < 0) {
            throw new IllegalArgumentException("현재 재고가 " + item.getQuantity() + "개 이므로, " + quantityChange + "개를 감소시킬 수 없습니다.");
        }

        // 재고 수량 변경
        item.setQuantity(item.getQuantity() + quantityChange);

        // 변경된 데이터 저장
        itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Item findItemById(Long itemId) {
        // 비관적 락 없이 데이터 조회
        return itemRepository.findById(itemId).orElse(null);
    }
}
