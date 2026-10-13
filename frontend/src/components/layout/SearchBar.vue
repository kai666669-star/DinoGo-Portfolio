<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps({
  compact: { type: Boolean, default: false },
})

const route = useRoute()
const router = useRouter()

const searchType = ref('product')
const keyword = ref(route.query.keyword || '')

// URL 的 keyword 改變時，同步搜尋框
watch(
  () => route.query.keyword,
  (newKeyword) => {
    keyword.value = newKeyword || ''
  },
)

const handleSearch = () => {
  const value = keyword.value.trim()

  if (!value) {
    return
  }

  if (searchType.value === 'product') {
    router.push({
      path: '/products',
      query: {
        keyword: value,
      },
    })
  } else {
    router.push({
      path: '/stores',
      query: {
        keyword: value,
      },
    })
  }
}
</script>

<template>
  <form
    class="search-bar"
    :class="{ 'search-bar--compact': compact }"
    role="search"
    @submit.prevent="handleSearch"
  >
    <label class="visually-hidden" for="site-search"> 搜尋 </label>

    <select v-model="searchType" class="search-category">
      <option value="product">搜尋商品</option>
      <option value="store">搜尋賣家</option>
    </select>

    <input
      id="site-search"
      v-model="keyword"
      class="search-input"
      type="text"
      :placeholder="searchType === 'product' ? '搜尋商品' : '搜尋賣家'"
    />

    <button class="btn search-submit" type="submit" aria-label="搜尋">
      <i class="bi bi-search" aria-hidden="true"></i>
    </button>
  </form>
</template>

<style scoped>
.search-bar {
  display: flex;
  width: 100%;
  max-width: 840px;
  min-height: 48px;
  border: 1px solid var(--color-primary-500);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.search-category {
  width: 112px;
  height: 46px;
  flex: 0 0 112px;
  padding-inline: var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  border: 0;
  border-right: 1px solid var(--color-border);
  border-radius: 0;
  background-color: var(--color-secondary-100);
}
.search-input {
  flex: 1;
  min-width: 0;
  height: 46px;
  padding-inline: var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  border: 0;
  border-left: 0;
  border-radius: 0;
}
.search-input::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}
.search-input:focus,
.search-category:focus {
  border-color: transparent;
  box-shadow: var(--shadow-focus);
}
.search-submit {
  width: 48px;
  flex: 0 0 48px;
  min-height: 46px;
  font-size: 20px;
  color: var(--color-surface);
  border: 0;
  border-radius: 0;
  background: var(--color-primary-500);
}
.search-submit:hover,
.search-submit:focus-visible {
  color: var(--color-surface);
  border-color: var(--color-primary-hover);
  background: var(--color-primary-hover);
}
.search-bar--compact .search-category {
  display: none;
}
.search-bar--compact .search-input {
  border-left: 0;
  border-radius: 0;
}

@media (max-width: 575.98px) {
  .search-bar {
    max-width: none;
  }
}
</style>
