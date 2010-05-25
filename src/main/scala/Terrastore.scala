package terrastore

import dispatch._
import Http._
import liftjson._
import Js._
import org.apache.http.client.methods._

object Terrastore {
  implicit def _queryable(value:String) = new Queryable(value:String)

  implicit def _limit(value:Int) = new Limit(value)

  implicit def _secret(value:String) = new Secret(value)
  implicit def _destination(value:String) = new Destination(value)
  implicit def _source(value:String) = new Source(value)

  implicit def _function(value:String) = new UpdateFunction(value)
  implicit def _timeout(value:Int) = new Timeout(value)

  implicit def _comparator(value:String) = new Comparator(value)
  implicit def _predicate(value:String) = new Predicate(value)
  implicit def _startKey(value:String) = new StartKey(value)
  implicit def _endKey(value:String) = new EndKey(value)
  implicit def _timeToLive(value:Int) = new TimeToLive(value)

  def export = new EmptyExport
  def `import` = new EmptyImport
  def update = new EmptyUpdate
  def range = new EmptyRangeQuery
  def predicate = new EmptyPredicateQuery
}

class Terrastore(request:Request){
  def this(host:String, port:Int) = this(:/(host, port))

  def apply(complete:CompleteUnit){
    Http( complete( request ) >| )
  }

