package io.hydrosphere.serving.manager.util

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.model.api.TensorUtil
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.Dims
import io.hydrosphere.serving.tensorflow.tensor.DoubleTensor

class TensorUtilSpec extends GenericUnitTest {
  describe("TensorUtilSpec") {
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
    it("should leave correct and static shape as-is") {
      val tensor = DoubleTensor(TensorShape.mat(2, 2, 3), Seq(1,2,3,4,5,6,7,8,9,10,11,12))
      val shapeRes = TensorUtil.verifyShape(tensor)
      assert(shapeRes.isRight, shapeRes)
      val shaped = shapeRes.right.get
      assert(shaped.shape === Dims(Seq(2, 2, 3)))
    }

    it("should fill dynamic dims and return it for correct shape") {
      val tensor = DoubleTensor(TensorShape.mat(-1, 3, 2), Seq(1,2,3,4,5,6))
      val shapeRes = TensorUtil.verifyShape(tensor)
      assert(shapeRes.isRight, shapeRes)
      val shaped = shapeRes.right.get
      assert(shaped.shape === Dims(Seq(1, 3, 2)))
    }

    it("should fail on incorrect shape") {
      val tensor = DoubleTensor(TensorShape.mat(-1, 3, 3), Seq(1,2,3,4,5,6,7,8,9,10))
      val shapeRes = TensorUtil.verifyShape(tensor)
      assert(shapeRes.isLeft, shapeRes)
    }
  }
}
