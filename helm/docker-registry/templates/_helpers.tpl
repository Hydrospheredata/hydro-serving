{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "docker-registry.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "docker-registry.fullname" -}}
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
{{- define "docker-registry.ingress.apiVersion" -}}
{{- if .Capabilities.APIVersions.Has "extensions/v1beta1" -}}
{{- print "extensions/v1beta1" -}}
{{- else -}}
{{- print "networking.k8s.io/v1beta1" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "docker-registry.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "docker-registry-proxy.fullname" -}}
{{- printf "%s-%s" .Release.Name "docker-registry-proxy" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Define region for docker registry
*/}}
{{- define "docker-registry.region" }}
{{- if .Values.global.registry.persistence.region }}
{{- print .Values.global.registry.persistence.region }}
{{- else }}
{{- print .Values.global.persistence.region }}
{{- end }}
{{- end }}

{{/*
Create storageInit script contents
*/}}
{{- define "docker-registry.storageInit" -}}
{{ printf "#!/bin/bash\n" }}
{{ printf "set -e\n" }}
{{- if .Values.global.persistence.url -}}
{{ printf "mc alias set storage %s %s %s\n" .Values.global.persistence.url .Values.global.persistence.accessKey .Values.global.persistence.secretKey }}
{{- else -}}
{{ printf "mc alias set storage http://%s:9000 %s %s\n" (include "minio.fullname" .) .Values.global.persistence.accessKey .Values.global.persistence.secretKey }}
{{- end -}}
{{ printf "mc mb -p --region %s storage/%s" (include "docker-registry.region" .) .Values.global.registry.persistence.bucket }}
{{- end -}}

{{- define "minio.fullname" -}}
{{- printf "%s-%s" .Release.Name "minio" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
