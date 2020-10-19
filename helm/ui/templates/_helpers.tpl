{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "ui.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ui.fullname" -}}
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
Return the appropriate apiVersion for ingress.
*/}}
{{- define "ui.ingress.apiVersion" -}}
{{- if .Capabilities.APIVersions.Has "extensions/v1beta1" -}}
{{- print "extensions/v1beta1" -}}
{{- else -}}
{{- print "networking.k8s.io/v1beta1" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ui.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "manager.fullname" -}}
{{- printf "%s-%s" .Release.Name "manager" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "sonar.fullname" -}}
{{- printf "%s-%s" .Release.Name "sonar" | trunc 63 | trimSuffix "-" -}}
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

{{- define "visualization.fullname" -}}
{{- printf "%s-%s" .Release.Name "visualization" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "rootcause.fullname" -}}
{{- printf "%s-%s" .Release.Name "rootcause" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "stat.fullname" -}}
{{- printf "%s-%s" .Release.Name "stat" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "auto-od.fullname" -}}
{{- printf "%s-%s" .Release.Name "auto-od" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "ui.ingress-http-path" -}}
{{- if eq .Values.global.ingress.path "/" -}}
{{- print "/" -}}
{{- else -}}
{{- printf "%s(/|$)(.*)" .Values.global.ingress.path}}
{{- end -}}
{{- end -}}
