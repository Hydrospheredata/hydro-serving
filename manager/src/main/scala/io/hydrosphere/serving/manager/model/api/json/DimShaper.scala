package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import spray.json.{JsArray, JsObject, JsValue}

sealed trait ColumnShaper {
  def shape(data: Seq[JsValue]): JsValue
}

case class AnyShaper() extends ColumnShaper {
  override def shape(data: Seq[JsValue]): JsValue = {
    data.headOption.getOrElse(JsObject.empty)
  }
}

case class ScalarShaper() extends ColumnShaper {
  override def shape(data: Seq[JsValue]): JsValue = {
    data.headOption.getOrElse(JsObject.empty)
  }
}

case class DimShaper(dims: Seq[Long]) extends ColumnShaper {
  val strides: Seq[Long] = {
    val res = Array.fill(dims.length)(1L)
    val stLen = dims.length - 1
    for (i <- 0.until(stLen).reverse) {
      res(i) = res(i + 1) * dims(i + 1)
    }
    res.toSeq
  }

  def shape(data: Seq[JsValue]): JsValue = {
    def shapeGrouped(dataId: Int, shapeId: Int): JsValue = {
      if (shapeId >= dims.length) {
        data(dataId)
      } else {
        val n = dims(shapeId).toInt
        val stride = strides(shapeId).toInt
        var mDataId = dataId
        val res = new Array[JsValue](n)

        for (i <- 0.until(n)) {
          val item = shapeGrouped(mDataId, shapeId + 1)
          res(i) = item
          mDataId += stride
        }
        JsArray(res.toVector)
      }
    } // def shapeGrouped

    shapeGrouped(0, 0)
  }
}

object ColumnShaper {
  def apply(tensorShape: TensorShape): ColumnShaper = {
    tensorShape match {
      case AnyDims() => AnyShaper()
      case Dims(dims, _) if dims.isEmpty => ScalarShaper()
      case Dims(dims, _) => DimShaper(dims)
    }
  }
}

/*
 * Converts a subarray of 'self' into lists, with starting data pointer
 * 'dataptr' and from dimension 'startdim' to the last dimension of 'self'.
 *
 * Returns a new reference.

static PyObject *
recursive_tolist(PyArrayObject *self, char *dataptr, int startdim)
{
  npy_intp i, n, stride;
  PyObject *ret, *item;

  /* Base case */
  if (startdim >= PyArray_NDIM(self)) {
  return PyArray_GETITEM(self, dataptr);
}

  n = PyArray_DIM(self, startdim);
  stride = PyArray_STRIDE(self, startdim);

  ret = PyList_New(n);
  if (ret == NULL) {
  return NULL;
}

  for (i = 0; i < n; ++i) {
  item = recursive_tolist(self, dataptr, startdim+1);
  if (item == NULL) {
  Py_DECREF(ret);
  return NULL;
}
  PyList_SET_ITEM(ret, i, item);

  dataptr += stride;
}

  return ret;
}
*/