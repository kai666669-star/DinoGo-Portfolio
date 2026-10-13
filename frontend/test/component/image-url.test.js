import { afterEach, describe, expect, it, vi } from 'vitest'

import { getImageUrl } from '../../src/utils/imageUrl.js'

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('getImageUrl', () => {
  it('uses the API origin for local development uploads', () => {
    vi.stubEnv('VITE_API_URL', 'http://localhost:8080/api')

    expect(getImageUrl('/uploads/a.jpg')).toBe('http://localhost:8080/uploads/a.jpg')
  })

  it('keeps uploads same-origin for a relative production API URL', () => {
    vi.stubEnv('VITE_API_URL', '/api')

    expect(getImageUrl('/uploads/a.jpg')).toBe('/uploads/a.jpg')
  })

  it('preserves complete external URLs', () => {
    expect(getImageUrl('https://res.cloudinary.com/test.jpg')).toBe(
      'https://res.cloudinary.com/test.jpg',
    )
  })

  it('preserves data URLs', () => {
    expect(getImageUrl('data:image/png;base64,test')).toBe('data:image/png;base64,test')
  })

  it('preserves blob URLs', () => {
    expect(getImageUrl('blob:https://example.com/test')).toBe('blob:https://example.com/test')
  })

  it('handles null safely', () => {
    expect(getImageUrl(null)).toBe('')
  })
})
