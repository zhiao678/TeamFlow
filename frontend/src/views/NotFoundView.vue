<template>
  <el-result icon="info" title="页面未找到" sub-title="该地址不存在，请返回工作台继续浏览。"
    ><template #extra
      ><RouterLink to="/dashboard"
        ><el-button type="primary">返回工作台</el-button></RouterLink
      ></template
    ></el-result
  >
</template>
