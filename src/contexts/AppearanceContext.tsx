import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

export type ThemeMode = 'light' | 'dark' | 'system'
export type AccentColor = 'green' | 'blue' | 'violet' | 'orange' | 'rose' | 'cyan'
export type Density = 'compact' | 'normal' | 'spacious'

export const ACCENT_COLORS: Record<AccentColor, { hsl: string; label: string }> = {
  green:  { hsl: '162 63% 41%', label: 'Vert' },
  blue:   { hsl: '215 80% 55%', label: 'Bleu' },
  violet: { hsl: '263 70% 60%', label: 'Violet' },
  orange: { hsl: '22 90% 52%',  label: 'Orange' },
  rose:   { hsl: '340 75% 58%', label: 'Rose' },
  cyan:   { hsl: '190 75% 42%', label: 'Cyan' },
}

interface AppearanceContextValue {
  themeMode: ThemeMode
  setThemeMode: (mode: ThemeMode) => void
  accentColor: AccentColor
  setAccentColor: (color: AccentColor) => void
  reduceMotion: boolean
  setReduceMotion: (value: boolean) => void
  density: Density
  setDensity: (density: Density) => void
}

function getInitialTheme(): ThemeMode {
  const v = localStorage.getItem('frigo-theme-mode')
  if (v === 'light' || v === 'dark' || v === 'system') return v
  const legacy = localStorage.getItem('frigo-dark-mode')
  if (legacy === 'true') return 'dark'
  if (legacy === 'false') return 'light'
  return 'system'
}

function getInitialAccent(): AccentColor {
  const v = localStorage.getItem('frigo-accent-color')
  if (v && v in ACCENT_COLORS) return v as AccentColor
  return 'green'
}

function getInitialDensity(): Density {
  const v = localStorage.getItem('frigo-density')
  if (v === 'compact' || v === 'normal' || v === 'spacious') return v
  return 'normal'
}

function getInitialReduceMotion(): boolean {
  const v = localStorage.getItem('frigo-reduce-motion')
  if (v !== null) return v === 'true'
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

const AppearanceContext = createContext<AppearanceContextValue | null>(null)

export function AppearanceProvider({ children }: { children: ReactNode }) {
  const [themeMode, setThemeModeState] = useState<ThemeMode>(getInitialTheme)
  const [accentColor, setAccentColorState] = useState<AccentColor>(getInitialAccent)
  const [reduceMotion, setReduceMotionState] = useState<boolean>(getInitialReduceMotion)
  const [density, setDensityState] = useState<Density>(getInitialDensity)

  useEffect(() => {
    const applyTheme = (isDark: boolean) => {
      document.documentElement.classList.toggle('dark', isDark)
    }
    if (themeMode === 'light') { applyTheme(false); return }
    if (themeMode === 'dark')  { applyTheme(true);  return }
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    applyTheme(mq.matches)
    const handler = (e: MediaQueryListEvent) => applyTheme(e.matches)
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [themeMode])

  useEffect(() => {
    const { hsl } = ACCENT_COLORS[accentColor]
    document.documentElement.style.setProperty('--primary', hsl)
    document.documentElement.style.setProperty('--ring', hsl)
  }, [accentColor])

  useEffect(() => {
    document.documentElement.classList.toggle('reduce-motion', reduceMotion)
  }, [reduceMotion])

  const setThemeMode = (mode: ThemeMode) => {
    localStorage.setItem('frigo-theme-mode', mode)
    setThemeModeState(mode)
  }
  const setAccentColor = (color: AccentColor) => {
    localStorage.setItem('frigo-accent-color', color)
    setAccentColorState(color)
  }
  const setReduceMotion = (value: boolean) => {
    localStorage.setItem('frigo-reduce-motion', String(value))
    setReduceMotionState(value)
  }
  const setDensity = (d: Density) => {
    localStorage.setItem('frigo-density', d)
    setDensityState(d)
  }

  return (
    <AppearanceContext.Provider value={{ themeMode, setThemeMode, accentColor, setAccentColor, reduceMotion, setReduceMotion, density, setDensity }}>
      {children}
    </AppearanceContext.Provider>
  )
}

export function useAppearance() {
  const ctx = useContext(AppearanceContext)
  if (!ctx) throw new Error('useAppearance must be used within AppearanceProvider')
  return ctx
}
