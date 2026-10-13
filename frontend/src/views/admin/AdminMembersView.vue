<script setup>
import { onMounted, ref } from 'vue'
import { listAdminMembers, restoreMember, suspendMember } from '@/api/adminMemberApi'

const members = ref([])
const status = ref('')
const keyword = ref('')
const feedback = ref('')
const actionMember = ref(null)
const reason = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)

async function load() {
  feedback.value = ''
  isLoading.value = true
  try {
    const { data } = await listAdminMembers({
      status: status.value || undefined,
      keyword: keyword.value || undefined,
    })
    members.value = data
  } catch (error) {
    feedback.value = error.response?.data?.message || '會員資料載入失敗。'
  } finally {
    isLoading.value = false
  }
}

function openSuspend(member) {
  actionMember.value = member
  reason.value = ''
}

async function suspend() {
  if (!reason.value.trim() || !actionMember.value || isSubmitting.value) return
  isSubmitting.value = true
  try {
    await suspendMember(actionMember.value.memberId, reason.value.trim())
    actionMember.value = null
    await load()
  } catch (error) {
    feedback.value = error.response?.data?.message || '停權失敗。'
  } finally {
    isSubmitting.value = false
  }
}

async function restore(member) {
  if (!window.confirm(`確定恢復 ${member.email} 的帳號？`)) return
  try {
    await restoreMember(member.memberId)
    await load()
  } catch (error) {
    feedback.value = error.response?.data?.message || '恢復帳號失敗。'
  }
}

onMounted(load)
</script>

