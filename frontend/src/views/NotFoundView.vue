<script setup lang="ts">
import { homeRoute } from '@/router/home'
</script>

<template>
  <el-result icon="info" title="页面未找到" sub-title="该地址不存在，请返回招募大厅继续浏览。"
    ><template #extra
      ><RouterLink :to="homeRoute"
        ><el-button type="primary">返回招募大厅</el-button></RouterLink
      ></template
    ></el-result
  >
</template>
