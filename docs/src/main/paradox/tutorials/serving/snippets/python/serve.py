import tensorflow as tf
import hydro_serving_grpc as hs  # this package is already present in the runtime

def increment(number):   # <- keep in mind the signature
    request_number = tf.make_ndarray(number)
    response_number = request_number + 1

    response_tensor_shape = [
        hs.TensorShapeProto.Dim(size=dim.size) for dim in number.tensor_shape.dim]
    response_tensor = hs.TensorProto(
        int_val=response_number.flatten(), 
        dtype=hs.DT_INT32,
        tensor_shape=hs.TensorShapeProto(dim=response_tensor_shape)
    )

    return hs.PredictResponse(
        outputs={"number": response_tensor})