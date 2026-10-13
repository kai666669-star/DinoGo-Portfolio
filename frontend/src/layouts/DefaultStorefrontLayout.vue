<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import PrimaryNav from '@/components/layout/PrimaryNav.vue'
import UtilityBar from '@/components/layout/UtilityBar.vue'
import mascotUrl from '@/assets/images/dinogo-mascot.png'

const route = useRoute()
const mascotBottom = ref('80px')
let mascotFrame = 0

const updateMascotBottom = () => {
  mascotFrame = 0
  const footer = document.querySelector('.app-footer')

  if (!footer) {
    mascotBottom.value = '80px'
    return
  }

  const footerTop = footer.getBoundingClientRect().top
  const footerVisibleHeight = Math.max(0, window.innerHeight - footerTop)
  const nextBottom = Math.max(80, footerVisibleHeight + 16)
  mascotBottom.value = `${Math.round(nextBottom)}px`
}

const scheduleMascotUpdate = () => {
  if (mascotFrame) return
  mascotFrame = window.requestAnimationFrame(updateMascotBottom)
}

onMounted(() => {
  updateMascotBottom()
  window.addEventListener('scroll', scheduleMascotUpdate, { passive: true })
  window.addEventListener('resize', scheduleMascotUpdate)
})

onBeforeUnmount(() => {
  if (mascotFrame) {
    window.cancelAnimationFrame(mascotFrame)
  }
  window.removeEventListener('scroll', scheduleMascotUpdate)
  window.removeEventListener('resize', scheduleMascotUpdate)
})

watch(
  () => route.fullPath,
  async () => {
    await nextTick()
    scheduleMascotUpdate()
  },
)
</script>

<template>
  <div class="layout-shell d-flex min-vh-100 flex-column">
    <UtilityBar />
    <AppHeader />
    <PrimaryNav />
    <main class="flex-grow-1"><RouterView /></main>
    <img
      :src="mascotUrl"
      :style="{ '--mascot-bottom': mascotBottom }"
      alt=""
      class="floating-mascot"
      aria-hidden="true"
    />
    <AppFooter />
  </div>
</template>

<style scoped>
.floating-mascot {
  position: fixed;
  right: clamp(var(--space-5), 2.5vw, 40px);
  bottom: var(--mascot-bottom);
  z-index: 1020;
  display: block;
  width: clamp(96px, 11vw, 160px);
  max-width: 18vw;
  height: auto;
  pointer-events: none;
  user-select: none;
}

@media (max-width: 767.98px) {
  .floating-mascot {
    right: var(--space-4);
    width: 88px;
    max-width: 22vw;
  }
}

@media (max-width: 575.98px) {
  .floating-mascot {
    display: none;
  }
}
</style>
