{{/*
Имя чарта (усечённое до 63 символов)
*/}}
{{- define "jwt-api.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Полное имя релиза (release-name + chart-name)
*/}}
{{- define "jwt-api.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "jwt-api.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Версия чарта для метки helm.sh/chart
*/}}
{{- define "jwt-api.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Общие метки для всех ресурсов
*/}}
{{- define "jwt-api.labels" -}}
helm.sh/chart: {{ include "jwt-api.chart" . }}
{{ include "jwt-api.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Метки-селекторы для Service и Deployment
*/}}
{{- define "jwt-api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "jwt-api.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
