<template>
  <router-view />
</template>

<script setup lang="ts">
// 应用根组件
</script>

<style>
:root {
  --el-color-primary: #375d88;
  --el-color-primary-light-3: #7390ae;
  --el-color-primary-light-5: #9baec3;
  --el-color-primary-light-7: #c3cedb;
  --el-color-primary-light-8: #d7dfe8;
  --el-color-primary-light-9: #ebeff4;
  --el-color-primary-dark-2: #27476c;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  background: #f5f7f9;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
}

#app {
  height: 100%;
}

.main-layout .section-intro {
  margin-bottom: 22px;
  padding: 2px 0 18px;
  border-bottom: 1px solid #dfe5eb;
  color: #263747;
}

.main-layout .section-intro h1 { margin: 0 0 7px; font-size: clamp(22px, 2vw, 27px); font-weight: 650; letter-spacing: .01em; animation: content-enter .32s ease-out both; }
.main-layout .section-intro p { color: #718093; font-size: 13px; animation: content-enter .4s .09s ease-out both; }

.main-layout :is(.user-management, .role-management, .menu-management) > :is(.toolbar-form, .toolbar) {
  min-height: 76px;
  display: flex;
  align-items: center;
  padding: 20px 24px 4px;
  border: 1px solid #e2e7ed;
  border-bottom: 0;
  border-radius: 10px 10px 0 0;
  background: #fff;
  animation: content-enter .4s .12s ease-out both;
}
.main-layout .menu-management > .toolbar { padding-bottom: 20px; }
.main-layout :is(.user-management, .role-management, .menu-management) > .el-table {
  --el-table-border-color: #e7ebef;
  --el-table-header-bg-color: #f8f9fa;
  --el-table-row-hover-bg-color: #f3f7fa;
  border: 1px solid #e2e7ed;
  border-top: 0;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 6px 20px #263d5310;
  font-size: 12px;
  animation: content-enter .42s .18s ease-out both;
}
.main-layout .el-table th.el-table__cell { height: 48px; color: #667689; font-size: 11px; font-weight: 650; }
.main-layout .el-table td.el-table__cell { height: 53px; color: #34475b; }
.main-layout .el-table .el-button.is-link { font-weight: 600; }
.main-layout .el-pagination { min-height: 52px; color: #8292a8; }
.main-layout .el-tag { border-radius: 7px; font-weight: 600; }
.main-layout .el-input__wrapper, .main-layout .el-select__wrapper, .main-layout .el-textarea__inner { border-radius: 8px; box-shadow: 0 0 0 1px #e3eaf2 inset; }
.main-layout .el-button:not(.is-link) { border-radius: 8px; font-weight: 600; }
.main-layout .el-button--primary:not(.is-link) { background: #375d88; border-color: #375d88; }
.main-layout .el-button--primary:not(.is-link):hover { background: #27476c; border-color: #27476c; }
.main-layout .el-button { transition: background-color .18s, border-color .18s, color .18s, transform .18s; }
.main-layout .el-button:not(:disabled):active { transform: translateY(1px); }
.el-dialog { max-width: calc(100vw - 32px); border-radius: 16px; box-shadow: 0 24px 80px #132c4c38; }
.el-dialog__header { padding-bottom: 18px; border-bottom: 1px solid #edf1f6; }
.el-dialog__title { color: #253a57; font-weight: 650; }
.el-dialog__body { padding-top: 24px; }

@media (max-width: 700px) {
  .main-layout .section-intro { padding-bottom: 16px; }
  .main-layout :is(.user-management, .role-management, .menu-management) > :is(.toolbar-form, .toolbar) { padding: 18px 16px 4px; }
  .main-layout .menu-management > .toolbar { padding-bottom: 18px; }
  .main-layout .el-pagination { overflow-x: auto; }
}
@keyframes content-enter { from { opacity: .6; transform: translateY(6px); } }
@media (prefers-reduced-motion: reduce) {
  .main-layout .section-intro h1, .main-layout .section-intro p,
  .main-layout :is(.user-management, .role-management, .menu-management) > :is(.toolbar-form, .toolbar, .el-table) { animation: none; }
  .main-layout .el-button { transition: none; }
}
</style>
