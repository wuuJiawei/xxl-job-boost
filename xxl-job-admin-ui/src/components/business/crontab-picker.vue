<template>
  <div class="crontab-picker">
    <n-tabs v-model:value="activeTab" type="line" animated>
      <n-tab-pane name="second" tab="秒">
        <CronNumberPanel
          v-model:mode="state.second.mode"
          v-model:start="state.second.start"
          v-model:end="state.second.end"
          v-model:step="state.second.step"
          v-model:values="state.second.values"
          label="秒"
          :min="0"
          :max="59"
        />
      </n-tab-pane>
      <n-tab-pane name="minute" tab="分钟">
        <CronNumberPanel
          v-model:mode="state.minute.mode"
          v-model:start="state.minute.start"
          v-model:end="state.minute.end"
          v-model:step="state.minute.step"
          v-model:values="state.minute.values"
          label="分钟"
          :min="0"
          :max="59"
        />
      </n-tab-pane>
      <n-tab-pane name="hour" tab="小时">
        <CronNumberPanel
          v-model:mode="state.hour.mode"
          v-model:start="state.hour.start"
          v-model:end="state.hour.end"
          v-model:step="state.hour.step"
          v-model:values="state.hour.values"
          label="小时"
          :min="0"
          :max="23"
        />
      </n-tab-pane>
      <n-tab-pane name="day" tab="日">
        <CronDayPanel
          v-model:mode="state.day.mode"
          v-model:start="state.day.start"
          v-model:end="state.day.end"
          v-model:step="state.day.step"
          v-model:workday="state.day.workday"
          v-model:values="state.day.values"
        />
      </n-tab-pane>
      <n-tab-pane name="month" tab="月">
        <CronNumberPanel
          v-model:mode="state.month.mode"
          v-model:start="state.month.start"
          v-model:end="state.month.end"
          v-model:step="state.month.step"
          v-model:values="state.month.values"
          label="月"
          :min="1"
          :max="12"
          :options="monthOptions"
        />
      </n-tab-pane>
      <n-tab-pane name="week" tab="周">
        <CronWeekPanel
          v-model:mode="state.week.mode"
          v-model:start="state.week.start"
          v-model:end="state.week.end"
          v-model:nth="state.week.nth"
          v-model:weekday="state.week.weekday"
          v-model:values="state.week.values"
        />
      </n-tab-pane>
      <n-tab-pane name="year" tab="年">
        <CronYearPanel
          v-model:mode="state.year.mode"
          v-model:start="state.year.start"
          v-model:end="state.year.end"
          v-model:step="state.year.step"
          v-model:values="state.year.values"
          :min="currentYear"
          :max="currentYear + 10"
        />
      </n-tab-pane>
    </n-tabs>

    <div class="crontab-result">
      <div class="crontab-result-row">
        <span>秒</span>
        <span>分</span>
        <span>时</span>
        <span>日</span>
        <span>月</span>
        <span>周</span>
        <span>年</span>
      </div>
      <div class="crontab-result-row crontab-result-value">
        <code>{{ cron.second }}</code>
        <code>{{ cron.minute }}</code>
        <code>{{ cron.hour }}</code>
        <code>{{ cron.day }}</code>
        <code>{{ cron.month }}</code>
        <code>{{ cron.week }}</code>
        <code>{{ cron.year || '-' }}</code>
      </div>
      <div class="crontab-expression">
        <span>Cron</span>
        <code>{{ expression }}</code>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive, ref, watch } from 'vue';
import {
  NForm,
  NInputNumber,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  type SelectOption
} from 'naive-ui';

defineOptions({
  name: 'CrontabPicker'
});

type NumberMode = 'every' | 'range' | 'step' | 'appoint';
type DayMode = NumberMode | 'none' | 'workday' | 'last';
type WeekMode = 'every' | 'none' | 'range' | 'nth' | 'last' | 'appoint';
type YearMode = 'empty' | NumberMode;

