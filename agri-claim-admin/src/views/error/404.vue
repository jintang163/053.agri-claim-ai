<template>
  <div class="nf-page flex-center flex-column">
    <div class="nf-code">404</div>
    <div class="nf-text">抱歉，您访问的页面不存在</div>
    <el-button type="primary" size="large" class="mt-20" @click="$router.replace('/')">返回首页</el-button>
  </div>
</template>

<style lang="scss" scoped>
.nf-page {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  .nf-code {
    font-size: 180px; font-weight: 900; line-height: 1;
    background: rgba(255,255,255,0.15);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    text-shadow: 0 10px 40px rgba(0,0,0,0.2);
  }
  .nf-text { font-size: 20px; margin-top: 20px; opacity: 0.9; }
  .mt-20 { margin-top: 20px; }
}
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-column { flex-direction: column; }
</style>
