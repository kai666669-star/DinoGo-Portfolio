export function isPasswordWithinUtf8ByteLimit(password) {
  return new TextEncoder().encode(password ?? '').length <= 72
}
