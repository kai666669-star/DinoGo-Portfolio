<script setup>
import { ref, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'

import api from '@/api/axios'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const descriptionImageInput = ref(null)
const ResizableImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),

      width: {
        default: '100%',

        parseHTML: (element) => element.getAttribute('width') || '100%',

        renderHTML: (attributes) => ({
          width: attributes.width,
        }),
      },
    }
  },
})
const editor = useEditor({
  content: props.modelValue,

  extensions: [
    StarterKit,

    Underline,

    Link.configure({
      openOnClick: false,
      autolink: true,
    }),

    ResizableImage.configure({
      inline: false,
      allowBase64: false,
    }),
  ],

  onUpdate({ editor }) {
    emit('update:modelValue', editor.getHTML())
  },
})

const setImageWidth = (width) => {
  if (!editor.value?.isActive('image')) {
    return
  }

  editor.value
    .chain()
    .focus()
    .updateAttributes('image', {
      width,
    })
    .run()
}
watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) {
      return
    }

    if (editor.value.getHTML() !== value) {
      editor.value.commands.setContent(value || '', false)
    }
  },
)

const addLink = () => {
  const url = window.prompt('請輸入連結網址')

  if (!url) {
    return
  }

  editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

const removeLink = () => {
  editor.value.chain().focus().unsetLink().run()
}

// 點圖片按鈕時，開啟本機檔案選擇器
const openDescriptionImagePicker = () => {
  descriptionImageInput.value?.click()
}

// 選完圖片後上傳 Cloudinary
const handleDescriptionImageSelect = async (event) => {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  try {
    const formData = new FormData()

    formData.append('file', file)

    const response = await api.post('/products/description-images', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    const imageUrl = response.data.imageUrl

    if (!imageUrl) {
      return
    }

    editor.value
      .chain()
      .focus()
      .setImage({
        src: imageUrl,
        width: '100%',
      })
      .run()
  } catch (error) {
    console.error('商品描述圖片上傳失敗：', error)
  } finally {
    // 讓同一張圖片下次還能重新選
    event.target.value = ''
  }
}
</script>

<template>
  <div class="rich-editor">
    <div v-if="editor" class="editor-toolbar">
      <button
        type="button"
        :class="{ active: editor.isActive('bold') }"
        title="粗體"
        @click="editor.chain().focus().toggleBold().run()"
      >
        <strong>B</strong>
      </button>

      <button
        type="button"
        :class="{ active: editor.isActive('italic') }"
        title="斜體"
        @click="editor.chain().focus().toggleItalic().run()"
      >
        <em>I</em>
      </button>

      <button
        type="button"
        :class="{ active: editor.isActive('underline') }"
        title="底線"
        @click="editor.chain().focus().toggleUnderline().run()"
      >
        <u>U</u>
      </button>

      <span class="toolbar-divider"></span>

      <button
        type="button"
        :class="{ active: editor.isActive('heading', { level: 2 }) }"
        title="標題"
        @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        H2
      </button>

      <button
        type="button"
        :class="{ active: editor.isActive('bulletList') }"
        title="項目符號"
        @click="editor.chain().focus().toggleBulletList().run()"
      >
        <i class="bi bi-list-ul"></i>
      </button>

      <button
        type="button"
        :class="{ active: editor.isActive('orderedList') }"
        title="編號清單"
        @click="editor.chain().focus().toggleOrderedList().run()"
      >
        <i class="bi bi-list-ol"></i>
      </button>

      <span class="toolbar-divider"></span>

      <button type="button" title="插入圖片" @click="openDescriptionImagePicker">
        <i class="bi bi-image"></i>
      </button>
      <span class="toolbar-divider"></span>

      <div class="image-size-tools">
        <button
          type="button"
          title="圖片寬度 25%"
          :disabled="!editor?.isActive('image')"
          @click="setImageWidth('25%')"
        >
          25%
        </button>

        <button
          type="button"
          title="圖片寬度 50%"
          :disabled="!editor?.isActive('image')"
          @click="setImageWidth('50%')"
        >
          50%
        </button>

        <button
          type="button"
          title="圖片寬度 75%"
          :disabled="!editor?.isActive('image')"
          @click="setImageWidth('75%')"
        >
          75%
        </button>

        <button
          type="button"
          title="圖片寬度 100%"
          :disabled="!editor?.isActive('image')"
          @click="setImageWidth('100%')"
        >
          100%
        </button>
      </div>
      <input
        ref="descriptionImageInput"
        type="file"
        accept="image/*"
        class="description-image-input"
        @change="handleDescriptionImageSelect"
      />

      <button v-if="editor.isActive('link')" type="button" title="移除連結" @click="removeLink">
        <i class="bi bi-link-45deg"></i> ×
      </button>

      <span class="toolbar-divider"></span>

      <button
        type="button"
        title="復原"
        :disabled="!editor.can().undo()"
        @click="editor.chain().focus().undo().run()"
      >
        <i class="bi bi-arrow-counterclockwise"></i>
      </button>

      <button
        type="button"
        title="重做"
        :disabled="!editor.can().redo()"
        @click="editor.chain().focus().redo().run()"
      >
        <i class="bi bi-arrow-clockwise"></i>
      </button>
    </div>

    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<style scoped>
.rich-editor {
  overflow: hidden;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;

  padding: var(--space-2);

  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-muted);
}

.editor-toolbar button {
  display: grid;
  place-items: center;

  width: 36px;
  height: 36px;
  min-height: 36px;

  padding: 0;

  border: 1px solid transparent;
  border-radius: var(--radius-sm);

  background: transparent;

  cursor: pointer;
}

.editor-toolbar button:hover,
.editor-toolbar button.active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.editor-toolbar button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.toolbar-divider {
  width: 1px;
  height: 24px;

  margin: 0 4px;

  background: var(--color-border);
}

.editor-content :deep(.ProseMirror img) {
  display: block;

  max-width: 100%;
  height: auto;

  margin: var(--space-3) 0;

  cursor: pointer;
}

/* Tiptap 內容本身不會帶 scoped attribute，所以要 :deep */
.editor-content :deep(.ProseMirror) {
  min-height: 220px;

  padding: var(--space-3);

  outline: none;

  line-height: 1.7;
}

.editor-content :deep(.ProseMirror p) {
  margin: 0 0 var(--space-2);
}

.editor-content :deep(.ProseMirror a) {
  color: var(--color-primary);
  text-decoration: underline;
}
.description-image-input {
  display: none;
}
.image-size-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

.image-size-tools button {
  width: auto;
  min-width: 46px;
  padding: 0 8px;
  font-size: 12px;
}
</style>
