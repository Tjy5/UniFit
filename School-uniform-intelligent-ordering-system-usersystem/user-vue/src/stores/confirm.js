import { reactive, readonly } from 'vue'

const state = reactive({
  open: false,
  title: '提示',
  message: '',
  options: {},
  resolve: null,
  reject: null,
})

function cleanup() {
  state.open = false
  state.title = '提示'
  state.message = ''
  state.options = {}
  state.resolve = null
  state.reject = null
}

export function useConfirmState() {
  return readonly(state)
}

export function requestConfirm(message, title = '提示', options = {}) {
  if (state.open && state.reject) {
    state.reject('cancel')
  }

  state.open = true
  state.title = title
  state.message = message
  state.options = options

  return new Promise((resolve, reject) => {
    state.resolve = resolve
    state.reject = reject
  })
}

export function resolveConfirm() {
  state.resolve?.(true)
  cleanup()
}

export function rejectConfirm(reason = 'cancel') {
  state.reject?.(reason)
  cleanup()
}
