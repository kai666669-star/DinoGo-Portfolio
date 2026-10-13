export const getImageUrl = (imageUrl) => {
  if (typeof imageUrl !== 'string' || !imageUrl.trim()) {
    return ''
  }

  const value = imageUrl.trim()

  if (/^(https?:|data:|blob:)/i.test(value)) {
    return value
  }

  const imagePath = value.startsWith('/') ? value : `/${value}`
  const apiUrl = import.meta.env?.VITE_API_URL

  if (!apiUrl || !/^[a-z][a-z\d+.-]*:\/\//i.test(apiUrl)) {
    return imagePath
  }

  try {
    const apiOrigin = new URL(apiUrl).origin
    return new URL(imagePath, `${apiOrigin}/`).toString()
  } catch {
    return imagePath
  }
}
