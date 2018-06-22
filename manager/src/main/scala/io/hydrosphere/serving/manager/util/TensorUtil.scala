package io.hydrosphere.serving.manager.util

import io.hydrosphere.serving.manager.model.{HResult, Result}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor.{TensorProto, TypedTensor, TypedTensorFactory}

object TensorUtil {
  def verifyShape[T](tensor: TensorProto): HResult[TensorProto] = {
    val ttensor = TypedTensorFactory.create(tensor)
    val data = ttensor.data.toArray
    if (ttensor.shape.dims.isEmpty && data.length <= 1) { // allow empty scalar tensors?
      Result.ok(tensor)
    } else {
      val dims = ttensor.shape.dims.get.reverseIterator

      val actualDims = new Array[Long](dims.length)
      var actualDimId = actualDims.length
      var dimDataLen = data.length

      var isShapeOk = true

      // test cases

      // [-1, 2, 3]

      // a b c d e f g h k l - 10
      // 1. 10 % 3 != 0
      // 2. Fail

      // a b c d e f g h k - 9
      // 1. 9 % 3 == 0; 9 / 3 == 3
      // 2. 3 % 2 != 0
      // 3. Fail

      // a b c d e f g - 6
      // 1. 6 % 3 == 0; 6 / 3 == 2
      // 2. 2 % 2 == 0; 2 / 2 == 1
      // 3. 1 % x == 0
      // 4. ok

      // a b c d e f g h k l q w - 12
      // 1. 12 % 3 == 0; 12 / 3 == 4
      // 2. 4 % 2 == 0; 4 / 2 == 2
      // 3. 2 % x == 0
      // 4. ok

      while (isShapeOk && dims.hasNext) {
        val currentDim = dims.next()
        val subCount = dimDataLen.toDouble / currentDim.toDouble
        if (subCount.isWhole()) { // ok
          dimDataLen = subCount.toInt
          actualDims(actualDimId) = dimDataLen
          actualDimId -= 1
        } else { // not ok
          isShapeOk = false
        }
      }

      if (isShapeOk) {
        Result.clientError(s"Invalid shape ${ttensor.shape.dims} for data $data")
      } else {
        val trueShapedTensor = tensor.copy(tensorShape = TensorShape.fromSeq(Some(actualDims)).toProto)
        Result.ok(trueShapedTensor)
      }
    }
  }
}
