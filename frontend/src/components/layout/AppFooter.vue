<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <footer class="app-footer">
    <div class="container py-5">
      <div class="row g-4">
        <div class="col-12 col-md-4">
          <RouterLink class="footer-brand" to="/"
            ><span aria-hidden="true">D</span>DINO-GO</RouterLink
          >
          <p class="mt-3 footer-copy">
            讓消費者安心選物，也讓商家專注經營。平台交易、訂單與售後資訊清楚可查。
          </p>
          <div class="d-flex gap-2" aria-label="社群連結">
            <a href="#instagram" class="social-link" aria-label="Instagram"
              ><i class="bi bi-instagram" aria-hidden="true"></i
            ></a>
            <a href="#facebook" class="social-link" aria-label="Facebook"
              ><i class="bi bi-facebook" aria-hidden="true"></i
            ></a>
            <a href="#line" class="social-link" aria-label="LINE"
              ><i class="bi bi-chat" aria-hidden="true"></i
            ></a>
          </div>
        </div>
        <div class="col-6 col-md-2">
          <h2 class="footer-title">購物協助</h2>
          <a href="#shipping" class="footer-link">配送說明</a
          ><a href="#payment" class="footer-link">付款方式</a
          ><a href="#returns" class="footer-link">退換貨政策</a>
        </div>
        <div class="col-6 col-md-2">
          <h2 class="footer-title">客服與平台</h2>
          <a href="#contact" class="footer-link">聯絡客服</a
          ><a href="#about" class="footer-link">關於 DinoGo</a
          ><a href="#terms" class="footer-link">服務條款</a>
        </div>
        <div class="col-6 col-md-2">
          <h2 class="footer-title">商家服務</h2>
          <RouterLink to="/seller/dashboard" class="footer-link">商家中心</RouterLink
          ><a href="#seller-help" class="footer-link">商家說明</a
          ><a href="#seller-contact" class="footer-link">商家客服</a>
        </div>
      </div>
      <div
        class="footer-bottom d-flex flex-column flex-md-row justify-content-between gap-3 mt-5 pt-3"
      >
        <span>© 2026 DINO-GO. All rights reserved.</span>
        <nav class="d-flex flex-wrap gap-3" aria-label="Legal navigation">
          <a href="#privacy" class="footer-legal-link">隱私權政策</a>
          <a href="#terms" class="footer-legal-link">服務條款</a>
          <a href="#usage" class="footer-legal-link">網站使用規範</a>
        </nav>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  color: var(--color-secondary-100);
  background: var(--color-primary-900);
}
.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-surface);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  text-decoration: none;
}
.footer-brand span {
  display: grid;
  width: 36px;
  height: 36px;
  color: var(--color-primary-800);
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
}
.footer-copy,
.footer-bottom {
  color: var(--color-secondary-400);
  font-size: var(--font-size-sm);
}
.footer-title {
  margin-bottom: var(--space-3);
  color: var(--color-surface);
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.footer-link {
  display: block;
  width: fit-content;
  margin-bottom: var(--space-2);
  color: var(--color-secondary-100);
  font-size: var(--font-size-sm);
  text-decoration: none;
}
.social-link {
  display: grid;
  width: 36px;
  height: 36px;
  color: var(--color-secondary-100);
  place-items: center;
  border: 1px solid var(--color-primary-700);
  border-radius: var(--radius-md);
  text-decoration: none;
}
.footer-legal-link {
  color: var(--color-secondary-400);
  text-decoration: none;
}
.footer-link:hover,
.footer-link:focus-visible,
.footer-brand:hover,
.footer-brand:focus-visible,
.social-link:hover,
.social-link:focus-visible,
.footer-legal-link:hover,
.footer-legal-link:focus-visible {
  color: var(--color-surface);
  text-decoration: underline;
}
.footer-bottom {
  border-top: 1px solid var(--color-primary-700);
}
</style>
