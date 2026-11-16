<script setup lang="ts">
import { ref, watch } from 'vue'
import { useApplicationsStore } from '@/stores/applications'
import { sessionToken } from '@/api/session'
const props = defineProps<{ id: number }>()
const emit = defineEmits<{ refresh: [] }>()
const applications = useApplicationsStore(),
  confirming = ref(false)
watch([sessionToken, () => props.id], () => {
  confirming.value = false
})
async function withdraw() {
  confirming.value = false
  if (await applications.withdraw(props.id)) emit('refresh')
}
</script>
<template>
  <div class="recruit-actions">
    <template v-if="confirming"
      ><span>确认撤回这条申请？</span
      ><button type="button" :disabled="applications.busy" @click="withdraw">确认撤回</button
      ><button type="button" @click="confirming = false">取消</button></template
    >
    <button
      v-else
      type="button"
      :disabled="applications.busy || !applications.ready"
      @click="confirming = true"
    >
      撤回申请
    </button>
  </div>
</template>
