<script setup>
defineProps({
  summaries: {
    type: Array,
    required: true,
  },
})
</script>

<template>
  <section class="member-summary" aria-label="今日會員摘要">
    <article v-for="summary in summaries" :key="summary.label" class="member-summary__item">
      <span class="member-summary__icon" aria-hidden="true">
        <i class="bi" :class="summary.icon"></i>
      </span>
      <div>
        <p class="member-summary__value">
          <strong>{{ summary.value }}</strong
          >{{ summary.label }}
        </p>
        <p class="member-summary__hint">{{ summary.hint }}</p>
      </div>
    </article>
  </section>
</template>

<style scoped>
.member-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.member-summary__item {
  display: flex;
  min-width: 0;
  min-height: 124px;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
}

.member-summary__item + .member-summary__item {
  border-left: 1px solid var(--color-border);
}

.member-summary__icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  color: var(--color-primary-active);
  font-size: 20px;
  place-items: center;
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
}

.member-summary__value,
.member-summary__hint {
  margin: 0;
}

.member-summary__value {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.member-summary__value strong {
  font-size: 24px;
}

.member-summary__hint {
  margin-top: 3px;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

@media (max-width: 767.98px) {
  .member-summary {
    grid-template-columns: 1fr;
  }
  .member-summary__item {
    min-height: 92px;
  }
  .member-summary__item + .member-summary__item {
    border-top: 1px solid var(--color-border);
    border-left: 0;
  }
}
</style>