type CronFields = {
  second: string;
  minute: string;
  hour: string;
  day: string;
  month: string;
  week: string;
  year: string;
};

const props = defineProps<{
  value?: string;
}>();

const emit = defineEmits<{
  'update:value': [value: string];
}>();

const activeTab = ref('second');
const currentYear = new Date().getFullYear();
const syncing = ref(false);

const state = reactive({
  second: createNumberState<NumberMode>('every', 0, 1, 1, [0]),
  minute: createNumberState<NumberMode>('every', 0, 1, 1, [0]),
  hour: createNumberState<NumberMode>('every', 0, 1, 1, [0]),
  day: {
    ...createNumberState<DayMode>('every', 1, 2, 1, [1]),
    workday: 1
  },
  month: createNumberState<NumberMode>('every', 1, 2, 1, [1]),
  week: {
    mode: 'none' as WeekMode,
    start: 2,
    end: 3,
    nth: 1,
    weekday: 2,
    values: [2]
  },
  year: createNumberState<YearMode>('empty', currentYear, currentYear + 1, 1, [currentYear])
});

const monthOptions = Array.from({ length: 12 }, (_, index) => ({
  label: `${index + 1} 月`,
  value: index + 1
}));

const cron = computed<CronFields>(() => ({
  second: buildNumberField(state.second),
  minute: buildNumberField(state.minute),
  hour: buildNumberField(state.hour),
  day: buildDayField(),
  month: buildNumberField(state.month),
  week: buildWeekField(),
  year: buildYearField()
}));

const expression = computed(() => {
  const items = [cron.value.second, cron.value.minute, cron.value.hour, cron.value.day, cron.value.month, cron.value.week];
  if (cron.value.year) {
    items.push(cron.value.year);
  }
  return items.join(' ');
});

watch(
  () => props.value,
  value => {
    parseExpression(value || '0 0 0 * * ?');
  },
  { immediate: true }
);

watch(
  expression,
  value => {
    if (!syncing.value && value !== props.value) {
      emit('update:value', value);
    }
  }
);

watch(
  () => state.day.mode,
  mode => {
    if (syncing.value) return;
    if (mode === 'none') {
      state.week.mode = 'every';
    } else {
      state.week.mode = 'none';
    }
  }
);

watch(
  () => state.week.mode,
  mode => {
    if (syncing.value) return;
    if (mode === 'none') {
      state.day.mode = 'every';
    } else {
      state.day.mode = 'none';
    }
  }
);

function createNumberState<T extends string>(mode: T, start: number, end: number, step: number, values: number[]) {
  return {
    mode,
    start,
    end,
    step,
    values
  };
}

function clamp(value: unknown, min: number, max: number) {
  const num = Number(value);
  if (Number.isNaN(num)) return min;
  return Math.min(Math.max(num, min), max);
}

function normalizeValues(values: unknown[], min: number, max: number) {
  const result = values.map(item => clamp(item, min, max));
  return Array.from(new Set(result)).sort((a, b) => a - b);
}

function buildNumberField(item: { mode: string; start: number; end: number; step: number; values: number[] }) {
  if (item.mode === 'range') {
    return `${item.start}-${item.end}`;
  }
  if (item.mode === 'step') {
    return `${item.start}/${item.step}`;
  }
  if (item.mode === 'appoint') {
    return item.values.length ? item.values.join(',') : String(item.start);
  }
  return '*';
}

function buildDayField() {
  if (state.day.mode === 'none') return '?';
  if (state.day.mode === 'workday') return `${state.day.workday}W`;
  if (state.day.mode === 'last') return 'L';
  return buildNumberField(state.day);
}

function buildWeekField() {
  if (state.week.mode === 'none') return '?';
  if (state.week.mode === 'range') return `${state.week.start}-${state.week.end}`;
  if (state.week.mode === 'nth') return `${state.week.weekday}#${state.week.nth}`;
  if (state.week.mode === 'last') return `${state.week.weekday}L`;
  if (state.week.mode === 'appoint') {
    return state.week.values.length ? state.week.values.join(',') : String(state.week.weekday);
  }
  return '*';
}

