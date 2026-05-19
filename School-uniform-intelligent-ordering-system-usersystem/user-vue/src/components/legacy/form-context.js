import { inject } from 'vue'

export const formContextKey = Symbol('legacy-form')

export function useFormContext() {
  return inject(formContextKey, null)
}
