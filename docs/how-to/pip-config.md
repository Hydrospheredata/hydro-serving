# Using private pip repositories

To use private pip repository you must add customized `pip.conf` file pointing to your custom PyPI repository.

For example, your custom pip.conf file can look like this:

```text
[global]
timeout = 60
index-url = http://pypi.python.org/simple/
```

If you need to specify the certificate to use during `pip install` you want to specify the path to it in a `pip.conf` file e.g.

```text
[global]
timeout = 60
index-url = http://pypi.python.org/simple/
cert = /model/files/cert.pem
```

You can tell `pip` to use this `pip.conf`file in the `install-command` field inside `serving.yaml`:

```yaml
kind: Model
name: linear_regression
runtime: "hydrosphere/serving-runtime-python-3.7:$released_version$"
install-command: "PIP_CONFIG_FILE=pip.conf pip install -r requirements.txt"
payload:
  - "src/"
  - "requirements.txt"
  - "pip.conf"  # location of your pip.conf
  - "cert.pem"  # location of your certificate. It'll be available under /model/files/cert.pem
  - "model.h5"
contract:
  name: infer
  inputs:
    x:
      shape: [-1, 2]
      type: double
  outputs:
    y:
      shape: [-1]
      type: double
```

