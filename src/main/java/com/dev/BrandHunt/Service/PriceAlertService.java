package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Constant.ErrorCode;
import com.dev.BrandHunt.Entity.Notification;
import com.dev.BrandHunt.Entity.PriceAlert;
import com.dev.BrandHunt.Entity.Product;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Repository.NotificationRepository;
import com.dev.BrandHunt.Repository.PriceAlertRepository;
import com.dev.BrandHunt.Repository.ProductRepository;
import com.dev.BrandHunt.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {
    private final PriceAlertRepository priceAlertRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public boolean isPriceAlertEnabled(Long productId, Long userId) {
        return priceAlertRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    @Transactional
    public boolean togglePriceAlert(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return priceAlertRepository.findByUserIdAndProductId(userId, productId)
                .map(alert -> {
                    priceAlertRepository.delete(alert);
                    return false;
                })
                .orElseGet(() -> {
                    priceAlertRepository.save(new PriceAlert(user, product));
                    return true;
                });
    }

    @Transactional
    public void createPriceChangeNotifications(Product product, String previousSalePrice, String currentSalePrice) {
        List<PriceAlert> alerts = priceAlertRepository.findByProductId(product.getId());

        String message = product.getName() + "의 할인가가 " +
                previousSalePrice + "원에서 " + currentSalePrice + "원으로 변경되었습니다.";

        for (PriceAlert alert : alerts) {
            notificationRepository.save(new Notification(alert.getUser(), product, message));
        }
    }
}
