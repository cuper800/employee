<script setup>
import SeatingChart from './components/SeatingChart.vue'
</script>

<template>
  <div class="app">
    <header class="app-header">
      <h1>員工座位安排系統</h1>
      <p class="subtitle">人資部門 — 各樓層座位管理</p>
    </header>
    <main>
      <SeatingChart />
    </main>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  font-family: "Microsoft JhengHei", "PingFang TC", Arial, sans-serif;
  background: #f4f6f8;
  color: #222;
}
.app {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.app-header {
  border-bottom: 2px solid #008659;
  padding-bottom: 12px;
  margin-bottom: 24px;
}
.app-header h1 {
  margin: 0;
  color: #008659;
  font-size: 24px;
}
.subtitle {
  margin: 4px 0 0;
  color: #666;
  font-size: 14px;
}
</style>
