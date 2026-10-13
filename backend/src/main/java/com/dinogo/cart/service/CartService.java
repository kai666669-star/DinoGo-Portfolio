package com.dinogo.cart.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.dto.CartItemRequest;
import com.dinogo.cart.dto.CartItemResponse;
import com.dinogo.cart.dto.CartResponse;
import com.dinogo.cart.dto.SkuOptionResponse;
import com.dinogo.cart.entity.Cart;
import com.dinogo.cart.entity.CartItem;
import com.dinogo.cart.repository.CartItemRepository;
import com.dinogo.cart.repository.CartRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
@Transactional
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductSkuRepository productSkuRepository;
	private final MemberRepository memberRepository;

	public CartService(
			ProductSkuRepository productSkuRepository,
			MemberRepository memberRepository,
			CartRepository cartRepository,
			CartItemRepository cartItemRepository) {

		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.productSkuRepository = productSkuRepository;
		this.memberRepository = memberRepository;
	}

	// =========================================================
	// 取得 / 建立購物車
	// =========================================================

	public Cart getOrCreateCart(String email) {

		Member member = memberRepository
				.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		return cartRepository
				.findByMember(member)
				.orElseGet(() -> {

					Cart cart = new Cart();
					cart.setMember(member);

					return cartRepository.save(cart);
				});
	}

	// =========================================================
	// 取得購物車
	// =========================================================

	public CartResponse getCart(String email) {

		Cart cart = getOrCreateCart(email);

		List<CartItemResponse> items = cart.getCartItems()
				.stream()
				.map(this::toResponse)
				.toList();

		return new CartResponse(
				cart.getCartId(),
				items);
	}

	// =========================================================
	// 新增商品
	// =========================================================

	public CartItemResponse addItem(
			String email,
			Integer skuId,
			Integer quantity) {

		if (quantity == null || quantity <= 0) {
			throw new RuntimeException(
					"商品數量必須大於 0");
		}

		Cart cart = getOrCreateCart(email);

		ProductSku sku = productSkuRepository
				.findById(skuId)
				.orElseThrow(() -> new RuntimeException("商品不存在"));

		Product product = sku.getProduct();

		// 商品是否上架
		if (!Byte.valueOf((byte) 1)
				.equals(product.getStatus())) {

			throw new RuntimeException(
					"商品目前未上架");
		}

		// SKU 是否啟用
		if (!Byte.valueOf((byte) 1)
				.equals(sku.getStatus())) {

			throw new RuntimeException(
					"此商品規格目前未啟用");
		}

		CartItem item = cartItemRepository
				.findByCartCartIdAndProductSkuSkuId(
						cart.getCartId(),
						skuId);

		int currentQuantity = 0;

		if (item != null) {
			currentQuantity = item.getQuantity();
		}

		int newQuantity = currentQuantity + quantity;

		// 檢查庫存
		if (newQuantity > sku.getStock()) {

			throw new RuntimeException(
					"商品庫存不足，目前剩餘 "
							+ sku.getStock()
							+ " 件");
		}

		if (item != null) {

			item.setQuantity(newQuantity);

		} else {

			item = new CartItem();

			item.setCart(cart);
			item.setProductSku(sku);
			item.setQuantity(quantity);
		}

		item = cartItemRepository.save(item);

		return toResponse(item);
	}

	// =========================================================
	// 修改購物車數量
	// =========================================================

	public CartItemResponse updateQuantity(
			String email,
			Integer cartItemId,
			CartItemRequest dto) {

		Integer quantity = dto.quantity();
		Integer skuId = dto.skuId();

		// =========================
		// 數量驗證
		// =========================

		if (quantity == null || quantity <= 0) {
			throw new RuntimeException("商品數量必須大於 0");
		}

		// =========================
		// 找會員
		// =========================

		Member member = memberRepository
				.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		// =========================
		// 找購物車商品
		// =========================

		CartItem item = cartItemRepository
				.findById(cartItemId)
				.orElseThrow(() -> new RuntimeException("購物車商品不存在"));

		// =========================
		// 確認是否為本人購物車
		// =========================

		if (!item.getCart()
				.getMember()
				.getMemberId()
				.equals(member.getMemberId())) {

			throw new RuntimeException("無權限修改此購物車商品");
		}

		// =========================================================
		// 如果有傳 skuId，處理 SKU 修改
		// =========================================================

		if (skuId != null) {

			ProductSku oldSku = item.getProductSku();

			Product oldProduct = oldSku.getProduct();

			// =========================
			// 找新的 SKU
			// =========================

			ProductSku newSku = productSkuRepository
					.findById(skuId)
					.orElseThrow(() -> new RuntimeException(
							"商品規格不存在"));

			// =========================
			// 確認是不是同一個商品
			// =========================

			if (!newSku.getProduct()
					.getProductId()
					.equals(oldProduct.getProductId())) {

				throw new RuntimeException(
						"不能更換其他商品的規格");
			}

			// =====================================================
			// 如果 SKU 根本沒有改變
			// =====================================================

			if (oldSku.getSkuId().equals(newSku.getSkuId())) {

				// 檢查庫存
				if (newSku.getStock() == null
						|| quantity > newSku.getStock()) {

					throw new RuntimeException(
							"商品庫存不足，目前剩餘 "
									+ newSku.getStock()
									+ " 件");
				}

				item.setQuantity(quantity);

				item = cartItemRepository.save(item);

				return toResponse(item);
			}

			// =========================
			// 新商品是否上架
			// =========================

			if (!Byte.valueOf((byte) 1)
					.equals(newSku.getProduct().getStatus())) {

				throw new RuntimeException(
						"商品目前未上架");
			}

			// =========================
			// 新 SKU 是否啟用
			// =========================

			if (!Byte.valueOf((byte) 1)
					.equals(newSku.getStatus())) {

				throw new RuntimeException(
						"此商品規格目前未啟用");
			}

			// =====================================================
			// ⭐ 找目前購物車是否已經有新的 SKU
			// =====================================================

			CartItem existingItem = cartItemRepository
					.findByCartCartIdAndProductSkuSkuId(
							item.getCart().getCartId(),
							newSku.getSkuId());

			// =====================================================
			// 情況 1：購物車已經有這個 SKU
			// =====================================================

			if (existingItem != null) {

				// 原本數量 + 目標 SKU 原本數量
				int mergedQuantity = existingItem.getQuantity() + item.getQuantity();

				// =========================
				// 檢查合併後庫存
				// =========================

				if (newSku.getStock() == null
						|| mergedQuantity > newSku.getStock()) {

					throw new RuntimeException(
							"合併後商品數量超過庫存，目前剩餘 "
									+ newSku.getStock()
									+ " 件");
				}

				// =========================
				// 合併數量
				// =========================

				existingItem.setQuantity(mergedQuantity);

				// =========================
				// 刪除原本 CartItem
				// =========================

				cartItemRepository.delete(item);

				// =========================
				// 儲存合併後商品
				// =========================

				existingItem = cartItemRepository.save(existingItem);

				return toResponse(existingItem);
			}

			// =====================================================
			// 情況 2：購物車沒有這個 SKU
			// =====================================================

			if (newSku.getStock() == null
					|| quantity > newSku.getStock()) {

				throw new RuntimeException(
						"商品庫存不足，目前剩餘 "
								+ newSku.getStock()
								+ " 件");
			}

			// 直接修改 SKU
			item.setProductSku(newSku);

			// 修改數量
			item.setQuantity(quantity);

			item = cartItemRepository.save(item);

			return toResponse(item);
		}

		// =========================================================
		// 沒有傳 skuId → 單純修改數量
		// =========================================================

		ProductSku sku = item.getProductSku();

		Product product = sku.getProduct();

		// =========================
		// 商品是否上架
		// =========================

		if (!Byte.valueOf((byte) 1)
				.equals(product.getStatus())) {

			throw new RuntimeException(
					"商品目前未上架");
		}

		// =========================
		// SKU 是否啟用
		// =========================

		if (!Byte.valueOf((byte) 1)
				.equals(sku.getStatus())) {

			throw new RuntimeException(
					"此商品規格目前未啟用");
		}

		// =========================
		// 庫存
		// =========================

		if (sku.getStock() == null
				|| quantity > sku.getStock()) {

			throw new RuntimeException(
					"商品庫存不足，目前剩餘 "
							+ sku.getStock()
							+ " 件");
		}

		// =========================
		// 更新數量
		// =========================

		item.setQuantity(quantity);

		item = cartItemRepository.save(item);

		return toResponse(item);
	}
	// =========================================================
	// 修改 SKU + 數量
	// =========================================================

	public CartItemResponse updateCartItem(
			Integer cartItemId,
			CartItemRequest request,
			Integer memberId) {

		if (request.quantity() == null
				|| request.quantity() <= 0) {

			throw new RuntimeException(
					"商品數量必須大於 0");
		}

		CartItem cartItem = cartItemRepository
				.findById(cartItemId)
				.orElseThrow(() -> new RuntimeException(
						"找不到購物車商品"));

		// 確認會員
		if (!cartItem.getCart()
				.getMember()
				.getMemberId()
				.equals(memberId)) {

			throw new RuntimeException(
					"無權修改此購物車商品");
		}

		// 找新的 SKU
		ProductSku sku = productSkuRepository
				.findById(request.skuId())
				.orElseThrow(() -> new RuntimeException(
						"找不到商品規格"));

		Product product = sku.getProduct();

		// 確認是不是同一商品
		if (!product.getProductId()
				.equals(
						cartItem.getProductSku()
								.getProduct()
								.getProductId())) {

			throw new RuntimeException(
					"不能更換成其他商品的規格");
		}

		// 商品是否上架
		if (!Byte.valueOf((byte) 1)
				.equals(product.getStatus())) {

			throw new RuntimeException(
					"商品目前未上架");
		}

		// SKU 是否啟用
		if (!Byte.valueOf((byte) 1)
				.equals(sku.getStatus())) {

			throw new RuntimeException(
					"此商品規格目前未啟用");
		}

		// 庫存
		if (request.quantity() > sku.getStock()) {

			throw new RuntimeException(
					"商品庫存不足，目前剩餘 "
							+ sku.getStock()
							+ " 件");
		}

		cartItem.setProductSku(sku);
		cartItem.setQuantity(request.quantity());

		cartItemRepository.save(cartItem);

		return toResponse(cartItem);
	}

	// =========================================================
	// 刪除單筆
	// =========================================================

	public void deleteItem(
			String email,
			Integer cartItemId) {

		Member member = memberRepository
				.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		CartItem item = cartItemRepository
				.findById(cartItemId)
				.orElseThrow(() -> new RuntimeException(
						"購物車商品不存在"));

		if (!item.getCart()
				.getMember()
				.getMemberId()
				.equals(member.getMemberId())) {

			throw new RuntimeException(
					"無權限刪除此購物車商品");
		}

		cartItemRepository.delete(item);
	}

	// =========================================================
	// 清空購物車
	// =========================================================

	public void clearCart(String email) {

		Member member = memberRepository
				.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		Cart cart = cartRepository
				.findByMember(member)
				.orElseThrow(() -> new RuntimeException(
						"購物車不存在"));

		List<CartItem> items = cartItemRepository
				.findByCartCartId(
						cart.getCartId());

		cartItemRepository.deleteAll(items);
	}

	// =========================================================
	// CartItem → Response
	// =========================================================

	private CartItemResponse toResponse(
			CartItem cartItem) {

		ProductSku sku = cartItem.getProductSku();

		Product product = sku.getProduct();

		// =====================================================
		// 商品目前是否可以購買
		// =====================================================

		boolean available = true;

		String unavailableReason = null;

		// 商品下架
		if (!Byte.valueOf((byte) 1)
				.equals(product.getStatus())) {

			available = false;

			unavailableReason = "商品已下架";

			// SKU 停用
		} else if (!Byte.valueOf((byte) 1)
				.equals(sku.getStatus())) {

			available = false;

			unavailableReason = "商品規格已停用";

			// 庫存不足
		} else if (sku.getStock() == null
				|| sku.getStock() < cartItem.getQuantity()) {

			available = false;

			unavailableReason = "庫存不足，目前剩餘 "
					+ sku.getStock()
					+ " 件";
		}

		// =====================================================
		// 商品主圖
		// =====================================================

		String productImage = product.getImages().stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsMain()))
				.findFirst()
				.or(() -> product.getImages().stream()
						.filter(image -> image.getSortOrder() != null)
						.min(Comparator.comparing(ProductImage::getSortOrder)))
				.map(ProductImage::getImageUrl)
				.orElse(null);

		// =====================================================
		// SKU 選項
		// =====================================================

		List<SkuOptionResponse> skus = product.getSkus()
				.stream()
				.map(item -> {

					StringBuilder skuName = new StringBuilder();

					if (item.getSpec1Name() != null
							&& item.getSpec1Value() != null) {

						skuName.append(
								item.getSpec1Name())
								.append("：")
								.append(
										item.getSpec1Value());
					}

					if (item.getSpec2Name() != null
							&& item.getSpec2Value() != null) {

						if (!skuName.isEmpty()) {
							skuName.append(" / ");
						}

						skuName.append(
								item.getSpec2Name())
								.append("：")
								.append(
										item.getSpec2Value());
					}

					return new SkuOptionResponse(
							item.getSkuId(),
							skuName.toString(),
							item.getPrice(),
							item.getStatus());
				})
				.toList();

		// =====================================================
		// Response
		// =====================================================

		return new CartItemResponse(
				cartItem.getCartItemId(),
				sku.getSkuId(),
				product.getProductId(),
				product.getProductName(),
				sku.getPrice(),
				cartItem.getQuantity(),
				sku.getStock(), // ⭐ 新增
				productImage,
				product.getSeller().getSellerId(),
				product.getSeller().getStoreName(),
				skus,
				available,
				unavailableReason);
	}
}