{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "manager.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "manager.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "manager.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create dockerconfig.json contents
*/}}
{{- define "manager.dockerconfig" -}}
{{- if .Values.global.registry.ingress.enabled }}
{{- printf "{\"auths\": {\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}}}" (include "docker-registry.ingress.url" .) .Values.global.registry.username .Values.global.registry.password (printf "%s:%s" .Values.global.registry.username .Values.global.registry.password | b64enc) | b64enc }}
{{- else if (eq .Values.global.registry.url "") }}
{{- printf "{\"auths\": {\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}, \"localhost:5000\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}}}" (printf "%s-docker-registry.%s.svc.cluster.local:5000" .Release.Name .Release.Namespace) .Values.global.registry.username .Values.global.registry.password (printf "%s:%s" .Values.global.registry.username .Values.global.registry.password | b64enc) .Values.global.registry.username .Values.global.registry.password (printf "%s:%s" .Values.global.registry.username .Values.global.registry.password | b64enc) | b64enc }}
{{- else }}
{{- printf "{\"auths\": {\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}}}" .Value.global.registry.url .Values.global.registry.username .Values.global.registry.password (printf "%s:%s" .Values.global.registry.username .Values.global.registry.password | b64enc) | b64enc }}
{{- end }}
{{- end }}

{{- define "postgresql.fullname" -}}
{{- printf "%s-%s" .Release.Name "postgresql" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mongo.fullname" -}}
{{- printf "%s-%s" .Release.Name "mongodb" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "docker-registry.fullname" -}}
{{- printf "%s-%s" .Release.Name "docker-registry" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "docker-registry-proxy.fullname" -}}
{{- printf "%s-%s" .Release.Name "docker-registry-proxy" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create joined url for the registry.
*/}}
{{- define "docker-registry.ingress.url" -}}
{{- printf "%s%s" .Values.global.registry.ingress.host .Values.global.registry.ingress.path -}}
{{- end -}}
