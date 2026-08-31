{{- define "relay.name" -}}relay{{- end }}
{{- define "relay.fullname" -}}{{ .Release.Name }}-relay{{- end }}
{{- define "relay.labels" -}}
app.kubernetes.io/name: {{ include "relay.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
