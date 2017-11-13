package io.hydrosphere.serving.streaming.service

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode

import scala.collection.mutable.ArrayBuffer

trait KafkaDataFormat {

  def toRequestData(input: Seq[String]): Array[Byte] =
    input.mkString("[", ",", "]").getBytes

  def toResponseRecords(json: Array[Byte]): Seq[String] = {
    val jsonFactory = new JsonFactory()
    val parser = jsonFactory.createParser(json)
    val mapper = new ObjectMapper()

    val array = mapper.readTree[ArrayNode](parser)
    val size = array.size()
    val values = new ArrayBuffer[String](size)
    (0 until size).foreach(i => {
      values += mapper.writeValueAsString(array.get(i))
    })
    values
  }
}

object KafkaDataFormat extends KafkaDataFormat
