<script>
import { defineComponent, h } from 'vue'

export default defineComponent({
  name: 'ElOption',
  props: {
    label: { type: [String, Number], default: '' },
    value: { type: null, default: null },
    disabled: { type: Boolean, default: false },
  },
  render() {
    return h('span', { style: { display: 'none' } })
  },
})
</script>
