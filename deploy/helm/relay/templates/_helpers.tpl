{{- define "relay.name" -}}relay{{- end }}
{{- define "relay.fullname" -}}{{ .Release.Name }}-relay{{- end }}
{{- define "relay.labels" -}}
app.kubernetes.io/name: {{ include "relay.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
{{- define "relay.nginxConfig" -}}
server {
  listen 8080;
  root /usr/share/nginx/html;
  location /api/ {
    proxy_pass http://{{ include "relay.fullname" . }}-backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
  location /in/ {
    proxy_pass http://{{ include "relay.fullname" . }}-backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
  location /ws {
    proxy_pass http://{{ include "relay.fullname" . }}-backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
  location / { try_files $uri $uri/ /index.html; }
}
{{- end }}
