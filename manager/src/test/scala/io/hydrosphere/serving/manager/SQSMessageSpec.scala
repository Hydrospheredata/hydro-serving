package io.hydrosphere.serving.manager

import java.time.{Instant, LocalDateTime, ZoneId}

import io.hydrosphere.serving.manager.actor.modelsource.S3SourceWatcher.SQSMessage
import org.scalatest.WordSpec

class SQSMessageSpec extends WordSpec {
  val be = afterWord("be")

  "SQSMessage" when {
    "correct S3 event message" should be {
      "defined" in {
        val time = Instant.now()
        val inMsg =
          s"""
            |{
            | "Records": [
            |  {
            |   "eventName": "TEST_EVENT",
            |   "eventTime": "${time.toString}",
            |   "s3": {
            |     "bucket": {
            |       "name" : "test_bucket"
            |     },
            |     "object": {
            |       "key": "test_object"
            |     }
            |   }
            |  }
            | ]
            |}
            |
          """.stripMargin
        val sqsMsg = SQSMessage.fromJson(inMsg)
        assert(sqsMsg.isDefined)
        val msg = sqsMsg.get
        assert(msg.bucket === "test_bucket")
        assert(msg.objKey === "test_object")
        assert(msg.eventTime === LocalDateTime.ofInstant(time, ZoneId.systemDefault()))
        assert(msg.eventName === "TEST_EVENT")
      }
    }
    "other event message" should be {
      "None" in {
        val inMsg =
          s"""
             |{
             | "ServiceMessage": [
             |  {
             |   "eventName": "TEST_EVENT",
             |   "s3": {
             |     "bucket": {
             |       "name" : "test_bucket"
             |     },
             |     "object": {
             |       "key": "test_object"
             |     }
             |   }
             |  }
             | ]
             |}
             |
          """.stripMargin
        val sqsMsg = SQSMessage.fromJson(inMsg)
        assert(sqsMsg.isEmpty)
      }
    }
  }
}
