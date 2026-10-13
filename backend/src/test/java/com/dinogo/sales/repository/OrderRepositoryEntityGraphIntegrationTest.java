package com.dinogo.sales.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.catalog.entity.Brand;
import com.dinogo.catalog.entity.Category;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.entity.Subcategory;
import com.dinogo.member.entity.Member;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.seller.entity.Seller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

@SpringBootTest(properties = "jwt.secret=test-secret-for-jwt-context-only-32-bytes")
@Transactional
class OrderRepositoryEntityGraphIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void memberOrderListFetchesMultipleItemsWithoutDuplicateOrdersOrLazyLoadingFailure() {
        TestOrderData data = persistOrderWithTwoItems();
        entityManager.flush();
        entityManager.clear();

        List<Order> orders = orderRepository
                .findByBuyerIdOrderByCreatedAtDesc(data.buyerId());

        assertThat(orders)
                .extracting(Order::getOrderId)
                .containsExactly(data.orderId());

        Order fetchedOrder = orders.getFirst();
        assertThat(Persistence.getPersistenceUtil()
                .isLoaded(fetchedOrder, "orderItems"))
                .isTrue();

        entityManager.clear();

        assertThatCode(() -> fetchedOrder.getOrderItems().size())
                .doesNotThrowAnyException();
        assertThat(fetchedOrder.getOrderItems())
                .extracting(OrderItem::getSkuId)
                .containsExactlyInAnyOrder(data.firstSkuId(), data.secondSkuId());
    }

    private TestOrderData persistOrderWithTwoItems() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Member buyer = persistMember("buyer-" + unique + "@example.com");
        Member sellerMember = persistMember("seller-" + unique + "@example.com");

        Seller seller = new Seller();
        seller.setMember(sellerMember);
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(seller, "storeName", "EntityGraph " + unique);
        ReflectionTestUtils.setField(seller, "status", "ACTIVE");
        ReflectionTestUtils.setField(seller, "createdAt", now);
        ReflectionTestUtils.setField(seller, "updatedAt", now);
        entityManager.persist(seller);

        Category category = new Category();
        category.setCategoryName("EntityGraph " + unique);
        entityManager.persist(category);

        Subcategory subcategory = new Subcategory();
        subcategory.setCategory(category);
        subcategory.setSubcategoryName("EntityGraph " + unique);
        entityManager.persist(subcategory);

        Brand brand = new Brand();
        brand.setBrandName("EntityGraph " + unique);
        entityManager.persist(brand);

        Product product = new Product();
        product.setSeller(seller);
        product.setSubcategory(subcategory);
        product.setBrand(brand);
        product.setProductName("EntityGraph product");
        product.setBasePrice(new BigDecimal("100.00"));
        product.setStatus((byte) 1);
        product.setViewCount(0);
        product.setSoldCount(0);
        entityManager.persist(product);

        ProductSku firstSku = persistSku(product, "A");
        ProductSku secondSku = persistSku(product, "B");

        Order order = new Order();
        order.setOrderNo("TEST-EG-" + unique);
        order.setBuyerId(buyer.getMemberId());
        order.setSellerId(seller.getSellerId());
        order.setReceiverName("Integration Buyer");
        order.setReceiverPhone("0900000000");
        order.setShippingPostalCode("100");
        order.setShippingCity("Taipei");
        order.setShippingDistrict("Zhongzheng");
        order.setShippingDetailAddress("Integration test address");
        order.setStatus(OrderStatus.SHIPPED);
        order.setSubtotalAmount(new BigDecimal("200.00"));
        order.setShippingFee(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("200.00"));
        order.addOrderItem(orderItem(product, firstSku, "A"));
        order.addOrderItem(orderItem(product, secondSku, "B"));
        entityManager.persist(order);

        return new TestOrderData(
                buyer.getMemberId(),
                order.getOrderId(),
                firstSku.getSkuId(),
                secondSku.getSkuId());
    }

    private Member persistMember(String email) {
        Member member = new Member();
        member.setEmail(email);
        member.setPasswordHash("integration-test-password-hash");
        member.setLastName("Test");
        member.setFirstName("Member");
        member.setStatus("ACTIVE");
        entityManager.persist(member);
        return member;
    }

    private ProductSku persistSku(Product product, String specValue) {
        ProductSku sku = new ProductSku();
        sku.setProduct(product);
        sku.setSpec1Name("Type");
        sku.setSpec1Value(specValue);
        sku.setPrice(new BigDecimal("100.00"));
        sku.setStock(10);
        sku.setStatus((byte) 1);
        entityManager.persist(sku);
        return sku;
    }

    private OrderItem orderItem(Product product, ProductSku sku, String specValue) {
        OrderItem item = new OrderItem();
        item.setProductId(product.getProductId());
        item.setSkuId(sku.getSkuId());
        item.setProductName(product.getProductName());
        item.setSkuSpec("Type: " + specValue);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("100.00"));
        item.setIsReviewed(false);
        return item;
    }

    private record TestOrderData(
            Integer buyerId,
            Integer orderId,
            Integer firstSkuId,
            Integer secondSkuId) {
    }
}
