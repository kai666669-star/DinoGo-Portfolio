import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js'

const fallbackPalette = {
  sales: '#657a6d',
  orders: '#9a7b42',
  averageOrderValue: '#c73e3a',
  quantity: '#64748b',
  text: '#38423d',
  muted: '#66706a',
  border: '#e8e6e1',
  surface: '#ffffff',
}

export function registerSellerLineChart() {
  ChartJS.register(CategoryScale, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip)
}

export function createSellerChartPalette() {
  if (typeof window === 'undefined') {
    return { ...fallbackPalette }
  }

  const styles = window.getComputedStyle(document.documentElement)
  return {
    sales: getCssColor(styles, '--color-primary-600', fallbackPalette.sales),
    orders: getCssColor(styles, '--color-warning', fallbackPalette.orders),
    averageOrderValue: getCssColor(styles, '--color-danger', fallbackPalette.averageOrderValue),
    quantity: getCssColor(styles, '--color-info', fallbackPalette.quantity),
    text: getCssColor(styles, '--color-text-700', fallbackPalette.text),
    muted: getCssColor(styles, '--color-text-muted', fallbackPalette.muted),
    border: getCssColor(styles, '--color-border', fallbackPalette.border),
    surface: getCssColor(styles, '--color-surface', fallbackPalette.surface),
  }
}

export function createSellerLineDataset(option, values, palette, fill = false) {
  const color = palette[option.key] || option.color || fallbackPalette.sales

  return {
    label: option.label,
    data: values,
    borderColor: color,
    backgroundColor: toAlpha(color, 0.12),
    pointBackgroundColor: palette.surface,
    pointBorderColor: color,
    pointHoverBackgroundColor: color,
    pointHoverBorderColor: palette.surface,
    pointRadius: 3,
    pointHoverRadius: 5,
    borderWidth: 2,
    tension: 0.36,
    fill,
  }
}

export function createSellerLineChartOptions({
  palette,
  formatValue,
  formatTooltipTitle = (label) => label,
}) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      intersect: false,
      mode: 'index',
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: palette.text,
        displayColors: true,
        padding: 12,
        callbacks: {
          title(items) {
            return formatTooltipTitle(items[0]?.label || '')
          },
          label(context) {
            const value = Number(context.raw || 0)
            return `${context.dataset.label}: ${formatValue(value, context.dataset.label)}`
          },
        },
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          color: palette.muted,
          maxRotation: 0,
          autoSkip: true,
          autoSkipPadding: 18,
          maxTicksLimit: 8,
          callback(_, index) {
            return formatDateLabel(this.getLabelForValue(index))
          },
        },
      },
      y: {
        beginAtZero: false,
        grid: {
          color: palette.border,
        },
        ticks: {
          color: palette.muted,
          precision: 0,
          callback(value) {
            return Number(value || 0).toLocaleString('zh-TW')
          },
        },
      },
    },
  }
}

export function formatDateLabel(label) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(label)) {
    return label
  }

  return label.slice(5).replace('-', '/')
}

export function formatFullDateLabel(label) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(label)) {
    return label
  }

  return label.replaceAll('-', '/')
}

function getCssColor(styles, token, fallback) {
  return styles.getPropertyValue(token).trim() || fallback
}

function toAlpha(color, alpha) {
  const normalized = color.trim()
  if (!normalized.startsWith('#') || ![4, 7].includes(normalized.length)) {
    return normalized
  }

  const hex =
    normalized.length === 4
      ? `#${normalized[1]}${normalized[1]}${normalized[2]}${normalized[2]}${normalized[3]}${normalized[3]}`
      : normalized
  const red = Number.parseInt(hex.slice(1, 3), 16)
  const green = Number.parseInt(hex.slice(3, 5), 16)
  const blue = Number.parseInt(hex.slice(5, 7), 16)

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}
