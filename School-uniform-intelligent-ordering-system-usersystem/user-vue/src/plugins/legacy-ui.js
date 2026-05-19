import { toast } from 'vue-sonner'
import { requestConfirm } from '@/stores/confirm'
import ElButton from '@/components/legacy/ElButton.vue'
import ElCard from '@/components/legacy/ElCard.vue'
import ElCarousel from '@/components/legacy/ElCarousel.vue'
import ElCarouselItem from '@/components/legacy/ElCarouselItem.vue'
import ElCol from '@/components/legacy/ElCol.vue'
import ElDialog from '@/components/legacy/ElDialog.vue'
import ElDivider from '@/components/legacy/ElDivider.vue'
import ElEmpty from '@/components/legacy/ElEmpty.vue'
import ElForm from '@/components/legacy/ElForm.vue'
import ElFormItem from '@/components/legacy/ElFormItem.vue'
import ElInput from '@/components/legacy/ElInput.vue'
import ElInputNumber from '@/components/legacy/ElInputNumber.vue'
import ElLink from '@/components/legacy/ElLink.vue'
import ElOption from '@/components/legacy/ElOption.vue'
import ElPagination from '@/components/legacy/ElPagination.vue'
import ElRadio from '@/components/legacy/ElRadio.vue'
import ElRadioGroup from '@/components/legacy/ElRadioGroup.vue'
import ElRate from '@/components/legacy/ElRate.vue'
import ElRow from '@/components/legacy/ElRow.vue'
import ElSelect from '@/components/legacy/ElSelect.vue'
import ElSkeleton from '@/components/legacy/ElSkeleton.vue'
import ElSwitch from '@/components/legacy/ElSwitch.vue'
import ElTag from '@/components/legacy/ElTag.vue'

const loadingClass = 'legacy-loading'

function ensureLoadingStyle() {
  if (document.getElementById('legacy-loading-style')) {
    return
  }

  const style = document.createElement('style')
  style.id = 'legacy-loading-style'
  style.textContent = `
    .${loadingClass} {
      position: relative;
      pointer-events: none;
      opacity: 0.72;
    }
    .${loadingClass}::after {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba(255,250,245,0.4);
      border-radius: inherit;
    }
  `
  document.head.appendChild(style)
}

function createMessage() {
  const base = (type, message) => toast[type]?.(message) ?? toast(message)

  return {
    success(message) {
      base('success', message)
    },
    error(message) {
      base('error', message)
    },
    warning(message) {
      base('warning', message)
    },
    warn(message) {
      base('warning', message)
    },
    info(message) {
      base('info', message)
    },
  }
}

export default {
  install(app) {
    ensureLoadingStyle()

    app.component('ElButton', ElButton)
    app.component('ElCard', ElCard)
    app.component('ElCarousel', ElCarousel)
    app.component('ElCarouselItem', ElCarouselItem)
    app.component('ElCol', ElCol)
    app.component('ElDialog', ElDialog)
    app.component('ElDivider', ElDivider)
    app.component('ElEmpty', ElEmpty)
    app.component('ElForm', ElForm)
    app.component('ElFormItem', ElFormItem)
    app.component('ElInput', ElInput)
    app.component('ElInputNumber', ElInputNumber)
    app.component('ElLink', ElLink)
    app.component('ElOption', ElOption)
    app.component('ElPagination', ElPagination)
    app.component('ElRadio', ElRadio)
    app.component('ElRadioGroup', ElRadioGroup)
    app.component('ElRate', ElRate)
    app.component('ElRow', ElRow)
    app.component('ElSelect', ElSelect)
    app.component('ElSkeleton', ElSkeleton)
    app.component('ElSwitch', ElSwitch)
    app.component('ElTag', ElTag)

    app.config.globalProperties.$message = createMessage()
    app.config.globalProperties.$confirm = (message, title, options) => requestConfirm(message, title, options)

    app.directive('loading', {
      mounted(el, binding) {
        el.classList.toggle(loadingClass, Boolean(binding.value))
      },
      updated(el, binding) {
        el.classList.toggle(loadingClass, Boolean(binding.value))
      },
    })

    app.directive('hasPermi', {
      mounted() {},
    })
  },
}
