package example

import terrastore._
import Terrastore._

trait Example {
  val bucket_name = "bucket_name"
  val document_key = "document_key"
  val max_elements = 10
  val timeout_value = 1000
  val snapshot_age = 1000
  val comparator_name = "comparator_name"
  val start_key = "start_key"
  val end_key = "end_key"
  val function_name = "function_name"
  val secret_key = "secret_key"
  val file_name = "file_name"
  val document = new Doc{}

  val terrastore = new Terrastore("localhost", 8080)

  // *** BUCKET MANAGEMENT ***

  /*
  Remove bucket

  DELETE /bucket_name
   */
  terrastore( DELETE /bucket_name )

  /*
  Get all bucket names

  GET /
   */
  terrastore( GET / )

  /*
  Get all key/values from bucket

  GET /bucket_name?limit=max_elements
   */
  terrastore( GET /bucket_name )
  terrastore( GET /bucket_name?(limit=max_elements) )


  // *** DOCUMENT MANAGEMENT ***

  /*
  Add/Replace document

  PUT /bucket_name/document_key
   */
  terrastore( PUT /bucket_name/document_key <<< document )

  /*
  Remove document

  DELETE /bucket_name/document_key
   */
  terrastore( DELETE /bucket_name/document_key )

  /*
  Get document

  GET /bucket_name/document_key
   */
  terrastore( GET /bucket_name/document_key )


  // *** BACKUP MANAGEMENT ***

  /*
  Export

  POST /bucket_name/export/?destination=file_name&secret=secret_key
   */
  terrastore( POST /bucket_name/export?(destination=file_name)&(secret=secret_key) )
  // (order of query params should not matter)
  terrastore( POST /bucket_name/export?(secret=secret_key)&(destination=file_name) )

  /*
  Import

  POST /bucket_name/import/?source=file_name&secret=secret_key
   */
  terrastore( POST /bucket_name/`import`?(source=file_name)&(secret=secret_key) )
  // (order of query params should not matter)
  terrastore( POST /bucket_name/`import`?(secret=secret_key)&(source=file_name) )

  // *** FEATURES ***

  /*
  Server-side updates

  POST /bucket_name/document_key/update?function=function_name&timeout=timeout_value
   */
  terrastore( POST /bucket_name/document_key/update?(function=function_name)&(timeout=timeout_value) ^ Map.empty )
  // (order of query params should not matter)
  terrastore( POST /bucket_name/document_key/update?(timeout=timeout_value)&(function=function_name) ^ Map.empty )

  /*
  Predicate queries

  GET /bucket_name/predicate?predicate=type:expression
   */
  terrastore( GET /bucket_name/predicate?(predicate="type:expression") )

  /*
  Range queries

  GET /bucket_name/range?comparator=comparator_name&startKey=start_key&endKey=end_key&timeToLive=snapshot_age
   */
  terrastore( GET /bucket_name/range?(startKey=start_key)&(endKey=end_key)&(comparator=comparator_name)&(timeToLive=snapshot_age) )

  /*
  Predicate queries and Range queries can be combined

  GET /bucket_name/range?comparator=comparator_name&startKey=start_key&endKey=end_key&limit=max_elements&timeToLive=snapshot_age
   */
  terrastore( GET /bucket_name/range?(startKey=start_key)&(endKey=end_key)&(comparator=comparator_name)&(limit=max_elements)&(timeToLive=snapshot_age) )

  //only startKey is required
  terrastore( GET /bucket_name/range?(startKey=start_key) )

  /*
  Conditional Put

  PUT /bucket_name/document_key?predicate=type:expression
   */
  terrastore( PUT /bucket_name/document_key?(predicate="type:expression") <<< document )

  /*
  Conditional Get

  GET /bucket_name/document_key?predicate=type:expression
   */
  terrastore( GET /bucket_name/document_key?(predicate="type:expression") )
}