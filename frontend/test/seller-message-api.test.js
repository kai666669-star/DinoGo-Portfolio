import assert from 'node:assert/strict'
import test from 'node:test'
import api from '../src/api/axios.js'
import {
  createSellerMessage,
  createSellerTemplate,
  deleteSellerOutboxMessage,
  deleteSellerTemplate,
  deleteSellerInboxMessage,
  getSellerInbox,
  getSellerInboxMessage,
  getSellerOutbox,
  getSellerTemplates,
  getSellerUnreadCounts,
  getSellerMessages,
  markAllSellerMessagesRead,
  markSellerInboxMessageRead,
  markSellerMessageRead,
  uploadSellerMessageImages,
  updateSellerTemplate,
} from '../src/api/sellerMessageApi.js'

test('seller message API uses the module F contract endpoints', async (t) => {
  const originalGet = api.get
  const originalPatch = api.patch
  const originalPost = api.post
  const originalPut = api.put
  const originalDelete = api.delete
  const calls = []

  api.get = async (...args) => calls.push(['get', ...args])
  api.patch = async (...args) => calls.push(['patch', ...args])
  api.post = async (...args) => calls.push(['post', ...args])
  api.put = async (...args) => calls.push(['put', ...args])
  api.delete = async (...args) => calls.push(['delete', ...args])
  t.after(() => {
    api.get = originalGet
    api.patch = originalPatch
    api.post = originalPost
    api.put = originalPut
    api.delete = originalDelete
  })

  await getSellerMessages({ category: 'ORDER', page: 1, size: 10 })
  await markSellerMessageRead(42)
  await markAllSellerMessagesRead()
  await getSellerInbox('NEW_ORDER', 2)
  await getSellerUnreadCounts()
  await getSellerInboxMessage(51)
  await markSellerInboxMessageRead(51)
  await deleteSellerInboxMessage(51)
  await createSellerMessage({ orderId: 12, sendTitle: '標題', sendContent: '內容' })
  await getSellerOutbox(4)
  await deleteSellerOutboxMessage(19)
  await getSellerTemplates(3)
  await createSellerTemplate({ msgLabel: '範本', sendTitle: '標題', sendContent: '內容' })
  await updateSellerTemplate(7, { msgLabel: '更新', sendTitle: '標題', sendContent: '內容' })
  await deleteSellerTemplate(7)
  const image = new Blob(['image'], { type: 'image/png' })
  await uploadSellerMessageImages([image])

  assert.deepEqual(calls.slice(0, -1), [
    ['get', '/seller/messages', { params: { category: 'ORDER', page: 1, size: 10 } }],
    ['patch', '/seller/messages/42/read'],
    ['patch', '/seller/messages/read-all'],
    ['get', '/sysmsg/seller/inbox', { params: { category: 'NEW_ORDER', page: 2 } }],
    ['get', '/sysmsg/seller/inbox/unread-counts'],
    ['get', '/sysmsg/seller/inbox/51'],
    ['patch', '/sysmsg/seller/inbox/51/status', { targetStatus: 'READ' }],
    ['delete', '/sysmsg/seller/inbox/51'],
    ['post', '/sysmsg/seller/messages', { orderId: 12, sendTitle: '標題', sendContent: '內容' }],
    ['get', '/sysmsg/seller/messages/outbox', { params: { page: 4 } }],
    ['delete', '/sysmsg/seller/messages/outbox/19'],
    ['get', '/sysmsg/seller/templates', { params: { page: 3 } }],
    ['post', '/sysmsg/seller/templates', { msgLabel: '範本', sendTitle: '標題', sendContent: '內容' }],
    ['put', '/sysmsg/seller/templates/7', { msgLabel: '更新', sendTitle: '標題', sendContent: '內容' }],
    ['delete', '/sysmsg/seller/templates/7'],
  ])
  const uploadCall = calls.at(-1)
  assert.equal(uploadCall[0], 'post')
  assert.equal(uploadCall[1], '/sysmsg/seller/messages/images')
  assert.ok(uploadCall[2] instanceof FormData)
})
