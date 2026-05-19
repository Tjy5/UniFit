import { inject } from 'vue'

export const radioGroupKey = Symbol('legacy-radio-group')

export function useRadioGroup() {
  return inject(radioGroupKey, null)
}
