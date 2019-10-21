# Model contract

In this page we've defined all possible values for certain type of fields 
of different manifests. 

## Model

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
metadata:
  experiment: "demo" 
...
``` 

<div class="flexible-table">
    <table>
        <thead>
            <tr>
                <th>Field</th>
                <th>Definition</th>
                <th>Type</th>
                <th>Type Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td rowspan="3"><code>shape</code></td>
                <td rowspan="3">Describes the shape of the input/output tensor.</td>
                <td><code>scalar</code></td>
                <td>Single element</td>
            </tr>
            <tr>
                <td><code>-1</code></td>
                <td>Arbitrary shape</td>
            </tr>
            <tr>
                <td><code>1</code>, <code>2</code>, <code>3</code>, …</td>
                <td>Any positive number</td>
            </tr>
            <tr>
                <td rowspan="19"><code>type</code></td>
                <td rowspan="19">Describes the data type of incoming tensor.</td>
                <td><code>bool</code></td>
                <td>Boolean.</td>
            </tr>
            <tr>
                <td><code>string</code></td>
                <td>String in bytes.</td>
            </tr>
            <tr>
                <td><code>half</code>, <code>float16</code></td>
                <td>16-bit half-precision floating-point.</td>
            </tr>
            <tr>
                <td><code>float32</code></td>
                <td>32-bit single-precision floating-point.</td>
            </tr>
            <tr>
                <td><code>double</code>, <code>float64</code></td>
                <td>64-bit double-precision floating-point.</td>
            </tr>
            <tr>
                <td><code>uint8</code></td>
                <td>8-bit unsigned integer.</td>
            </tr>
            <tr>
                <td><code>uint16</code></td>
                <td>16-bit unsigned integer.</td>
            </tr>
            <tr>
                <td><code>uint32</code></td>
                <td>32-bit unsigned integer.</td>
            </tr>
            <tr>
                <td><code>uint64</code></td>
                <td>64-bit unsigned integer.</td>
            </tr>
            <tr>
                <td><code>int8</code></td>
                <td>8-bit signed integer.</td>
            </tr>
            <tr>
                <td><code>int16</code></td>
                <td>16-bit signed integer.</td>
            </tr>
            <tr>
                <td><code>int32</code></td>
                <td>32-bit signed integer.</td>
            </tr>
            <tr>
                <td><code>int64</code></td>
                <td>64-bit signed integer.</td>
            </tr>
            <tr>
                <td><code>qint8</code></td>
                <td>Quantized 8-bit signed integer.</td>
            </tr>
            <tr>
                <td><code>quint8</code></td>
                <td>Quantized 8-bit unsigned integer.</td>
            </tr>
            <tr>
                <td><code>qint16</code></td>
                <td>Quantized 16-bit signed integer.</td>
            </tr>
            <tr>
                <td><code>quint16</code></td>
                <td>Quantized 16-bit unsigned integer.</td>
            </tr>
            <tr>
                <td><code>complex64</code></td>
                <td>64-bit single-precision complex.</td>
            </tr>
            <tr>
                <td><code>complex128</code></td>
                <td>128-bit double-precision complex.</td>
            </tr>
            <tr>
                <td rowspan="3"><code>profile</code></td>
                <td rowspan="3">Describes the nature of the data.</td>
                <td><code>text</code></td>
                <td>Monitoring such fields will be done with <b>text</b>-oriented algorithms.</td>
            </tr>
            <tr>
                <td><code>image</code></td>
                <td>Monitoring such fields will be done with <b>image</b>-oriented algorithms.</td>
            </tr>
            <tr>
                <td><code>numerical</code></td>
                <td>Monitoring such fields will be done with <b>numerical</b>-oriented algorithms.</td>
            </tr>
        </tbody>
    </table>
</div>