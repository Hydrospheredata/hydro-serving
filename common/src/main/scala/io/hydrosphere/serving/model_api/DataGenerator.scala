//package io.hydrosphere.serving.model_api
//
//class DataGenerator(val modelApi: ModelApi) {
//  def generate: Any = modelApi match {
//    case DataFrame(definition) =>
//      definition.map(x => x.name -> DataGenerator.generateField(x.fieldType)).toMap
//    case UntypedAPI =>
//      DataGenerator.generateField(FAny)
//  }
//}
//
//object DataGenerator {
//  def apply(modelApi: ModelApi): DataGenerator = new DataGenerator(modelApi)
//
//  def generateField(field: FieldType): Any = field match {
//    case _: FString.type => "foo"
//    case _: FInteger.type => 0
//    case _: FDouble.type  => 0.5
//    case _: FAny.type => "any"
//    case _: FAnyScalar.type => "any_scalar"
//    case FMatrix(item, shape) =>
//      shape.map(_.max(1)).reverse.foldLeft(List.empty[Any]){
//        case (Nil, y) =>
//          1L.to(y).map(_ => generateField(item)).toList
//        case (x, y) =>
//          1L.to(y).map(_ => x).toList
//      }
//  }
//}
