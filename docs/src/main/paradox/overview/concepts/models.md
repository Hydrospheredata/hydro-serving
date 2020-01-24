# Models 

__Model__ is a machine learning model or a processing function that consumes 
provided inputs and produces predictions/transformations. Each model is a 
collection of its own versions. Every time you upload or re-upload a model, 
the newer version is getting created and added to that collection. At the lowest 
level a model version is represented as a Docker image created based on the 
model binaries. This essentially means, that during building stage the model 
version gets frozen and can no longer change. Each collection is identified 
by the model's name. 

When you upload a model to Hydrosphere, roughly the following steps are 
executed:

1. [CLI](../overview/components/cli.html) uploads model binaries to the platform;
1. [Manager](../overview/components/index.html#manager-service) builds a new Docker 
image based on the uploaded binaries and saves the image in the configured 
[Docker registry](../overview/components/index.html#docker-registry);
1. A built image is assigned with the model's collection with an increased 
version.

## Frameworks

Model can be written using a variety of modern machine learning frameworks. 
You can implement your model using TensorFlow graph computations or create 
your model with scikit-learn, PyTorch, Keras, Fastai, MXNet, Spark ML/MLlib, 
etc. Hydrosphere can understand your models depending on what framework you are 
using. It’s possible due to the metadata that the frameworks save with the model, 
but it’s not always the case. You should refer to the table below with listed 
frameworks and their inference. 

<div class="flexible-table">
	<table>
		<thead>
			<tr>
				<th>Framework</th>
				<th>Status</th>
				<th>Inferring</th>
				<th>Commentaries</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td>TensorFlow</td>
				<td><code>maintained</code></td>
				<td>100%</td>
				<td>TensorFlow saves all needed metadata with <code>SavedModelBuilder</code>, so generated contracts will be very accurate.</td>
			</tr>
			<tr>
				<td>Spark</td>
				<td><code>partly</code></td>
				<td>50%</td>
				<td>Spark has metadata, but it's insufficient and contract's inference may be inaccurate. To give an example:<br>1) there isn't enough notation on how shape of the model is formed (i.e. [30, 40] might be a matrix 30x40 or 40x30);<br>2) types are not always coincide with what Serving knows, etc.</td>
			</tr>
			<tr>
				<td>MXNet</td>
				<td><code>manual</code></td>
				<td>0%</td>
				<td>MXNet has its own export mechanism, but it does not contain any metadata related to types and shapes. Serve the model as a Python model.</td>
			</tr>
			<tr>
				<td>SkLearn</td>
				<td><code>manual</code></td>
				<td>0%</td>
				<td>Exported models does not provide required metadata. Serve the model as a Python model.</td>
			</tr>
			<tr>
				<td>Theano</td>
				<td><code>manual</code></td>
				<td>0%</td>
				<td>Exported models does not provide required metadata. Serve the model as a Python model.</td>
			</tr>
			<tr>
				<td>ONNX</td>
				<td><code>manual</code></td>
				<td>80%</td>
				<td>Currently Serving is able to read ONNX's proto files, but due to the lack of support from other frameworks (PyTorch, TensorFlow, etc.) ONNX models cannot be run in the implemented runtimes.</td>
			</tr>
		</tbody>
	</table>
</div>

<p style="font-size:0.8em; margin-top: 10px; margin-left: 8px;">
	<code>maintained</code> - No need to provide any self-written contracts, model definitions;<br>
	<code>partly</code> - Complicated models will likely fail inference;<br>
	<code>manual</code> - Need to provide self-written contracts, model definitions.<br>
</p>

<hr>

If inferring percentage is high, you can omit providing model definitions, 
otherwise you should. Learn more about writing custom model definitions 
[here](../how-to/write-resource-definitions.html#model).
