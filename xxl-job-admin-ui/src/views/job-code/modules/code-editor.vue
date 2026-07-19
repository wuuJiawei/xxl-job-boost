<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import CodeMirror from 'codemirror';
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/material-darker.css';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/edit/matchbrackets';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/addon/hint/anyword-hint';
import 'codemirror/addon/selection/active-line';
import 'codemirror/mode/clike/clike';
import 'codemirror/mode/javascript/javascript';
import 'codemirror/mode/php/php';
import 'codemirror/mode/powershell/powershell';
import 'codemirror/mode/python/python';
import 'codemirror/mode/shell/shell';
import { getGlueEditorMode } from '@/constants/job-glue';

interface Props {
  modelValue: string;
  glueType: string;
}

interface Emits {
  (event: 'update:modelValue', value: string): void;
  (event: 'save'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const editorElement = ref<HTMLElement | null>(null);
let editor: CodeMirror.Editor | null = null;

function saveShortcut(instance: CodeMirror.Editor) {
  emit('save');
  instance.focus();
}

onMounted(() => {
  if (!editorElement.value) {
    return;
  }

  editor = CodeMirror(editorElement.value, {
    value: props.modelValue,
    mode: getGlueEditorMode(props.glueType),
    theme: 'material-darker',
    lineNumbers: true,
    lineWrapping: false,
    indentUnit: 4,
    tabSize: 4,
    indentWithTabs: false,
    smartIndent: true,
    matchBrackets: true,
    autoCloseBrackets: true,
    styleActiveLine: true,
    extraKeys: {
      'Ctrl-S': saveShortcut,
      'Cmd-S': saveShortcut,
      'Ctrl-Space': 'autocomplete',
      Tab: instance => {
        if (instance.somethingSelected()) {
          instance.indentSelection('add');
          return;
        }
        instance.replaceSelection(' '.repeat(instance.getOption('indentUnit') || 4), 'end', '+input');
      }
    }
  });

  editor.on('change', instance => {
    emit('update:modelValue', instance.getValue());
  });

  void nextTick(() => editor?.refresh());
});

watch(
  () => props.modelValue,
  value => {
    if (editor && value !== editor.getValue()) {
      const cursor = editor.getCursor();
      editor.setValue(value);
      editor.setCursor(cursor);
    }
  }
);

watch(
  () => props.glueType,
  glueType => {
    editor?.setOption('mode', getGlueEditorMode(glueType));
  }
);

onBeforeUnmount(() => {
  editor = null;
});
</script>

<template>
  <div ref="editorElement" class="job-code-editor"></div>
</template>
