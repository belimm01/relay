<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { Overview } from '@/api/types'
import { compactNumber } from '@/lib/format'
import { pushToast } from '@/lib/toast'
import AppIcon from '@/components/AppIcon.vue'
import SkeletonBlock from '@/components/SkeletonBlock.vue'
import type { IconName } from '@/lib/icons'

const overview = ref<Overview | null>(null)
const loading = ref(true)

async function load() {
  try {
    overview.value = await api.getOverview()
  } catch {
    pushToast('Could not load overview metrics.', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

const W = 760
const H = 260
const padL = 44
const padR = 12
const padT = 16
const padB = 28
const plotW = W - padL - padR
const plotH = H - padT - padB
const baseY = padT + plotH

function niceMax(value: number): number {
  if (value <= 0) return 4
  const pow = Math.pow(10, Math.floor(Math.log10(value)))
  for (const step of [1, 2, 2.5, 5, 10]) {
    if (pow * step >= value) return pow * step
  }
  return pow * 10
}

function dayLabel(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  })
}

const series = computed(() => overview.value?.eventsPerDay ?? [])
const maxCount = computed(() => Math.max(0, ...series.value.map((d) => d.count)))
const yMax = computed(() => niceMax(maxCount.value))
const windowTotal = computed(() => series.value.reduce((sum, d) => sum + d.count, 0))

const yTicks = computed(() => {
  const top = yMax.value
  return [0, top / 2, top].map((value) => ({
    value,
    y: baseY - (value / top) * plotH,
  }))
})

const bars = computed(() => {
  const data = series.value
  const n = data.length || 1
  const slot = plotW / n
  const barW = Math.min(22, slot - 8)
  return data.map((d, i) => {
    const h = yMax.value === 0 ? 0 : (d.count / yMax.value) * plotH
    const x = padL + i * slot + (slot - barW) / 2
    const y = baseY - h
    return {
      i,
      date: d.date,
      count: d.count,
      x,
      y,
      w: barW,
      slotX: padL + i * slot,
      slotW: slot,
      centerX: x + barW / 2,
      path: barPath(x, y, barW, h),
    }
  })
})

function barPath(x: number, y: number, w: number, h: number): string {
  if (h <= 0.5) return ''
  const r = Math.min(4, w / 2, h)
  return `M${x},${baseY} L${x},${y + r} Q${x},${y} ${x + r},${y} L${x + w - r},${y} Q${x + w},${y} ${x + w},${y + r} L${x + w},${baseY} Z`
}

const axisLabels = computed(() => {
  const n = bars.value.length
  if (n === 0) return []
  const indexes = new Set([0, Math.floor(n / 2), n - 1])
  return bars.value
    .filter((b) => indexes.has(b.i))
    .map((b) => ({ x: b.centerX, label: dayLabel(b.date) }))
})

const hovered = ref<number | null>(null)
const hoverBar = computed(() =>
  hovered.value === null ? null : bars.value[hovered.value] ?? null,
)

const tiles = computed(() => {
  const o = overview.value
  if (!o) return []
  const rate = Math.round(o.successRate * 100)
  const latency = o.averageLatencyMs
  return [
    {
      key: 'endpoints',
      icon: 'inbox' as IconName,
      label: 'Endpoints',
      value: String(o.totals.endpoints),
      sub: `${o.totals.activeEndpoints} active · ${o.totals.destinations} destinations`,
    },
    {
      key: 'events',
      icon: 'activity' as IconName,
      label: 'Events captured',
      value: compactNumber(o.totals.events),
      sub: `${compactNumber(windowTotal.value)} in last 14 days`,
    },
    {
      key: 'rate',
      icon: 'gauge' as IconName,
      label: 'Delivery success',
      value: o.deliveries.total === 0 ? '—' : `${rate}%`,
      sub:
        o.deliveries.total === 0
          ? 'No deliveries yet'
          : `${compactNumber(o.deliveries.delivered)} of ${compactNumber(o.deliveries.total)} delivered`,
    },
    {
      key: 'latency',
      icon: 'clock' as IconName,
      label: 'Avg latency',
      value: latency == null ? '—' : `${Math.round(latency)} ms`,
      sub: 'Successful deliveries',
    },
  ]
})

const statusMeta: { key: keyof Overview['deliveries']; label: string; cls: string }[] = [
  { key: 'delivered', label: 'Delivered', cls: 'good' },
  { key: 'pending', label: 'Pending', cls: 'warning' },
  { key: 'retrying', label: 'Retrying', cls: 'serious' },
  { key: 'failed', label: 'Failed', cls: 'critical' },
]

const breakdown = computed(() => {
  const d = overview.value?.deliveries
  if (!d) return []
  return statusMeta.map((s) => ({
    ...s,
    count: d[s.key] as number,
    pct: d.total === 0 ? 0 : ((d[s.key] as number) / d.total) * 100,
  }))
})

const deliveryTotal = computed(() => overview.value?.deliveries.total ?? 0)
</script>

<template>
  <div class="stack">
    <div>
      <h1>Overview</h1>
      <p class="muted">A live snapshot of your webhook traffic and delivery health.</p>
    </div>

    <div v-if="loading" class="tiles">
      <div v-for="n in 4" :key="n" class="card tile">
        <SkeletonBlock width="60%" height="12px" />
        <SkeletonBlock width="45%" height="28px" />
        <SkeletonBlock width="70%" height="12px" />
      </div>
    </div>

    <template v-else-if="overview">
      <div class="tiles">
        <div v-for="tile in tiles" :key="tile.key" class="card tile">
          <div class="tile-head">
            <span class="tile-icon"><AppIcon :name="tile.icon" :size="16" /></span>
            <span class="tile-label">{{ tile.label }}</span>
          </div>
          <div class="tile-value">{{ tile.value }}</div>
          <div class="tile-sub muted">{{ tile.sub }}</div>
        </div>
      </div>

      <div class="split">
        <section class="card stack chart-card">
          <div class="spread">
            <h2>Events per day</h2>
            <span class="muted small">Last 14 days</span>
          </div>

          <div class="chart-wrap">
            <svg
              class="chart"
              :viewBox="`0 0 ${W} ${H}`"
              preserveAspectRatio="xMidYMid meet"
              role="img"
              :aria-label="`Events captured per day over the last 14 days, ${windowTotal} in total`"
            >
              <line
                v-for="tick in yTicks"
                :key="`g${tick.value}`"
                :x1="padL"
                :x2="W - padR"
                :y1="tick.y"
                :y2="tick.y"
                class="grid"
              />
              <text
                v-for="tick in yTicks"
                :key="`t${tick.value}`"
                :x="padL - 8"
                :y="tick.y + 4"
                class="axis-y"
                text-anchor="end"
              >
                {{ Math.round(tick.value) }}
              </text>

              <path
                v-for="bar in bars"
                :key="`b${bar.i}`"
                :d="bar.path"
                class="bar"
                :class="{ active: hovered === bar.i }"
              />

              <text
                v-for="(label, idx) in axisLabels"
                :key="`x${idx}`"
                :x="label.x"
                :y="H - 8"
                class="axis-x"
                text-anchor="middle"
              >
                {{ label.label }}
              </text>

              <rect
                v-for="bar in bars"
                :key="`h${bar.i}`"
                :x="bar.slotX"
                :y="padT"
                :width="bar.slotW"
                :height="plotH"
                fill="transparent"
                @mouseenter="hovered = bar.i"
                @mouseleave="hovered = null"
              />
            </svg>

            <div
              v-if="hoverBar"
              class="tip"
              :style="{
                left: `${(hoverBar.centerX / W) * 100}%`,
                top: `${(hoverBar.y / H) * 100}%`,
              }"
            >
              <strong>{{ hoverBar.count }}</strong>
              <span class="muted">{{ dayLabel(hoverBar.date) }}</span>
            </div>
          </div>
        </section>

        <section class="card stack">
          <h2>Delivery status</h2>

          <template v-if="deliveryTotal > 0">
            <div class="meter">
              <span
                v-for="seg in breakdown.filter((s) => s.count > 0)"
                :key="seg.key"
                class="seg"
                :class="seg.cls"
                :style="{ flexGrow: seg.count }"
              />
            </div>
            <ul class="legend">
              <li v-for="seg in breakdown" :key="seg.key">
                <span class="dot" :class="seg.cls" />
                <span class="legend-label">{{ seg.label }}</span>
                <span class="legend-count mono">{{ seg.count }}</span>
                <span class="legend-pct muted">{{ Math.round(seg.pct) }}%</span>
              </li>
            </ul>
          </template>

          <div v-else class="empty-block muted">
            <AppIcon name="send" :size="20" />
            <p>No deliveries recorded yet. Add a destination to start forwarding events.</p>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.tiles {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.tile {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.tile-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--muted);
}

