{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "serving.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "serving.fullname" -}}
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
{{- define "serving.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "sonar.fullname" -}}
{{- printf "%s-%s" .Release.Name "sonar" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "rootcause.fullname" -}}
{{- printf "%s-%s" .Release.Name "rootcause" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "manager.fullname" -}}
{{- printf "%s-%s" .Release.Name "manager" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "ui.fullname" -}}
{{- printf "%s-%s" .Release.Name "ui" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "gateway.fullname" -}}
{{- printf "%s-%s" .Release.Name "gateway" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "prometheus-am.fullname" -}}
{{- printf "%s-%s" .Release.Name "prometheus-am" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "timemachine.fullname" -}}
{{- printf "%s-%s" .Release.Name "timemachine" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "postgresql.fullname" -}}
{{- printf "%s-%s" .Release.Name "postgresql" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mongodb.fullname" -}}
{{- printf "%s-%s" .Release.Name "mongodb" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "docker-registry.fullname" -}}
{{- printf "%s-%s" .Release.Name "docker-registry" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "docker-registry-proxy.fullname" -}}
{{- printf "%s-%s" .Release.Name "docker-registry-proxy" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
