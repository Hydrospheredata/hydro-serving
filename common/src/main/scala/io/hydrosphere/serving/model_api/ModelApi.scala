package io.hydrosphere.serving.model_api

trait ModelApi {
  def validate(data: Seq[Any]): Boolean
  def generate: Seq[Any]
}

case class DataFrame(definition: List[ModelField]) extends ModelApi {
  override def validate(data: Seq[Any]): Boolean = {
//    println("array is validated the data")
//    val mapCheck = data.forall(_.isInstanceOf[Map[String, Any]])
//    if (mapCheck) {
//      val mapData = data.asInstanceOf[Seq[Map[String, Any]]]
//      definition.forall{ d =>
//        d
//        mapData.map{ row =>
//          row(d)
//        }
//      }
//    } else {
//      false
//    }
    ???
  }

  override def generate: Seq[Map[String, Any]] = List(definition.map(_.generate).toMap)
}


case class DataArray(definition: List[FMatrix]) extends ModelApi {
  override def validate(data: Seq[Any]): Boolean = {
    println("array is validated the data")
    data.forall(row => definition.forall(ddef => ddef.validate(row)))
  }

  override def generate: Seq[Any] = ???
}