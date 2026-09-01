export const icons = {
  sun: '<circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />',
  moon: '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />',
  arrowLeft: '<path d="M19 12H5M12 19l-7-7 7-7" />',
  plus: '<path d="M12 5v14M5 12h14" />',
  copy: '<rect x="9" y="9" width="13" height="13" rx="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />',
  check: '<path d="M20 6 9 17l-5-5" />',
  trash: '<path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M10 11v6M14 11v6" />',
  pause: '<rect x="6" y="4" width="4" height="16" rx="1" /><rect x="14" y="4" width="4" height="16" rx="1" />',
  play: '<path d="M6 4l14 8-14 8V4z" />',
  replay: '<path d="M3 2v6h6" /><path d="M3.51 15a9 9 0 1 0 2.13-9.36L3 8" />',
  activity: '<path d="M22 12h-4l-3 9L9 3l-3 9H2" />',
  inbox: '<path d="M22 12h-6l-2 3h-4l-2-3H2" /><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />',
  send: '<path d="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7z" />',
  gauge: '<path d="M12 14a2 2 0 0 0 2-2c0-1.1-2-6-2-6s-2 4.9-2 6a2 2 0 0 0 2 2z" /><path d="M4.6 19a9 9 0 1 1 14.8 0" />',
  clock: '<circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />',
  alert: '<path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><path d="M12 9v4M12 17h.01" />',
  close: '<path d="M18 6 6 18M6 6l12 12" />',
  layers: '<path d="M12 2 2 7l10 5 10-5-10-5z" /><path d="M2 17l10 5 10-5M2 12l10 5 10-5" />',
} as const

export type IconName = keyof typeof icons
