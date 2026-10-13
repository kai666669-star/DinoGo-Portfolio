<script setup>
defineProps({
  code: { type: String, required: true },
  label: { type: String, required: true },
  heading: { type: String, required: true },
  description: { type: String, required: true },
})
</script>

<template>
  <main class="error-page" :aria-labelledby="`error-${code}-title`">
    <section class="error-page__card dg-card">
      <p class="error-page__code" aria-hidden="true">{{ code }}</p>
      <p class="error-page__label">{{ label }}</p>
      <h1 :id="`error-${code}-title`">{{ heading }}</h1>
      <p class="error-page__description">{{ description }}</p>
      <div class="error-page__actions"><slot name="actions" /></div>
    </section>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100vh;
  padding: 36px 24px 72px;
  place-items: start center;
}

.error-page__card {
  display: flex;
  width: min(100%, 354px);
  min-height: 412px;
  flex-direction: column;
  align-items: center;
  padding: 58px var(--space-5) var(--space-6);
  text-align: center;
}

.error-page__code {
  margin: 0;
  color: var(--color-primary-700);
  font-family: var(--font-heading);
  font-size: 128px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 0.9;
}

.error-page__label {
  margin: var(--space-4) 0 var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  font-weight: 500;
}

.error-page__card h1 {
  margin: 0;
  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.error-page__description {
  margin: var(--space-4) 0 var(--space-6);
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  line-height: var(--line-height-base);
}

.error-page__actions { margin-top: auto; }

:deep(.error-page__button) {
  display: inline-flex;
  min-width: 114px;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-4);
  font-size: var(--font-size-base);
  text-decoration: none;
  border-radius: var(--radius-md);
}

@media (max-width: 375px) {
  .error-page { padding-inline: var(--space-4); }
  .error-page__card { padding-inline: var(--space-4); }
  .error-page__code { font-size: 112px; }
  .error-page__card h1 { font-size: 22px; }
}
</style>
