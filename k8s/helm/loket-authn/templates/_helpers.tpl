{{- define "loket-authn.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "loket-authn.fullname" -}}
{{- printf "%s-%s" (include "loket-authn.name" .) .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "loket-authn.chart" -}}
{{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

