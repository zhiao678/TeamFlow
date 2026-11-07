<script setup lang="ts">
import type { RecruitmentFormState, RecruitmentTagOption } from '@/types/recruitment'

const form = defineModel<RecruitmentFormState>({ required: true })
const props = defineProps<{
  categories: RecruitmentTagOption[]
  tags: RecruitmentTagOption[]
  currentMembers: number
  errors: Record<string, string>
  unmatchedTags: string[]
  disabled: boolean
}>()
const emit = defineEmits<{ 'tags-change': [] }>()

function toggleTag() {
  emit('tags-change')
}
</script>

<template>
  <form class="recruit-form" novalidate @submit.prevent>
    <p v-if="props.unmatchedTags.length" class="notice" role="note">
      该招募带有选项之外的标签：{{ props.unmatchedTags.join('、') }}。当前详情只返回标签名称，
      未改动标签时保存不会提交标签，以免覆盖原有内容。
    </p>
    <div class="recruit-form-grid">
      <label>
        <span class="recruit-label">分类 <i class="req" aria-hidden="true">*</i></span>
        <select
          v-model="form.categoryId"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.categoryId"
        >
          <option :value="null" disabled>请选择分类</option>
          <option v-for="item in props.categories" :key="item.id" :value="item.id">
            {{ item.name }}
          </option>
        </select>
        <small v-if="props.errors.categoryId" class="form-error">{{
          props.errors.categoryId
        }}</small>
      </label>

      <label class="span-2">
        <span class="recruit-label">招募标题 <i class="req" aria-hidden="true">*</i></span>
        <input
          v-model="form.title"
          maxlength="150"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.title"
          placeholder="例如：寻找 Java 课程汇报队友"
        />
        <small v-if="props.errors.title" class="form-error">{{ props.errors.title }}</small>
        <small v-else class="hint">{{ form.title.length }} / 150</small>
      </label>

      <label>
        <span class="recruit-label">课程</span>
        <input
          v-model="form.courseName"
          maxlength="100"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.courseName"
          placeholder="课堂组队建议填写"
        />
        <small v-if="props.errors.courseName" class="form-error">
          {{ props.errors.courseName }}
        </small>
      </label>

      <label>
        <span class="recruit-label">教师</span>
        <input
          v-model="form.teacherName"
          maxlength="100"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.teacherName"
          placeholder="教师姓名"
        />
        <small v-if="props.errors.teacherName" class="form-error">
          {{ props.errors.teacherName }}
        </small>
      </label>

      <label class="span-2">
        <span class="recruit-label"
          >组队目的／活动名称 <i class="req" aria-hidden="true">*</i></span
        >
        <input
          v-model="form.purpose"
          maxlength="200"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.purpose"
          placeholder="例如：完成课程期末汇报"
        />
        <small v-if="props.errors.purpose" class="form-error">{{ props.errors.purpose }}</small>
        <small v-else class="hint">{{ form.purpose.length }} / 200</small>
      </label>

      <label class="span-2">
        <span class="recruit-label">详细说明</span>
        <textarea
          v-model="form.description"
          rows="4"
          :disabled="props.disabled"
          placeholder="计划、时间安排、预期投入等"
        ></textarea>
      </label>

      <label class="span-2">
        <span class="recruit-label">招募要求</span>
        <textarea
          v-model="form.requirements"
          rows="3"
          :disabled="props.disabled"
          placeholder="对队友的技能、时间或分工要求"
        ></textarea>
      </label>

      <label>
        <span class="recruit-label">总人数上限 <i class="req" aria-hidden="true">*</i></span>
        <input
          v-model.number="form.maxMembers"
          type="number"
          min="2"
          step="1"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.maxMembers"
        />
        <small v-if="props.errors.maxMembers" class="form-error">
          {{ props.errors.maxMembers }}
        </small>
        <small v-else class="hint">含负责人，当前在队 {{ props.currentMembers }} 人</small>
      </label>

      <label>
        <span class="recruit-label">申请截止时间</span>
        <input
          v-model="form.deadlineAt"
          type="datetime-local"
          step="1"
          :disabled="props.disabled"
          :aria-invalid="!!props.errors.deadlineAt"
        />
        <small v-if="props.errors.deadlineAt" class="form-error">
          {{ props.errors.deadlineAt }}
        </small>
        <small v-else class="hint">北京时间，可留空；已有截止时间暂不支持清空。</small>
      </label>

      <fieldset class="span-2 recruit-form-tags" :disabled="props.disabled">
        <legend>技能标签</legend>
        <label v-for="item in props.tags" :key="item.id" class="recruit-check">
          <input v-model="form.tagIds" type="checkbox" :value="item.id" @change="toggleTag" />
          <span>{{ item.name }}</span>
        </label>
        <small v-if="!props.tags.length" class="hint">暂无可用标签选项。</small>
      </fieldset>
    </div>
  </form>
</template>
