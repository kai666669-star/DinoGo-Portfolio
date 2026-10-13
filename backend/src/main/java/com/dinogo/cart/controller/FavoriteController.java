// package com.dinogo.cart.controller;

// import java.util.List;

// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.dinogo.cart.dto.AddFavoriteRequest;
// import com.dinogo.cart.dto.FavoriteResponse;
// import com.dinogo.cart.service.FavoriteService;
// import com.dinogo.member.entity.Member;

// @RestController
// @RequestMapping("/api/favorites")
// public class FavoriteController {

// 	private final FavoriteService favoriteService;

// 	public FavoriteController(FavoriteService favoriteService) {
// 		this.favoriteService = favoriteService;
// 	}

// 	// 查詢收藏
// 	@GetMapping
// 	public ResponseEntity<List<FavoriteResponse>> getFavorites() {

// 		// 暫時測試會員
// 		Member member = new Member();
// 		member.setMemberId(1);

// 		List<FavoriteResponse> favorites = favoriteService.getFavorites(member);

// 		return ResponseEntity.ok(favorites);
// 	}
// 	//帳號
// //	@GetMapping
// //	public ResponseEntity<List<FavoriteResponse>> getFavorites(
// //	        Authentication authentication) {
// //
// //	    Integer memberId = Integer.valueOf(authentication.getName());
// //
// //	    Member member = new Member();
// //	    member.setMemberId(memberId);
// //
// //	    return ResponseEntity.ok(
// //	            favoriteService.getFavorites(member)
// //	    );
// //	}
// 	// 新增收藏
// 	@PostMapping
// 	public ResponseEntity<FavoriteResponse> addFavorite(
// 			@RequestBody AddFavoriteRequest request) {

// 		// 暫時測試會員
// 		Member member = new Member();
// 		member.setMemberId(1);

// 		FavoriteResponse response = favoriteService.addFavorite(member, request);

// 		return ResponseEntity.ok(response);
// 	}
// 	//帳號
// //	@PostMapping
// //	public ResponseEntity<FavoriteResponse> addFavorite(
// //	        @RequestBody AddFavoriteRequest request,
// //	        Authentication authentication) {
// //
// //	    Integer memberId = Integer.valueOf(authentication.getName());
// //
// //	    Member member = new Member();
// //	    member.setMemberId(memberId);
// //
// //	    return ResponseEntity.ok(
// //	            favoriteService.addFavorite(member, request)
// //	    );
// //	}
// 	// 取消收藏
// 	@DeleteMapping("/{productId}")
// 	public ResponseEntity<Void> removeFavorite(
// 	        @PathVariable Integer productId) {

// 	    Member member = new Member();
// 	    member.setMemberId(1);

// 	    favoriteService.removeFavorite(member, productId);

// 	    return ResponseEntity.noContent().build();
// 	}
// 	//帳號
// //	@DeleteMapping("/{productId}")
// //	public ResponseEntity<Void> removeFavorite(
// //	        @PathVariable Integer productId,
// //	        Authentication authentication) {
// //
// //	    Integer memberId = Integer.valueOf(authentication.getName());
// //
// //	    Member member = new Member();
// //	    member.setMemberId(memberId);
// //
// //	    favoriteService.removeFavorite(member, productId);
// //
// //	    return ResponseEntity.noContent().build();
// //	}
// }
package com.dinogo.cart.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.cart.dto.AddFavoriteRequest;
import com.dinogo.cart.dto.FavoriteResponse;
import com.dinogo.cart.service.FavoriteService;
import com.dinogo.member.entity.Member;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

	private final FavoriteService favoriteService;

	public FavoriteController(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	// 查詢收藏
	@GetMapping
	public ResponseEntity<List<FavoriteResponse>> getFavorites(
			Authentication authentication) {

		String email = authentication.getName();

		List<FavoriteResponse> favorites = favoriteService.getFavoritesByEmail(email);

		return ResponseEntity.ok(favorites);
	}

	// 新增收藏
	@PostMapping
	public ResponseEntity<FavoriteResponse> addFavorite(
			@RequestBody AddFavoriteRequest request,
			Authentication authentication) {

		String email = authentication.getName();

		FavoriteResponse response = favoriteService.addFavoriteByEmail(email, request);

		return ResponseEntity.ok(response);
	}

	// 取消收藏
	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> removeFavorite(
			@PathVariable Integer productId,
			Authentication authentication) {

		String email = authentication.getName();

		favoriteService.removeFavoriteByEmail(email, productId);

		return ResponseEntity.noContent().build();
	}
}