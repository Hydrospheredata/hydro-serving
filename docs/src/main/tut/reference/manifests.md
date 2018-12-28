---
layout: docs
title:  "Manifests"
permalink: 'manifests.html'
---

# Manifests

In this page we've defined all possible values for certain type of fields of different manifests. 

<br> 

### `kind: Model` 

```yaml
kind: Model
...
contract:
  signature_name:
    inputs:
      field_name:
        shape: scalar
        type: int32
        profile: numeric
...
``` 

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `shape` | Describes the shape of the input/ouput tensor. | `scalar` | Single number |
| | | `-1` | Arbitrary shape | 
| | | `1`, `2`, `3`, ... | Any positive number |
| | | | |
| `type` |  Describes the data type of incoming tensor. | `bool` | Boolean | 
| | | `string`      | String | 
| | | `half`, `float16` | 16-bit half-precision floating-point |
| | | `float32`     | 32-bit single-precision floating-point | 
| | | `double`, `float64`      | 64-bit double-precision floating-point | 
| | | `uint8`       | 8-bit unsigned integer | 
| | | `uint16`      | 16-bit unsigned integer | 
| | | `uint32`      | 32-bit unsigned integer | 
| | | `uint64`      | 64-bit unsigned integer | 
| | | `int8`        | 8-bit signed integer | 
| | | `int16`       | 16-bit signed integer | 
| | | `int32`       | 32-bit signed integer | 
| | | `int64`       | 64-bit signed integer | 
| | | `qint8`       | Quantized 8-bit signed integer | 
| | | `quint8`      | Quantized 8-bit unsigned integer | 
| | | `qint16`      | Quantized 16-bit signed integer | 
| | | `quint16`     | Quantized 16-bit unsigned integer |
| | | `complex64`   | 64-bit single-precision complex | 
| | | `complex128`  | 128-bit double-precision complex | 
| | | | |
| `profile` | Describes the nature of the data. | `text` | Monitoring such fields will be done with __text__-oriented algorithms. |
| | | `image` | Monitoring such fields will be done with __image__-oriented algorithms. | 
| | | `numerical` | Monitoring such fields will be done with __numerical__-oriented algorithms.|
