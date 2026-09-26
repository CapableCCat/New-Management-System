<script setup>
/**
 * 富文本编辑器（PRD F-008 公告正文）
 *
 * 选型 @wangeditor-next/editor：原版 wangEditor 已停维护，社区 fork 仍在持续更新；
 * 中文文档与开箱即用的工具栏最省事。
 *
 * 两条刻意为之的约束：
 *   1. **只给「上传图片」，不给「网络图片」** —— 站外图片会被后端白名单剥掉，
 *      给了入口反而让人白填一次；图片统一走本站 MinIO（与头像同一通道）
 *   2. 不提供视频/全屏菜单 —— 视频不在白名单内，全屏在弹窗里意义不大
 *
 * ⚠️ 拿到的 HTML 一律经 `sanitizeHtml` 清洗后再交给父组件（提交前的那一道）；
 * 渲染时还会再清一次，见 `utils/sanitizeHtml.js`。
 */
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import '@wangeditor-next/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor-next/editor-for-vue'
import { uploadAnnouncementImage } from '@/api/announcement'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入公告内容…' },
  height: { type: String, default: '340px' }
})

const emit = defineEmits(['update:modelValue'])

/** 编辑器实例必须用 shallowRef：深层响应式会代理掉编辑器内部结构 */
const editorRef = shallowRef(null)
const valueHtml = ref(props.modelValue || '')

watch(
  () => props.modelValue,
  (next) => {
    if ((next || '') !== valueHtml.value) {
      valueHtml.value = next || ''
    }
  }
)

watch(valueHtml, (next) => emit('update:modelValue', next || ''))

const toolbarConfig = {
  excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'insertImage', 'fullScreen']
}

const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 2 * 1024 * 1024,
      allowedFileTypes: ['image/jpeg', 'image/png'],
      // 走本站上传接口；失败提示由 axios 拦截器统一弹出
      customUpload(file, insertFn) {
        uploadAnnouncementImage(file)
          .then((url) => insertFn(url, file.name || '公告配图', url))
          .catch(() => {})
      }
    }
  }
}

function handleCreated(editor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  // 不销毁会在弹窗反复开关时堆积监听，官方要求显式 destroy
  editorRef.value?.destroy()
})
</script>

<template>
  <div class="rich-editor">
    <Toolbar class="rich-editor__toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      v-model="valueHtml"
      class="rich-editor__body"
      :style="{ height }"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
    />
  </div>
</template>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: var(--brand-radius);
  overflow: hidden;
}

.rich-editor__toolbar {
  border-bottom: 1px solid #ebeef5;
}

.rich-editor__body {
  overflow-y: auto;
}

/* 工具栏下拉面板要压在 el-dialog（z-index 2000+）之上，否则会被弹窗盖住 */
.rich-editor :deep(.w-e-drop-panel),
.rich-editor :deep(.w-e-modal) {
  z-index: 3000;
}

.rich-editor :deep(.w-e-bar-item-menus-container) {
  z-index: 3000;
}
</style>