function buildYearField() {
  if (state.year.mode === 'empty') return '';
  return buildNumberField(state.year);
}

function parseExpression(value: string) {
  syncing.value = true;
  const parts = value.trim().split(/\s+/);
  const [second = '0', minute = '0', hour = '0', day = '*', month = '*', week = '?', year = ''] = parts;
  parseNumberField(second, state.second, 0, 59);
  parseNumberField(minute, state.minute, 0, 59);
  parseNumberField(hour, state.hour, 0, 23);
  parseDayField(day);
  parseNumberField(month, state.month, 1, 12);
  parseWeekField(week);
  parseYearField(year);
  syncing.value = false;
}

function parseNumberField(value: string, item: { mode: string; start: number; end: number; step: number; values: number[] }, min: number, max: number) {
  if (value === '*') {
    item.mode = 'every';
    return;
  }
  if (value.includes('-')) {
    const [start, end] = value.split('-');
    item.mode = 'range';
    item.start = clamp(start, min, max - 1);
    item.end = clamp(end, item.start + 1, max);
    return;
  }
  if (value.includes('/')) {
    const [start, step] = value.split('/');
    item.mode = 'step';
    item.start = clamp(start, min, max - 1);
    item.step = clamp(step, 1, max - item.start);
    return;
  }
  item.mode = 'appoint';
  item.values = normalizeValues(value.split(','), min, max);
}

function parseDayField(value: string) {
  if (value === '?') {
    state.day.mode = 'none';
    return;
  }
  if (value === 'L') {
    state.day.mode = 'last';
    return;
  }
  if (value.includes('W')) {
    state.day.mode = 'workday';
    state.day.workday = clamp(value.replace('W', ''), 1, 31);
    return;
  }
  parseNumberField(value, state.day, 1, 31);
}

function parseWeekField(value: string) {
  if (value === '?') {
    state.week.mode = 'none';
    return;
  }
  if (value === '*') {
    state.week.mode = 'every';
    return;
  }
  if (value.includes('-')) {
    const [start, end] = value.split('-');
    state.week.mode = 'range';
    state.week.start = clamp(start, 1, 6);
    state.week.end = clamp(end, state.week.start + 1, 7);
    return;
  }
  if (value.includes('#')) {
    const [weekday, nth] = value.split('#');
    state.week.mode = 'nth';
    state.week.weekday = clamp(weekday, 1, 7);
    state.week.nth = clamp(nth, 1, 4);
    return;
  }
  if (value.includes('L')) {
    state.week.mode = 'last';
    state.week.weekday = clamp(value.replace('L', ''), 1, 7);
    return;
  }
  state.week.mode = 'appoint';
  state.week.values = normalizeValues(value.split(','), 1, 7);
}

function parseYearField(value: string) {
  if (!value) {
    state.year.mode = 'empty';
    return;
  }
  parseNumberField(value, state.year as { mode: NumberMode; start: number; end: number; step: number; values: number[] }, currentYear, currentYear + 10);
}

function numberOptions(min: number, max: number, customOptions?: SelectOption[]) {
  if (customOptions?.length) return customOptions;
  return Array.from({ length: max - min + 1 }, (_, index) => ({
    label: String(index + min),
    value: index + min
  }));
}

function weekOptions() {
  return [
    { label: '星期日', value: 1 },
    { label: '星期一', value: 2 },
    { label: '星期二', value: 3 },
    { label: '星期三', value: 4 },
    { label: '星期四', value: 5 },
    { label: '星期五', value: 6 },
    { label: '星期六', value: 7 }
  ];
}

