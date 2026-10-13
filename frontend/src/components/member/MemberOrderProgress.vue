<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  order: {
    type: Object,
    default: null,
  },
  state: {
    type: String,
    default: 'ready',
  },
})

const orderLink = computed(() =>
  props.order?.id
    ? { name: 'MemberOrderDetail', params: { id: props.order.id } }
    : { name: 'MemberOrders' },
)
</script>

<template>
  <section class="order-progress" aria-labelledby="order-progress-title">
    <div v-if="state !== 'ready' || !order" class="order-progress__empty" :role="state === 'error' ? 'alert' : 'status'">
      <i class="bi bi-inbox" aria-hidden="true"></i>
      <div>
        <h2 id="order-progress-title">最近訂單進度</h2>
        <p v-if="state === 'error'">訂單資料暫時無法載入，請稍後再試。</p>
        <p v-else-if="state === 'loading'">訂單資料載入中。</p>
        <p v-else>目前沒有可顯示的訂單。</p>
      </div>
      <RouterLink class="order-progress__view" :to="orderLink">查看訂單</RouterLink>
    </div>
    <template v-else>
    <header class="order-progress__header">
      <div>
        <h2 id="order-progress-title">最近訂單進度</h2>
        <p>訂單 #{{ order.number }} · {{ order.date }}</p>
      </div>
      <span class="order-progress__status"
        ><i class="bi bi-truck" aria-hidden="true"></i>{{ order.status }}</span
      >
    </header>

    <ol class="order-progress__journey" aria-label="訂單配送進度">
      <li
        v-for="(step, index) in order.progress"
        :key="step.label"
        :class="{ 'is-complete': step.complete }"
      >
        <span class="order-progress__marker"
          ><i class="bi" :class="step.icon" aria-hidden="true"></i
        ></span>
        <strong>{{ step.label }}</strong
        ><small>{{ step.time }}</small>
        <span
          v-if="index < order.progress.length - 1"
          class="order-progress__connector"
          aria-hidden="true"
        ></span>
      </li>
    </ol>

    <div class="order-progress__product">
      <span class="order-progress__image" aria-hidden="true"><i class="bi bi-image"></i></span>
      <div>
        <strong>{{ order.productName }}</strong
        ><small>{{ order.sellerName }} · 數量 {{ order.quantity }} · 平台保障交易</small>
      </div>
      <p>
        <span>訂單金額</span><strong>{{ order.amount }}</strong>
      </p>
    </div>

    <footer class="order-progress__actions">
      <p>配送資訊由商家與物流服務同步更新</p>
      <div>
        <RouterLink class="order-progress__support" :to="{ name: 'MemberMessages' }"
          >聯絡客服</RouterLink
        ><RouterLink class="order-progress__view" :to="orderLink">查看訂單</RouterLink>
      </div>
    </footer>
    </template>
  </section>
</template>

<style scoped>
.order-progress {
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.order-progress__header,
.order-progress__actions,
.order-progress__actions > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
.order-progress__empty {
  display: flex;
  min-height: 180px;
  align-items: center;
  gap: var(--space-4);
  color: var(--color-text-muted);
}
.order-progress__empty > i {
  color: var(--color-text-subtle);
  font-size: 42px;
}
.order-progress__empty h2,
.order-progress__empty p {
  margin: 0;
}
.order-progress__empty h2 {
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
}
.order-progress__empty p {
  margin-top: var(--space-1);
  font-size: var(--font-size-sm);
}
.order-progress__empty .order-progress__view {
  margin-left: auto;
}
.order-progress__header h2,
.order-progress__header p,
.order-progress__product p,
.order-progress__actions p {
  margin: 0;
}
.order-progress__header h2 {
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
}
.order-progress__header p,
.order-progress__actions p {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.order-progress__status {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  color: var(--color-warning);
  font-size: var(--font-size-xs);
  font-weight: 600;
  background: var(--color-warning-soft);
  border-radius: var(--radius-sm);
}
.order-progress__journey {
  display: flex;
  margin: var(--space-3) 0;
  padding: var(--space-2) 6px;
  list-style: none;
}
.order-progress__journey li {
  position: relative;
  display: grid;
  min-width: 0;
  flex: 1;
  justify-items: center;
  gap: 5px;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-align: center;
}
.order-progress__journey strong {
  color: inherit;
  font-size: var(--font-size-xs);
}
.order-progress__journey small {
  font-size: 10px;
  white-space: nowrap;
}
.order-progress__marker {
  display: grid;
  width: 34px;
  height: 34px;
  color: var(--color-text-muted);
  place-items: center;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}
.order-progress__journey .is-complete {
  color: var(--color-text);
}
.order-progress__journey .is-complete .order-progress__marker {
  color: var(--color-surface);
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.order-progress__connector {
  position: absolute;
  top: 17px;
  left: calc(50% + 17px);
  width: calc(100% - 34px);
  height: 1px;
  background: var(--color-border);
}
.is-complete .order-progress__connector {
  background: var(--color-primary);
}
.order-progress__product {
  display: flex;
  min-height: 96px;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}
.order-progress__image {
  display: grid;
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  color: var(--color-primary);
  font-size: 22px;
  place-items: center;
  background: var(--color-bg-muted);
  border-radius: var(--radius-sm);
}
.order-progress__product > div {
  display: grid;
  min-width: 0;
  gap: 4px;
}
.order-progress__product > div strong {
  overflow: hidden;
  color: var(--color-text);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-progress__product small,
.order-progress__product p span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.order-progress__product p {
  display: grid;
  gap: 3px;
  margin-left: auto;
  text-align: right;
}
.order-progress__product p strong {
  color: var(--color-text);
  font-size: var(--font-size-base);
}
.order-progress__actions {
  margin-top: var(--space-4);
}
.order-progress__support,
.order-progress__view {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-decoration: none;
  border-radius: var(--radius-md);
}
.order-progress__support {
  color: var(--color-primary-active);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}
.order-progress__view {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}
.order-progress__support:hover {
  background: var(--color-primary-soft);
}
.order-progress__view:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}
.order-progress a:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
@media (max-width: 767.98px) {
  .order-progress {
    padding: var(--space-4);
  }
  .order-progress__journey {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    row-gap: var(--space-4);
  }
  .order-progress__connector {
    display: none;
  }
  .order-progress__product {
    align-items: flex-start;
  }
  .order-progress__product p {
    margin-left: 0;
    text-align: left;
  }
  .order-progress__actions {
    align-items: flex-start;
    flex-direction: column;
  }
  .order-progress__actions > div {
    width: 100%;
  }
  .order-progress__support,
  .order-progress__view {
    justify-content: center;
    flex: 1;
  }
}
@media (max-width: 479.98px) {
  .order-progress__header {
    align-items: flex-start;
    flex-direction: column;
  }
  .order-progress__empty {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .order-progress__empty .order-progress__view {
    width: 100%;
    justify-content: center;
    margin-left: 0;
  }
  .order-progress__product {
    display: grid;
    grid-template-columns: 56px minmax(0, 1fr);
  }
  .order-progress__image {
    width: 56px;
    height: 56px;
    flex-basis: 56px;
  }
  .order-progress__product p {
    grid-column: 2;
  }
  .order-progress__actions > div {
    flex-direction: column;
    align-items: stretch;
  }
  .order-progress__support,
  .order-progress__view {
    width: 100%;
  }
}
</style>
