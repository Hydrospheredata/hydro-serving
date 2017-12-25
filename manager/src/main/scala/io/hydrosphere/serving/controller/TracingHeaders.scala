package io.hydrosphere.serving.controller

/**
  *
  */
object TracingHeaders {
  val xOtSpanContext = "x-ot-span-context"
  val xRequestId = "x-request-id"
  val xB3TraceId = "x-b3-traceid"
  val xB3SpanId = "x-b3-spanid"
  val xB3ParentSpanId = "x-b3-parentspanid"
  val xB3Sampled = "x-b3-sampled"
  val xB3Flags = "x-b3-flags"

  val allHeaders = Seq(xOtSpanContext, xRequestId, xB3TraceId,
    xB3SpanId, xB3ParentSpanId, xB3Sampled, xB3Flags)

  def isTracingHeaderName(header: String): Boolean = header.toLowerCase match {
    case `xOtSpanContext` | `xRequestId` | `xB3TraceId`
         | `xB3SpanId` | `xB3ParentSpanId` | `xB3Sampled` | `xB3Flags` => true
    case _ => false
  }
}