---
layout: docs
title:  "Applications"
permalink: 'applications.html'
---

# Applications 

__Application__ is a publicly available endpoint to reach your models. It allows you to use your most recent deployed production models via HTTP-requests, gRPC API calls or configure it as a part of Kafka streams. 

When configuring applications, you have 2 options:

1. `singular` application. It's an option if you want to use just one of your models. In that case the model probably handles all necessary transformations and data cleaning itself and produces only the desired result. Or maybe you do the cleaning on your side, and you just need to get predictions from the model (although in that case you might consider migrating pre/post-processing operations as __pipeline__ stages). 

2. `pipeline` application. That's an option, if you want to create pipelines that will let data flow through different models, perform pre/post processing operations, etc.

<br>
<hr>

# What's next?

- [Learn, how to invoke applications]({{site.baseurl}}{%link how-to/invoke-applications.md%});
- [Learn, how to write manifests]({{site.baseurl}}{%link how-to/write-manifests.md%});
