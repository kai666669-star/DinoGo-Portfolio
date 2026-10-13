package com.dinogo.seller.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.dto.SellerWalletResponse;
import com.dinogo.seller.dto.SellerWalletTransactionResponse;
import com.dinogo.seller.dto.SellerWithdrawalResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.entity.WithdrawalRequest;
import com.dinogo.seller.entity.WithdrawalStatus;
import com.dinogo.seller.repository.SellerRepository;
import com.dinogo.seller.repository.WithdrawalRequestRepository;

@Service
public class SellerWalletService {

    private static final Set<WithdrawalStatus> DEDUCTED_WITHDRAWAL_STATUSES = Set.of(
            WithdrawalStatus.PROCESSING,
            WithdrawalStatus.PAID);

    private static final Set<OrderStatus> PENDING_BALANCE_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED);

    private final CurrentSellerService currentSellerService;
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    //sysmsg-start，總共2次修改，第1次//
    /**
     * 功能：SellerWalletService 不注入 sysmsg Entity 或 Repository。
     * 應用：保持單向依賴，未來若需要提款通知，改由 sysmsg 依賴本服務取得權威資料。
     */
    //sysmsg-end，總共2次修改，第1次//
    public SellerWalletService(
            CurrentSellerService currentSellerService,
            OrderRepository orderRepository,
            SellerRepository sellerRepository,
            WithdrawalRequestRepository withdrawalRequestRepository) {
        this.currentSellerService = currentSellerService;
        this.orderRepository = orderRepository;
        this.sellerRepository = sellerRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
    }

    @Transactional(readOnly = true)
    public SellerWalletResponse getWallet(Integer memberId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        return buildWallet(sellerId);
    }

    @Transactional
    public SellerWithdrawalResponse withdraw(Integer memberId) {
        Seller seller = sellerRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        if (!"ACTIVE".equals(seller.getStatus())) {
            throw new IllegalArgumentException("Seller is inactive.");
        }

        SellerWalletResponse wallet = buildWallet(seller.getSellerId());
        BigDecimal amount = wallet.availableBalance();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("目前沒有可提領金額。");
        }

        WithdrawalRequest withdrawal = withdrawalRequestRepository.save(
                new WithdrawalRequest(seller, amount));

        //sysmsg-start，總共2次修改，第2次//
        // 功能：提款交易只儲存 seller 資料，不寫入 sysmsg.send/record。
        // 應用：現階段不產生提款通知，也不建立反向 seller → sysmsg 依賴。
        //sysmsg-end，總共2次修改，第2次//
        return new SellerWithdrawalResponse(
                withdrawal.getWithdrawalId(),
                withdrawal.getAmount(),
                withdrawal.getStatus(),
                withdrawal.getRequestedAt(),
                buildWallet(seller.getSellerId()));
    }

    @Transactional(readOnly = true)
    public List<SellerWalletTransactionResponse> getTransactions(Integer memberId) {
        return getTransactions(memberId, null, null);
    }

    @Transactional(readOnly = true)
    public List<SellerWalletTransactionResponse> getTransactions(
            Integer memberId,
            LocalDate startDate,
            LocalDate endDate) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);

        if ((startDate == null) != (endDate == null)
                || (startDate != null && endDate.isBefore(startDate))) {
            throw new IllegalArgumentException("查詢結束日期不可早於開始日期，且日期不可為空");
        }

        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .filter(order -> order.getStatus() != OrderStatus.PENDING_PAYMENT)
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toTransactionResponse)
                .filter(transaction -> isTransactionInRange(transaction, startDate, endDate))
                .toList();
    }

    private SellerWalletResponse buildWallet(Integer sellerId) {
        List<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        BigDecimal completedBalance = sumOrdersByStatus(orders, OrderStatus.COMPLETED);
        BigDecimal withdrawnBalance = withdrawalRequestRepository.sumAmountBySellerIdAndStatusIn(
                sellerId,
                DEDUCTED_WITHDRAWAL_STATUSES);
        if (withdrawnBalance == null) {
            withdrawnBalance = BigDecimal.ZERO;
        }
        BigDecimal availableBalance = completedBalance.subtract(withdrawnBalance).max(BigDecimal.ZERO);
        BigDecimal pendingBalance = orders.stream()
                .filter(order -> PENDING_BALANCE_STATUSES.contains(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerWalletResponse(
                availableBalance,
                pendingBalance,
                withdrawnBalance,
                "台新銀行",
                "0908",
                true);
    }

    private BigDecimal sumOrdersByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(order -> order.getStatus() == status)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SellerWalletTransactionResponse toTransactionResponse(Order order) {
        boolean completed = order.getStatus() == OrderStatus.COMPLETED;

        return new SellerWalletTransactionResponse(
                order.getOrderId(),
                order.getOrderNo(),
                "ORDER_INCOME",
                "income",
                order.getTotalAmount(),
                completed ? "AVAILABLE" : "PENDING",
                completed && order.getCompletedAt() != null
                        ? order.getCompletedAt()
                        : order.getCreatedAt());
    }

    private boolean isTransactionInRange(
            SellerWalletTransactionResponse transaction,
            LocalDate startDate,
            LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return true;
        }

        LocalDate occurredDate = transaction.occurredAt().toLocalDate();
        return !occurredDate.isBefore(startDate) && !occurredDate.isAfter(endDate);
    }
}