  def apply(complete:CompleteList) = {
    Http( complete( request ) ># { js => js } ) //jvList combinators ?
  }

  def apply(complete:CompleteOption) = {
    Http( complete( request ) ># { js => js } )
  }
}

sealed trait Complete extends (Request => Request)
trait CompleteList extends Complete
trait CompleteUnit extends Complete
trait CompleteOption extends Complete
/*
Complete
 */

class PutDocument(bucket:String, document_key:String, predicate:Option[Predicate], document:Doc) extends CompleteUnit {
  def apply(request:Request) = PUT(request) / bucket / document_key <<? Map(predicate.toList :_*) <<< document
}

class DeleteBucket(bucket:String) extends CompleteUnit {
  def / (document_key:String) = new DeleteDocument(bucket, document_key)

  def apply(request:Request) = DELETE(request) / bucket
}

class DeleteDocument(bucket:String, document_key:String) extends CompleteUnit {
  def apply(request:Request) = DELETE(request) / bucket / document_key
}

class GetBucket(bucket:String) extends CompleteList {
  def / (document_key:String) = new GetDocument(bucket, document_key)
  def / (predicate:DocumentWithPredicate) = new GetDocument(bucket, predicate.document_key, predicate.predicate)
  def / (predicate:PredicateQueryParam) = new GetByPredicate(bucket, predicate.predicate)

  def /(range: RangeQueryParams) = new GetByRange(bucket,
    range.startKey,
    range.endKey,
    range.comparator,
    range.timeToLive,
    range.limit,
    range.predicate)

  def apply(request:Request) = GET(request) / bucket
}
class GetByPredicate(bucket:String, predicate:Predicate) extends CompleteList {
  def apply(request:Request) = GET(request) / bucket / "predicate" <<? Map(predicate)
}

class GetByRange(bucket:String, parameters:RangeQueryParams) extends CompleteList {

  def this(bucket:String,
           startKey:StartKey,
           endKey:Option[EndKey] = None,
           comparator:Option[Comparator] = None,
           timeToLive:Option[TimeToLive] = None,
           limit:Option[Limit] = None,
           predicate:Option[Predicate] = None) =
    this(bucket, new RangeQueryParams(startKey, endKey, comparator, timeToLive, limit, predicate))

  def & (startKey:StartKey) = new GetByRange(bucket, parameters & (startKey=startKey))
  def & (endKey:EndKey) = new GetByRange(bucket, parameters & (endKey=endKey))
  def & (comparator:Comparator) = new GetByRange(bucket, parameters & (comparator=comparator))
  def & (timeToLive:TimeToLive) = new GetByRange(bucket, parameters & (timeToLive=timeToLive))
  def & (limit:Limit) = new GetByRange(bucket, parameters & (limit=limit))
  def & (predicate:Predicate) = new GetByRange(bucket, parameters & (predicate=predicate))

  private def queryParameters = Map(parameters.startKey) ++ parameters.endKey ++ parameters.comparator ++ parameters.timeToLive ++ parameters.limit ++ parameters.predicate 

  def apply(request:Request) = GET(request) / bucket <<? queryParameters
}

class GetDocument(bucket:String, document_key:String, predicate:Option[Predicate]) extends CompleteOption {
  def this(bucket:String, document_key:String) = this(bucket, document_key, None)
  def this(bucket:String, document_key:String, predicate:Predicate) = this(bucket, document_key, Some(predicate))

  def apply(request:Request) = GET(request) / bucket / document_key <<? Map(predicate.toList :_*)
}

class GetBucketWithLimit(bucket:String, limit:Limit) extends CompleteList {
  def apply(request:Request) = GET(request) / bucket <<? Map(limit)
}

class PostUpdateFunction(bucket:String, document_key:String, function:UpdateFunction, timeout:Timeout, values:Map[String, Any]) extends CompleteUnit {
  def this(bucket:String, document_key:String, function:UpdateFunction, timeout:Timeout) =
    this(bucket, document_key, function, timeout, Map.empty)
  /* ^ was chosen because it has lower precedence than & */
  def ^ (parameters:Map[String, Any]) = new PostUpdateFunction(bucket, document_key, function, timeout, values ++ parameters)
  def apply(request:Request) = POST(request) / bucket / document_key <<? Map(function, timeout) << values
}

class Export(bucket:String, destination:Destination, secret:Secret) extends CompleteUnit {
  def apply(request:Request) = POST(request) / bucket / "export" <<? Map(destination, secret)
}
class Import(bucket:String, source:Source, secret:Secret) extends CompleteUnit {
  def apply(request:Request) = POST(request) / bucket / "import" <<? Map(source, secret)
}

sealed trait Method {
  protected def method:HttpRequestBase
  def apply(request:Request) = request.next{ Request.mimic( method )_ } <:< Map("Content-Type" -> "application/json")
}

object PUT extends Method {
  def / (bucket:String) = new PutBucket(bucket)
  override def method = new HttpPut
}

object DELETE extends Method {
  def / (bucket:String) = new DeleteBucket(bucket)
  override def method = new HttpDelete
}

object GET extends Method {
  object / extends CompleteList {
    def apply(request:Request) = GET(request)
  }
  def / (bucket:String) = new GetBucket(bucket)
  def / (bucket_with_limit:BucketWithLimit) = new GetBucketWithLimit(bucket_with_limit.bucket, bucket_with_limit.limit)
  override def method = new HttpGet
}

object POST extends Method {
  def / (bucket:String) = new PostBucket(bucket)
  override def method = new HttpPost
}



class QueryParam(key:String, value:String) extends Tuple2(key, value)


class PutBucket(bucket:String) {
  def / (document_key:String) = new PutMissingDocument(bucket, document_key, None)
  def / (predicate:DocumentWithPredicate) = new PutMissingDocument(bucket, predicate.document_key, Some(predicate.predicate))
}


class DocumentWithPredicate(val document_key:String, val predicate:Predicate)


class EmptyRangeQuery {
  def ? (startKey:StartKey) = new RangeQueryParams(startKey, None, None, None, None, None)
}
class RangeQueryParams(
        val startKey:StartKey,
        val endKey:Option[EndKey],
        val comparator:Option[Comparator],
        val timeToLive:Option[TimeToLive],
        val limit:Option[Limit],
        val predicate:Option[Predicate]) {

  private def copy(startKey:StartKey = this.startKey,
          endKey:Option[EndKey] = this.endKey,
          comparator:Option[Comparator] = this.comparator,
          timeToLive:Option[TimeToLive] = this.timeToLive,
          limit:Option[Limit] = this.limit,
          predicate:Option[Predicate] = this.predicate) =
    new RangeQueryParams(startKey, endKey, comparator, timeToLive, limit, predicate)

  def & (startKey:StartKey) = copy(startKey = startKey)
  def & (endKey:EndKey) = copy(endKey = Some(endKey))
  def & (comparator:Comparator) = copy(comparator = Some(comparator))
  def & (timeToLive:TimeToLive) = copy(timeToLive = Some(timeToLive))
  def & (limit:Limit) = copy(limit = Some(limit))
  def & (predicate:Predicate) = copy(predicate = Some(predicate))
}

class PredicateQueryParam(val predicate:Predicate)

class EmptyPredicateQuery {
  def ? (predicate:Predicate) = new PredicateQueryParam(predicate)
}


class Queryable(value:String) {
  def ? (limit:Limit) = new BucketWithLimit(value, limit)
  def ? (predicate:Predicate) = new DocumentWithPredicate(value, predicate)
}

class Predicate(value:String) extends QueryParam("predicate", value)
class BucketWithLimit(val bucket:String, val limit:Limit)
class Limit(value:Int) extends QueryParam("limit", value.toString)
class TimeToLive(value:Int) extends QueryParam("timeToLive", value.toString)
class Comparator(value:String) extends QueryParam("comparator", value)
class StartKey(value:String) extends QueryParam("startKey", value)
class EndKey(value:String) extends QueryParam("endKey", value)



class PostBucket(bucket:String) {
  def / (export:ExportDestination) = new PostBucketExportDestination(bucket, export.destination)
  def / (export:ExportSecret) = new PostBucketExportSecret(bucket, export.secret)
  def / (`import`:ImportSource) = new PostBucketImportSource(bucket, `import`.source)
  def / (`import`:ImportSecret) = new PostBucketImportSecret(bucket, `import`.secret)
  def / (document_key:String) = new PostDocument(bucket, document_key)
}

class PostBucketExportDestination(bucket:String, destination:Destination){
  def & (secret:Secret) = new Export(bucket, destination, secret)
}
class PostBucketExportSecret(bucket:String, secret:Secret) {
  def & (destination:Destination) = new Export(bucket, destination, secret)
}
class PostBucketImportSource(bucket:String, source:Source) {
  def & (secret:Secret) = new Import(bucket, source, secret)
}
class PostBucketImportSecret(bucket:String, secret:Secret) {
  def & (source:Source) = new Import(bucket, source, secret)
}
class EmptyExport {
  def ? (destination:Destination) = new ExportDestination(destination)
  def ? (secret:Secret) = new ExportSecret(secret)
}
class ExportDestination(val destination:Destination)
class ExportSecret(val secret:Secret)

class Secret(value:String) extends QueryParam("secret", value)
class Destination(value:String) extends QueryParam("destination", value)
class Source(value:String) extends QueryParam("source", value)



class EmptyImport {
  def ? (source:Source) = new ImportSource(source)
  def ? (secret:Secret) = new ImportSecret(secret)
}
class ImportSource(val source:Source)
class ImportSecret(val secret:Secret)

class PostDocument(bucket:String, document_key:String){
  def / (function:UpdateFunctionOnly) = new PostDocumentUpdateFunction(bucket, document_key, function.function)
  def / (timeout:TimeoutOnly) = new PostDocumentUpdateTimeout(bucket, document_key, timeout.timeout)
}

class EmptyUpdate {
  def ? (function:UpdateFunction) = new UpdateFunctionOnly(function)
  def ? (timeout:Timeout) = new TimeoutOnly(timeout)
}

class UpdateFunctionOnly(val function:UpdateFunction)
class TimeoutOnly(val timeout:Timeout)

class UpdateFunction(value:String) extends QueryParam("function", value)
class Timeout(value:Int) extends QueryParam("timeout", value.toString)


class PostDocumentUpdateFunction(bucket:String, document_key:String, function:UpdateFunction) {
  def & (timeout:TimeoutWithValues) = new PostUpdateFunction(bucket:String, document_key:String, function, timeout.timeout)
  def & (timeout:Timeout) = new PostUpdateFunction(bucket:String, document_key:String, function, timeout)
}

class TimeoutWithValues(val timeout:Timeout, values:Map[String, Any])

class PostDocumentUpdateTimeout(bucket:String, document_key:String, timeout:Timeout) {
  def & (function:UpdateFunction) = new PostUpdateFunction(bucket, document_key, function, timeout)
}

class PutMissingDocument(bucket:String, document_key:String, predicate:Option[Predicate]) {
  def <<< (document:Doc) = new PutDocument(bucket:String, document_key:String, predicate:Option[Predicate], document:Doc)
}

trait Doc
