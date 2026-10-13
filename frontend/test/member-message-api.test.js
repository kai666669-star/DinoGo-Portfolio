import assert from 'node:assert/strict'
import test from 'node:test'
import api from '../src/api/axios.js'
import {
  deleteMemberMessage,
  getMemberInbox,
  getMemberMessage,
  getMemberUnreadCount,
  markMemberMessageRead,
} from '../src/api/memberMessageApi.js'

test('member message API uses the sysmsg inbox contract endpoints', async (t) => {
  const originalGet = api.get
  const originalPatch = api.patch
  const originalDelete = api.delete
  const calls = []

  api.get = async (...args) => calls.push(['get', ...args])
  api.patch = async (...args) => calls.push(['patch', ...args])
  api.delete = async (...args) => calls.push(['delete', ...args])
  t.after(() => {
    api.get = originalGet
    api.patch = originalPatch
    api.delete = originalDelete
  })

  await getMemberInbox('ORDER_INBOX', 0)
  await getMemberUnreadCount()
  await getMemberMessage(42)
  await markMemberMessageRead(42)
  await deleteMemberMessage(42)

  assert.deepEqual(calls, [
    ['get', '/sysmsg/member/inbox', { params: { category: 'ORDER_INBOX', page: 0 } }],
    ['get', '/sysmsg/member/inbox/unread-count'],
    ['get', '/sysmsg/member/inbox/42'],
    ['patch', '/sysmsg/member/inbox/42/status', { targetStatus: 'READ' }],
    ['delete', '/sysmsg/member/inbox/42'],
  ])
})
