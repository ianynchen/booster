package io.github.booster.example.order;

import io.github.booster.commons.util.EitherUtil;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.LineItem;
import io.github.booster.example.dto.Order;
import io.github.booster.example.order.component.InventoryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Profile("client_mock_test")
public class InventoryClientMockConfiguration {

    @Bean
    @Primary
    public InventoryClient inventoryClient() {
        InventoryClient inventoryClient = mock(InventoryClient.class);
        when(inventoryClient.invoke(any(Order.class)))
                .thenAnswer(answer -> {
                    Order order = answer.getArgument(0, Order.class);
                    CheckoutResult result = new CheckoutResult();
                    result.setItems(new ArrayList<>());

                    for (LineItem lineItem : order.getItems()) {
                        CheckoutResult.Item item = new CheckoutResult.Item();
                        item.setProduct(lineItem.getProduct());

                        if (lineItem.getProduct().getId().equals("z")) {
                            item.setInStock(false);
                        } else {
                            item.setInStock(true);
                        }
                        result.getItems().add(item);
                    }
                    return Mono.just(EitherUtil.convertData(result));
                });
        return inventoryClient;
    }
}
