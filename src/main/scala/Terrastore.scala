package com.jteigen.terrastore

import dispatch._
import Http._
import liftjson._
import Js._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

object Terrastore{
  def apply(hostname:String, port:Int) = new Request(:/(hostname, port)) with Terrastore
}

trait Terrastore extends Request {
  val bucket_names = this ># (ary >>~> str)

  abstract override def / (bucket_name:String) = new Request(super./(bucket_name)) with Bucket
}

trait Where extends Request {
  def where(predicate:String) = this / "predicate" <<? Map("predicate" -> predicate)
}

trait TakeWhere extends Where with Take {
  override def where(predicate:String) = new Request(super.where(predicate)) with Take

  override def take(limit:Long) = new Request(super.take(limit)) with Where
}

trait Take extends Request {
  def take(limit:Long) = new Request(this <<? Map("limit" -> limit.toString))
}

trait Bucket extends Request with Take with Where {
  val remove = DELETE >|

  def range(startKey:String, endKey:String = null, comparator:String = null, timeToLive:Long = -1) = {
    val ttl = if(timeToLive == -1) None else Some(timeToLive)
    new Request(this / "range" <<? Map("startKey" -> startKey) ++
      Option(endKey).map("endKey" -> _) ++
      Option(comparator).map("comparator" -> _) ++
      ttl.map("timeToLive" -> _.toString)) with TakeWhere
  }

  override def / (document_key:String) = new Request(super./(document_key)) with Doc

  def export(destination:String, secret:String) =
    POST / "export" <<? Map("destination" -> destination, "secret" -> secret) >|

  def `import`(source:String, secret:String) =
    POST / "import" <<? Map("source" -> source, "secret" -> secret) >|
}

case class WhereBuilder(request:Request) extends Builder[Handler[Unit]]{
  def product = request >|

  def where(predicate:String) = request <<? Map("predicate" -> predicate)
}

trait Doc extends Request {
  private val json = Map("Content-Type" -> "application/json")
  private def as_s(json:JValue) = compact(render(json))

  def <<< (js:JValue):WhereBuilder = WhereBuilder(this <<< as_s(js) <:< json)

  def where(predicate:String) = new Request(this <<? Map("predicate" -> predicate))

  def update(function:String, timeout:Long, parameters:(String, JValue)*):Handler[Unit] =
    update(function, timeout, JObject(parameters.toList.map{ case (key, value) => JField(key, value) }))

  def update(function:String, timeout:Long, parameters:JObject) =
    POST / "update" <<? Map("function" -> function, "timeout" -> timeout.toString) << as_s(parameters) <:< json  >|

  def remove = DELETE >|
}

case class ErrorMessage(message:String, code:Int)


