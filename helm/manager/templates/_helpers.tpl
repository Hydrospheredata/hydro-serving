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
{{- if .Values.global.dockerRegistry.host -}}
{{- printf "{\"auths\": {\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}}}" .Values.global.dockerRegistry.host .Values.global.dockerRegistry.username .Values.global.dockerRegistry.password (printf "%s:%s" .Values.global.dockerRegistry.username .Values.global.dockerRegistry.password | b64enc) | b64enc }}
{{- else -}}
{{- printf "{\"auths\": {\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}}}" (printf "%s.%s.%s" (include "docker-registry.fullname" .) .Release.Namespace "svc.cluster.local:5000") .Values.global.dockerRegistry.username .Values.global.dockerRegistry.password (printf "%s:%s" .Values.global.dockerRegistry.username .Values.global.dockerRegistry.password | b64enc) | b64enc }}
{{- end -}}
{{- end -}}

{{/*
Create dockerconfig.json for localhost to pull images on host machines
*/}}
{{- define "manager.dockerconfig.local" }}
{{- printf "{\"auths\": {\"localhost:5000\": {\"username\": \"%s\", \"password\": \"%s\", \"auth\": \"%s\"}}}" .Values.global.dockerRegistry.username .Values.global.dockerRegistry.password (printf "%s:%s" .Values.global.dockerRegistry.username .Values.global.dockerRegistry.password | b64enc) | b64enc }}
{{- end }}

{{/*
Create daemon.json config for docker daemon
*/}}
{{- define "manager.daemonconfig" -}}
{{- if .Values.global.dockerRegistry.host -}}
{{- printf "{ \"insecure-registries\":[%s] }" (printf .Values.global.dockerRegistry.host | quote) -}}
{{- else -}}
{{- printf "{ \"insecure-registries\":[\"%s\"] }" (printf "%s.%s.%s" (include "docker-registry.fullname" .) .Release.Namespace "svc.cluster.local:5000") -}}
{{- end -}}
{{- end -}}

{{- define "postgres.fullname" -}}
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