const CronNumberPanel = defineComponent({
  name: 'CronNumberPanel',
  props: {
    mode: { type: String, required: true },
    start: { type: Number, required: true },
    end: { type: Number, required: true },
    step: { type: Number, required: true },
    values: { type: Array as () => number[], required: true },
    label: { type: String, required: true },
    min: { type: Number, required: true },
    max: { type: Number, required: true },
    options: { type: Array as () => SelectOption[], default: undefined }
  },
  emits: ['update:mode', 'update:start', 'update:end', 'update:step', 'update:values'],
  setup(panelProps, { emit: panelEmit }) {
    return () =>
      h(
        NForm,
        { class: 'crontab-panel', labelPlacement: 'left', labelWidth: 0 },
        {
          default: () =>
            h(
              NRadioGroup,
              { value: panelProps.mode, 'onUpdate:value': value => panelEmit('update:mode', value) },
              {
                default: () =>
                  h(NSpace, { vertical: true, size: 14 }, () => [
                    h(NRadio, { value: 'every' }, () => `${panelProps.label}，允许通配符 [, - * /]`),
                    renderRangeRow(panelProps, panelEmit),
                    renderStepRow(panelProps, panelEmit),
                    renderAppointRow(panelProps, panelEmit)
                  ])
              }
            )
        }
      );
  }
});

const CronDayPanel = defineComponent({
  name: 'CronDayPanel',
  props: {
    mode: { type: String, required: true },
    start: { type: Number, required: true },
    end: { type: Number, required: true },
    step: { type: Number, required: true },
    workday: { type: Number, required: true },
    values: { type: Array as () => number[], required: true }
  },
  emits: ['update:mode', 'update:start', 'update:end', 'update:step', 'update:workday', 'update:values'],
  setup(panelProps, { emit: panelEmit }) {
    return () =>
      h(NRadioGroup, { value: panelProps.mode, 'onUpdate:value': value => panelEmit('update:mode', value) }, () =>
        h(NSpace, { vertical: true, size: 14 }, () => [
          h(NRadio, { value: 'every' }, () => '日，允许通配符 [, - * ? / L W]'),
          h(NRadio, { value: 'none' }, () => '不指定'),
          renderRangeRow({ ...panelProps, label: '日', min: 1, max: 31 }, panelEmit),
          renderStepRow({ ...panelProps, label: '日', min: 1, max: 31 }, panelEmit),
          h(NRadio, { value: 'workday' }, () =>
            h(NSpace, { align: 'center' }, () => [
              '每月',
              renderInputNumber(panelProps.workday, 1, 31, value => panelEmit('update:workday', value)),
              '号最近的工作日'
            ])
          ),
          h(NRadio, { value: 'last' }, () => '本月最后一天'),
          renderAppointRow({ ...panelProps, label: '日', min: 1, max: 31 }, panelEmit)
        ])
      );
  }
});

const CronWeekPanel = defineComponent({
  name: 'CronWeekPanel',
  props: {
    mode: { type: String, required: true },
    start: { type: Number, required: true },
    end: { type: Number, required: true },
    nth: { type: Number, required: true },
    weekday: { type: Number, required: true },
    values: { type: Array as () => number[], required: true }
  },
  emits: ['update:mode', 'update:start', 'update:end', 'update:nth', 'update:weekday', 'update:values'],
  setup(panelProps, { emit: panelEmit }) {
    return () =>
      h(NRadioGroup, { value: panelProps.mode, 'onUpdate:value': value => panelEmit('update:mode', value) }, () =>
        h(NSpace, { vertical: true, size: 14 }, () => [
          h(NRadio, { value: 'every' }, () => '周，允许通配符 [, - * ? / L #]'),
          h(NRadio, { value: 'none' }, () => '不指定'),
          h(NRadio, { value: 'range' }, () =>
            h(NSpace, { align: 'center' }, () => [
              '周期从',
              renderSelect(panelProps.start, weekOptions().filter(item => Number(item.value) < 7), value => panelEmit('update:start', value)),
              '-',
              renderSelect(panelProps.end, weekOptions().filter(item => Number(item.value) > panelProps.start), value => panelEmit('update:end', value))
            ])
          ),
          h(NRadio, { value: 'nth' }, () =>
            h(NSpace, { align: 'center' }, () => [
              '第',
              renderInputNumber(panelProps.nth, 1, 4, value => panelEmit('update:nth', value)),
              '周的',
              renderSelect(panelProps.weekday, weekOptions(), value => panelEmit('update:weekday', value))
            ])
          ),
          h(NRadio, { value: 'last' }, () =>
            h(NSpace, { align: 'center' }, () => [
              '本月最后一个',
              renderSelect(panelProps.weekday, weekOptions(), value => panelEmit('update:weekday', value))
            ])
          ),
          h(NRadio, { value: 'appoint', class: 'crontab-appoint-radio' }, () =>
            h('div', { class: 'crontab-appoint-block' }, [
              '指定星期',
              panelProps.mode === 'appoint'
                ? renderValueGrid(
                    panelProps.values,
                    weekOptions(),
                    value => panelEmit('update:values', value),
                    () => panelEmit('update:mode', 'appoint')
                  )
                : null
            ])
          )
        ])
      );
  }
});

