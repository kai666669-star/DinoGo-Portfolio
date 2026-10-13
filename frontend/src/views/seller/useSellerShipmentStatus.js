import { computed, ref } from 'vue'

export function useSellerShipmentStatus({ order, updateStatus }) {
  const updatingShipment = ref(false)
  const shipmentActionError = ref('')

  const shipmentAction = computed(() => {
    if (
      order.value?.shipment?.status === 'PREPARING' &&
      ['PAID', 'PROCESSING'].includes(order.value.status)
    ) {
      return {
        status: 'SHIPPED',
        label: '確認出貨',
        pendingLabel: '出貨確認中…',
      }
    }
    return null
  })

  async function updateShipmentStatus() {
    if (!shipmentAction.value || updatingShipment.value) return
    const targetStatus = shipmentAction.value.status
    updatingShipment.value = true
    shipmentActionError.value = ''
    try {
      order.value.shipment = await updateStatus(targetStatus)
      if (targetStatus === 'SHIPPED') order.value.status = 'SHIPPED'
    } catch (error) {
      shipmentActionError.value = error.response?.data?.message ?? '更新物流狀態失敗，請稍後再試。'
    } finally {
      updatingShipment.value = false
    }
  }

  return { shipmentAction, shipmentActionError, updatingShipment, updateShipmentStatus }
}
