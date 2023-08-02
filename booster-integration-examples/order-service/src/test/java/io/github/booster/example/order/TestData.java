package io.github.booster.example.order;

import io.github.booster.example.dto.LineItem;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.Product;

import java.util.List;

public interface TestData {

    static Order createGoodOrder() {
        Order order1 = new Order();

        Product product1 = new Product();
        product1.setId("a");
        product1.setName("a");
        product1.setUnitPrice(100);
        LineItem lineItem1 = new LineItem();
        lineItem1.setQuantity(2);
        lineItem1.setProduct(product1);

        order1.setItems(
                List.of(
                        lineItem1
                )
        );
        return order1;
    }

    static Order createBadOrder() {

        Product product1 = new Product();
        product1.setId("a");
        product1.setName("a");
        product1.setUnitPrice(100);
        LineItem lineItem1 = new LineItem();
        lineItem1.setQuantity(2);
        lineItem1.setProduct(product1);

        Product product2 = new Product();
        product2.setId("z");
        product2.setName("z");
        product2.setUnitPrice(100);
        LineItem lineItem2 = new LineItem();
        lineItem2.setQuantity(3);
        lineItem2.setProduct(product2);

        Order order2 = new Order();
        order2.setItems(
                List.of(
                        lineItem1,
                        lineItem2
                )
        );
        return order2;
    }
}