const CronYearPanel = defineComponent({
  name: 'CronYearPanel',
  props: {
    mode: { type: String, required: true },
    start: { type: Number, required: true },
    end: { type: Number, required: true },
    step: { type: Number, required: true },
    values: { type: Array as () => number[], required: true },
    min: { type: Number, required: true },
    max: { type: Number, required: true }
  },
  emits: ['update:mode', 'update:start', 'update:end', 'update:step', 'update:values'],
  setup(panelProps, { emit: panelEmit }) {
    const options = numberOptions(panelProps.min, panelProps.max);
    return () =>
      h(NRadioGroup, { value: panelProps.mode, 'onUpdate:value': value => panelEmit('update:mode', value) }, () =>
        h(NSpace, { vertical: true, size: 14 }, () => [
          h(NRadio, { value: 'empty' }, () => '不填，允许通配符 [, - * /]'),
          h(NRadio, { value: 'every' }, () => '每年'),
          renderRangeRow({ ...panelProps, label: '年', options }, panelEmit),
          renderStepRow({ ...panelProps, label: '年', options }, panelEmit),
          renderAppointRow({ ...panelProps, label: '年', options }, panelEmit)
        ])
      );
  }
});

function renderRangeRow(propsLike: any, emitLike: any) {
  return h(NRadio, { value: 'range' }, () =>
    h(NSpace, { align: 'center' }, () => [
      '周期从',
      renderInputNumber(propsLike.start, propsLike.min, propsLike.max - 1, value => emitLike('update:start', value)),
      '-',
      renderInputNumber(propsLike.end, propsLike.start + 1, propsLike.max, value => emitLike('update:end', value)),
      propsLike.label
    ])
  );
}

function renderStepRow(propsLike: any, emitLike: any) {
  return h(NRadio, { value: 'step' }, () =>
    h(NSpace, { align: 'center' }, () => [
      '从',
      renderInputNumber(propsLike.start, propsLike.min, propsLike.max - 1, value => emitLike('update:start', value)),
      `${propsLike.label}开始，每`,
      renderInputNumber(propsLike.step, 1, Math.max(1, propsLike.max - propsLike.start), value => emitLike('update:step', value)),
      `${propsLike.label}执行一次`
    ])
  );
}

function renderAppointRow(propsLike: any, emitLike: any) {
  return h(NRadio, { value: 'appoint', class: 'crontab-appoint-radio' }, () =>
    h('div', { class: 'crontab-appoint-block' }, [
      `指定${propsLike.label}`,
      propsLike.mode === 'appoint'
        ? renderValueGrid(
            propsLike.values,
            numberOptions(propsLike.min, propsLike.max, propsLike.options),
            next => emitLike('update:values', next),
            () => emitLike('update:mode', 'appoint')
          )
        : null
    ])
  );
}

