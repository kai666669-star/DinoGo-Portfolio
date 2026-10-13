package com.dinogo.cart.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.dto.AddFavoriteRequest;
import com.dinogo.cart.dto.FavoriteResponse;
import com.dinogo.cart.dto.FavoriteSkuResponse;
import com.dinogo.cart.entity.Favorite;
import com.dinogo.cart.repository.FavoriteRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
@Transactional
public class FavoriteService {
	private final FavoriteRepository favoriteRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;

	public FavoriteService(ProductRepository productRepository, FavoriteRepository favoriteRepository,
			MemberRepository memberRepository) {
		this.favoriteRepository = favoriteRepository;
		this.productRepository = productRepository;
		this.memberRepository = memberRepository;
	}

	// 查詢會員的所有收藏
	@Transactional(readOnly = true)
	public List<FavoriteResponse> getFavorites(Member member) {
		return favoriteRepository.findByMember(member).stream().map(this::toResponse).toList();
	}

	public List<FavoriteResponse> getFavoritesByEmail(String email) {

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		return getFavorites(member);
	}

	// 新增收藏
	public FavoriteResponse addFavorite(Member member, AddFavoriteRequest request) {

		// 真的從資料庫取得商品
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new RuntimeException("商品不存在"));

		// 商品已下架，不允許新增收藏
		if (!Byte.valueOf((byte) 1).equals(product.getStatus())) {
			throw new RuntimeException("商品已下架，無法加入收藏");
		}

		// 檢查是否已收藏
		Optional<Favorite> existing = favoriteRepository.findByMemberAndProduct_ProductId(
				member,
				request.productId());

		if (existing.isPresent()) {
			return toResponse(existing.get());
		}

		// 建立收藏
		Favorite favorite = new Favorite();
		favorite.setMember(member);
		favorite.setProduct(product);

		Favorite saved = favoriteRepository.save(favorite);

		return toResponse(saved);
	}

	public FavoriteResponse addFavoriteByEmail(
			String email,
			AddFavoriteRequest request) {

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		return addFavorite(member, request);
	}

	// 取消收藏
	public void removeFavorite(Member member, Integer productId) {

		Favorite favorite = favoriteRepository.findByMemberAndProduct_ProductId(member, productId)
				.orElseThrow(() -> new RuntimeException("收藏不存在"));

		favoriteRepository.delete(favorite);
	}

	public void removeFavoriteByEmail(
			String email,
			Integer productId) {

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		removeFavorite(member, productId);
	}

	private String buildSkuName(ProductSku sku) {

		StringBuilder sb = new StringBuilder();

		if (sku.getSpec1Value() != null
				&& !sku.getSpec1Value().isBlank()) {

			sb.append(sku.getSpec1Value());
		}

		if (sku.getSpec2Value() != null
				&& !sku.getSpec2Value().isBlank()) {

			if (sb.length() > 0) {
				sb.append(" / ");
			}

			sb.append(sku.getSpec2Value());
		}

		return sb.toString();
	}

	private FavoriteResponse toResponse(Favorite favorite) {

		Product product = favorite.getProduct();

		// ================================
		// 商品圖片
		// ================================

		String imageUrl = product.getImages()
				.stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsMain()))
				.findFirst()
				.or(() -> product.getImages().stream()
						.filter(image -> image.getSortOrder() != null)
						.min(Comparator.comparing(ProductImage::getSortOrder)))
				.map(ProductImage::getImageUrl)
				.orElse(null);

		// ================================
		// 所有 SKU
		// ================================

		List<FavoriteSkuResponse> skus = product.getSkus()
				.stream()
				.map(sku -> {

					String skuName = buildSkuName(sku);

					boolean skuAvailable = Byte.valueOf((byte) 1).equals(sku.getStatus())
							&& sku.getStock() != null
							&& sku.getStock() > 0;

					return new FavoriteSkuResponse(
							sku.getSkuId(),
							skuName,
							sku.getStatus(),
							sku.getStock(),
							skuAvailable);
				})
				.toList();

		// ================================
		// 商品是否可以購買
		//
		// 商品啟用
		// +
		// 至少一個 SKU 可以購買
		// ================================

		boolean available = Byte.valueOf((byte) 1).equals(product.getStatus())
				&& skus.stream()
						.anyMatch(FavoriteSkuResponse::available);

		// ================================
		// 回傳
		// ================================

		return new FavoriteResponse(
				favorite.getFavoriteId(),
				product.getProductId(),
				product.getProductName(),
				product.getBasePrice(),
				imageUrl,
				product.getStatus(),
				skus,
				available);
	}

}