<template>
  <section class="admin-members" aria-labelledby="admin-members-title">
    <header class="admin-members__header">
      <div>
        <p class="admin-members__eyebrow">平台管理</p>
        <h1 id="admin-members-title">會員管理</h1>
        <p class="admin-members__description">搜尋會員、停權或恢復帳號。</p>
      </div>
    </header>

    <form class="admin-members__filters" @submit.prevent="load">
      <label class="admin-members__search" for="member-search">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input
          id="member-search"
          v-model.trim="keyword"
          type="search"
          placeholder="搜尋姓名、Email 或會員編號"
        />
      </label>
      <select v-model="status" aria-label="依帳號狀態篩選" @change="load">
        <option value="">全部狀態</option>
        <option value="ACTIVE">正常</option>
        <option value="SUSPENDED">已停權</option>
        <option value="DEACTIVATED">已註銷</option>
      </select>
      <button
        class="admin-members__search-submit dg-btn-primary dg-focus-ring"
        :disabled="isLoading"
        type="submit"
      >
        {{ isLoading ? '搜尋中' : '搜尋' }}
      </button>
    </form>

    <p v-if="feedback" class="admin-members__feedback" role="status">{{ feedback }}</p>

    <div class="admin-members__table-card">
      <div class="admin-members__row admin-members__row--header" role="row">
        <span role="columnheader">會員</span><span role="columnheader">角色</span
        ><span role="columnheader">狀態</span><span role="columnheader">註冊時間</span
        ><span role="columnheader">操作</span>
      </div>
      <div v-for="member in members" :key="member.memberId" class="admin-members__row" role="row">
        <span class="admin-members__identity" role="cell"
          ><strong>{{ member.lastName }}{{ member.firstName }}</strong
          ><small>{{ member.email }} · #{{ member.memberId }}</small></span
        >
        <span role="cell">{{ member.roles.join('、') || '—' }}</span>
        <span role="cell"
          ><em :class="member.status">{{ member.status }}</em></span
        >
        <span role="cell">{{
          member.createdAt ? new Date(member.createdAt).toLocaleDateString('zh-TW') : '—'
        }}</span>
        <span class="admin-members__actions" role="cell"
          ><button
            v-if="member.status === 'ACTIVE'"
            class="admin-members__suspend dg-focus-ring"
            type="button"
            @click="openSuspend(member)"
          >
            停權</button
          ><button
            v-else-if="member.status === 'SUSPENDED'"
            class="admin-members__restore dg-focus-ring"
            type="button"
            @click="restore(member)"
          >
            恢復</button
          ><span v-else>—</span></span
        >
      </div>
      <p
        v-if="!isLoading && !members.length && !feedback"
        class="admin-members__empty"
        role="status"
      >
        沒有符合條件的會員。
      </p>
    </div>

    <div
      v-if="actionMember"
      class="admin-members__modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="suspend-member-title"
    >
      <form class="admin-members__dialog" @submit.prevent="suspend">
        <h2 id="suspend-member-title">停權帳號</h2>
        <p>將立即使 {{ actionMember.email }} 的登入憑證失效。</p>
        <label><span>停權原因</span><textarea v-model="reason" required maxlength="500" /></label>
        <div class="admin-members__dialog-actions">
          <button
            class="admin-members__cancel dg-focus-ring"
            type="button"
            @click="actionMember = null"
          >
            取消</button
          ><button
            class="admin-members__suspend dg-focus-ring"
            :disabled="isSubmitting"
            type="submit"
          >
            確認停權
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.admin-members {
  display: grid;
  gap: var(--space-5);
  color: var(--color-text);
  font-family: var(--font-body);
}
.admin-members p,
.admin-members h1,
.admin-members h2 {
  margin: 0;
}
.admin-members__header > div {
  display: grid;
  gap: 6px;
}
.admin-members__eyebrow {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.admin-members h1 {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.admin-members__description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.admin-members__filters {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.admin-members__search {
  display: flex;
  width: min(360px, 100%);
  min-height: 42px;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 14px;
  color: var(--color-text-muted);
  background: var(--color-surface);
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}
.admin-members__search:hover {
  border-color: var(--color-border-strong);
  background: var(--color-surface-soft);
}
.admin-members__search:focus-within {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}
.admin-members__search i {
  font-size: 18px;
}
.admin-members__search input {
  width: 100%;
  border: 0;
  outline: 0;
  color: var(--color-text);
  background: transparent;
  font: inherit;
  font-size: 13px;
}
.admin-members__search input::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}
.admin-members__filters select,
.admin-members__dialog textarea {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text);
  background: var(--color-surface);
  font: inherit;
}
.admin-members__filters select {
  min-height: 42px;
  padding: 0 34px 0 12px;
  font-size: var(--font-size-sm);
}
.admin-members__filters select:hover {
  border-color: var(--color-border-strong);
  background: var(--color-surface-soft);
}
.admin-members__filters select:focus-visible,
.admin-members__dialog textarea:focus-visible {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}
.admin-members__search-submit {
  min-height: 42px;
  border-radius: var(--radius-md);
  padding: 0 18px;
  font: 700 var(--font-size-sm) var(--font-body);
}
.admin-members__search-submit:disabled {
  cursor: not-allowed;
  border-color: var(--color-disabled-bg);
  color: var(--color-disabled);
  background: var(--color-disabled-bg);
}
.admin-members__feedback {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}
.admin-members__table-card {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.admin-members__row {
  display: grid;
  grid-template-columns:
    minmax(190px, 2fr) minmax(90px, 1fr) minmax(94px, 0.8fr) minmax(112px, 1fr)
    minmax(76px, 0.7fr);
  align-items: center;
  gap: var(--space-3);
  min-height: 72px;
  padding: 0 var(--space-5);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-size: 13px;
}
.admin-members__row:last-of-type {
  border-bottom: 0;
}
.admin-members__row--header {
  min-height: 48px;
  color: var(--color-text);
  background: var(--color-bg);
  font-weight: 700;
}
.admin-members__identity {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.admin-members__identity strong {
  color: var(--color-text);
}
.admin-members__identity small {
  overflow: hidden;
  color: var(--color-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-members em {
  display: inline-flex;
  width: max-content;
  border-radius: var(--radius-pill);
  padding: 5px 10px;
  font-style: normal;
  font-weight: 700;
}
.ACTIVE {
  color: var(--color-success);
  background: var(--color-success-soft);
}
.SUSPENDED,
.DEACTIVATED {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}
.admin-members__actions {
  display: flex;
  justify-content: start;
}
.admin-members__actions button,
.admin-members__dialog-actions button {
  min-height: 38px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  font: 700 var(--font-size-sm) var(--font-body);
}
.admin-members__suspend {
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
  background: var(--color-surface);
}
.admin-members__suspend:hover {
  background: var(--color-danger-soft);
}
.admin-members__restore,
.admin-members__cancel {
  border: 1px solid var(--color-border);
  color: var(--color-text);
  background: var(--color-surface);
}
.admin-members__restore:hover,
.admin-members__cancel:hover {
  border-color: var(--color-border-strong);
  background: var(--color-primary-soft);
}
.admin-members__empty {
  padding: var(--space-6);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-align: center;
}
.admin-members__modal {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: grid;
  place-items: center;
  padding: var(--space-4);
  background: rgba(26, 31, 46, 0.4);
}
.admin-members__dialog {
  display: grid;
  width: min(440px, 100%);
  gap: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}
.admin-members__dialog h2 {
  font-size: var(--font-size-md);
}
.admin-members__dialog p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.admin-members__dialog label {
  display: grid;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.admin-members__dialog textarea {
  min-height: 112px;
  padding: var(--space-3);
  resize: vertical;
}
.admin-members__dialog-actions {
  display: flex;
  justify-content: end;
  gap: var(--space-3);
}
@media (max-width: 960px) {
  .admin-members__table-card {
    overflow-x: auto;
  }
  .admin-members__row {
    min-width: 760px;
  }
}
@media (max-width: 560px) {
  .admin-members__filters {
    align-items: stretch;
    flex-direction: column;
  }
  .admin-members__search {
    width: 100%;
  }
  .admin-members__filters select,
  .admin-members__search-submit {
    width: 100%;
  }
}
</style>