function renderValueGrid(
  values: number[],
  options: SelectOption[],
  onUpdate: (value: number[]) => void,
  onActivate?: () => void
) {
  const selectedValues = new Set(values);
  const optionValues = options.map(item => Number(item.value));
  const min = Math.min(...optionValues);
  const max = Math.max(...optionValues);
  const updateValues = (next: number[]) => {
    onActivate?.();
    onUpdate(normalizeValues(next, min, max));
  };
  return h(
    'div',
    { class: 'crontab-value-picker' },
    [
      h('div', { class: 'crontab-value-toolbar' }, [
        renderValueAction('全选', () => updateValues(optionValues)),
        renderValueAction('清空', () => updateValues([])),
        renderValueAction('奇数', () => updateValues(optionValues.filter(item => item % 2 === 1))),
        renderValueAction('偶数', () => updateValues(optionValues.filter(item => item % 2 === 0)))
      ]),
      h(
        'div',
        { class: 'crontab-value-grid' },
        options.map(option => {
          const value = Number(option.value);
          const selected = selectedValues.has(value);
          return h(
            'button',
            {
              type: 'button',
              class: ['crontab-value-button', selected && 'is-selected'],
              onClick: (event: MouseEvent) => {
                event.stopPropagation();
                const next = selected ? values.filter(item => item !== value) : [...values, value];
                updateValues(next);
              }
            },
            String(option.label)
          );
        })
      )
    ]
  );
}

function renderValueAction(label: string, onClick: () => void) {
  return h(
    'button',
    {
      type: 'button',
      class: 'crontab-value-action',
      onClick: (event: MouseEvent) => {
        event.stopPropagation();
        onClick();
      }
    },
    label
  );
}

function renderInputNumber(value: number, min: number, max: number, onUpdate: (value: number) => void) {
  return h(NInputNumber, {
    value,
    min,
    max,
    size: 'small',
    class: 'crontab-number',
    'onUpdate:value': next => onUpdate(clamp(next, min, max))
  });
}

function renderSelect(value: number, options: SelectOption[], onUpdate: (value: number) => void) {
  return h(NSelect, {
    value,
    options,
    size: 'small',
    class: 'crontab-week-select',
    'onUpdate:value': onUpdate
  });
}
</script>

<style scoped>
.crontab-picker {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.crontab-select {
  width: 260px;
}

.crontab-appoint-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.crontab-appoint-radio {
  align-items: flex-start;
}

:deep(.crontab-appoint-radio .n-radio__label) {
  width: min(760px, calc(100vw - 220px));
}

.crontab-value-picker {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: 32px;
  padding: 12px;
  border: 1px solid rgb(226 232 240);
  border-radius: 8px;
  background: rgb(248 250 252);
}

.crontab-value-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.crontab-value-action,
.crontab-value-button {
  border: 1px solid rgb(203 213 225);
  border-radius: 6px;
  background: rgb(255 255 255);
  color: rgb(51 65 85);
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
}

.crontab-value-action {
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.crontab-value-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(44px, 1fr));
  gap: 8px;
  max-height: 176px;
  overflow-y: auto;
}

.crontab-value-button {
  height: 32px;
  padding: 0 6px;
  font-size: 13px;
}

.crontab-value-action:hover,
.crontab-value-button:hover {
  border-color: rgb(99 102 241);
  color: rgb(79 70 229);
}

.crontab-value-button.is-selected {
  border-color: rgb(99 102 241);
  background: rgb(99 102 241);
  color: rgb(255 255 255);
}

.crontab-week-select {
  width: 120px;
}

.crontab-number {
  width: 96px;
}

.crontab-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgb(226 232 240);
  border-radius: 8px;
  background: rgb(248 250 252);
}

.crontab-result-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  font-size: 12px;
  color: rgb(100 116 139);
}

.crontab-result-value code,
.crontab-expression code {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  padding: 3px 6px;
  border-radius: 6px;
  background: rgb(255 255 255);
  color: rgb(15 23 42);
  font-size: 12px;
}

.crontab-expression {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  color: rgb(100 116 139);
}
</style>
