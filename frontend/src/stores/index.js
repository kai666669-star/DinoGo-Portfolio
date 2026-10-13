import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

// Use one Pinia instance for the whole application.
// Non-component files such as the router can import this instance.
// Register the persistence plugin once for every store in the application.
export const pinia = createPinia().use(piniaPluginPersistedstate)
