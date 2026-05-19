<template>
  <AlertDialogRoot :open="confirmState.open">
    <AlertDialogPortal>
      <AlertDialogOverlay class="fixed inset-0 z-[1200] bg-[rgba(15,23,42,0.48)] backdrop-blur-sm" />
      <AlertDialogContent
        class="fixed left-1/2 top-1/2 z-[1201] w-[min(92vw,420px)] -translate-x-1/2 -translate-y-1/2 rounded-[12px] border border-[var(--line)] bg-[var(--surface-strong)] p-6 shadow-[var(--shadow-lg)]"
      >
        <AlertDialogTitle class="text-xl font-semibold text-[var(--text-primary)]">
          {{ confirmState.title }}
        </AlertDialogTitle>
        <AlertDialogDescription class="mt-3 whitespace-pre-wrap text-sm leading-6 text-[var(--text-secondary)]">
          {{ confirmState.message }}
        </AlertDialogDescription>

        <div class="mt-6 flex justify-end gap-3">
          <AlertDialogCancel
            class="rounded-[8px] border border-[var(--line)] bg-white px-4 py-2 text-sm font-medium text-[var(--text-primary)] transition hover:bg-[var(--surface-muted)]"
            @click="handleCancel"
          >
            {{ confirmState.options.cancelButtonText || '取消' }}
          </AlertDialogCancel>
          <AlertDialogAction
            class="rounded-[8px] border border-transparent bg-[var(--brand)] px-4 py-2 text-sm font-medium text-white transition hover:brightness-95"
            @click="handleConfirm"
          >
            {{ confirmState.options.confirmButtonText || '确定' }}
          </AlertDialogAction>
        </div>
      </AlertDialogContent>
    </AlertDialogPortal>
  </AlertDialogRoot>
</template>

<script setup>
import {
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogOverlay,
  AlertDialogPortal,
  AlertDialogRoot,
  AlertDialogTitle,
} from 'radix-vue'
import { rejectConfirm, resolveConfirm, useConfirmState } from '@/stores/confirm'

const confirmState = useConfirmState()

function handleConfirm() {
  resolveConfirm()
}

function handleCancel() {
  rejectConfirm('cancel')
}
</script>