.tile-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  background: var(--accent-weak);
  color: var(--accent);
}

.tile-label {
  font-size: var(--text-xs);
  font-weight: 550;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.tile-value {
  font-size: var(--text-2xl);
  font-weight: 680;
  letter-spacing: -0.02em;
}

.tile-sub {
  font-size: var(--text-xs);
}

.split {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
  align-items: start;
}

.chart-card {
  min-width: 0;
}

.small {
  font-size: var(--text-xs);
}

.chart-wrap {
  position: relative;
  width: 100%;
}

.chart {
  width: 100%;
  height: auto;
  display: block;
}

.grid {
  stroke: var(--chart-grid);
  stroke-width: 1;
}

.axis-y,
.axis-x {
  fill: var(--muted);
  font-size: 11px;
  font-family: var(--sans);
}

.bar {
  fill: var(--chart-fill);
  transition: fill var(--dur-fast) var(--ease-standard);
}

.bar.active {
  fill: var(--accent-hover);
}

.tip {
  position: absolute;
  transform: translate(-50%, calc(-100% - 8px));
  background: var(--surface-3);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  padding: 5px 9px;
  box-shadow: var(--shadow-2);
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  pointer-events: none;
  white-space: nowrap;
}

.tip strong {
  font-size: var(--text-base);
}

.tip .muted {
  font-size: 11px;
}

.meter {
  display: flex;
  gap: 2px;
  height: 12px;
  border-radius: var(--radius-pill);
  overflow: hidden;
  background: var(--chart-track);
}

.seg {
  min-width: 4px;
  border-radius: 2px;
}

.seg.good,
.dot.good {
  background: var(--status-good);
}

.seg.warning,
.dot.warning {
  background: var(--status-warning);
}

.seg.serious,
.dot.serious {
  background: var(--status-serious);
}

.seg.critical,
.dot.critical {
  background: var(--status-critical);
}

.legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: none;
}

.legend-label {
  flex: 1;
}

.legend-count {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.legend-pct {
  width: 42px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  text-align: center;
  padding: var(--space-6) var(--space-4);
}

.empty-block p {
  margin: 0;
}

@media (max-width: 860px) {
  .tiles {
    grid-template-columns: repeat(2, 1fr);
  }

  .split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .tiles {
    grid-template-columns: 1fr;
  }
}
</style>
