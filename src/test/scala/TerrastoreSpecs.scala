package com.jteigen.terrastore

import org.specs.Specification

import dispatch._
import Http._
import liftjson._
import Js._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._
import net.liftweb.json.DefaultFormats

class TerrastoreSpecs extends Specification {
  val http = new Http
  val ts = Terrastore("localhost", 8080)
  val a = ts / "a"
  val b = ts / "b"
  val terrastore = ("db" -> "terrastore") ~ ("version" -> 0.5)
  val cassandra =  ("db" -> "cassandra")  ~ ("version" -> 0.6)

  val doc = a / "doc"

  implicit val formats = DefaultFormats 

  "terrastore" should {

    doAfter {
      http(a.remove)
      http(b.remove)
    }

    "remove bucket" in {
      http(doc <<< terrastore)
      http(doc remove)
      (http when {_ == 404})(doc ># {_.extract[ErrorMessage]}) must_== ErrorMessage("Key not found: doc", 404)
    }

    "get all bucket names" in {
      http(ts bucket_names) must_== Nil

      http(a / "terrastore" <<< terrastore)
      http(b / "cassandra" <<< cassandra)

      http(ts bucket_names) must_== List("b", "a")
    }

    "get all key values from bucket" in {
      http(a / "doc" <<< terrastore)
      http(a / "column" <<< cassandra)

      http(a ># obj) must haveTheSameElementsAs(List(JField("doc", terrastore), JField("column", cassandra)))
    }

    "get all key values from a bucket with limit" in {
      http(a / "doc1" <<< terrastore)
      http(a / "doc2" <<< terrastore)
      http(a / "doc3" <<< terrastore)

      http((a take 2) ># obj).length must_== 2
      http((a take 1) ># obj).length must_== 1
    }

    "add / replace document" in {
      http(doc <<< terrastore)
      http(doc ># {js => js}) must_== terrastore

      http(doc <<< cassandra)
      http(doc ># {js => js}) must_== cassandra
    }

    "remove document" in {
      http(doc <<< terrastore)
      http(doc remove)

      (http when {_ == 404})(doc ># {_.extract[ErrorMessage]}) must_== ErrorMessage("Key not found: doc", 404)
    }

    "get document" in {
      http(doc <<< terrastore)
      http(doc ># {js => js}) must_== terrastore
    }

    "export / import" in {
      http(a / "terrastore" <<< terrastore)
      http(a / "cassandra" <<< cassandra)

      val backup = "backup"
      val secret_key = "SECRET-KEY"

      http(a export (destination=backup, secret=secret_key))
      http(a remove)
      http(a `import` (source=backup, secret=secret_key))

      http(a / "terrastore" ># {js => js}) must_== terrastore
      http(a / "cassandra" ># {js => js}) must_== cassandra
    }

    "update using server side functions" in {
      http(a / "terrastore" <<< terrastore)
      http(a / "terrastore" ># {js => js}) must_== terrastore
      http(a / "terrastore" update("replace", 1000L, "db" -> "voldemort"))
      http(a / "terrastore" ># 'db ? str) must_== List("voldemort")
      http(a / "terrastore" update("replace", 1000L, terrastore))
      http(a / "terrastore" ># {js => js}) must_== terrastore
    }

    "get documents from bucket matching predicate" in {
      http(a / "terrastore" <<< terrastore)
      http(a / "terrastore2" <<< terrastore)
      http(a / "cassandra" <<< cassandra)

      http((a where "jxpath:/db[.='cassandra']") ># obj) must_== List(JField("cassandra", cassandra))
      http((a where "jxpath:/db[.='terrastore']") ># obj) must haveTheSameElementsAs (List(JField("terrastore", terrastore), JField("terrastore2", terrastore)))
    }

    "get documents from bucket in range" in {
      http(a / "1" <<< terrastore)
      http(a / "2" <<< cassandra)
      http(a / "3" <<< terrastore)

      http(a.range(startKey="1", endKey="2", comparator="lexical-asc") ># {js => js}) must_== ("1" -> terrastore)~("2" -> cassandra)
      http(a.range(startKey="3", endKey="2", comparator="lexical-desc") ># {js => js}) must_== ("3" -> terrastore)~("2" -> cassandra)
    }

    "support conditional get using predicates" in {
      http(doc <<< terrastore)
      http((doc where "jxpath:/db[.='terrastore']") ># {js => js}) must_== terrastore
      (http when {_ == 404})((doc where "jxpath:/db[.='cassandra']") ># ('message ? str)) must_== List("Unsatisfied condition: jxpath:/db[.='cassandra'] for key: doc")
    }

    "support conditional put using predicates" in {
      http(doc <<< terrastore)
      http(doc <<< cassandra where "jxpath:/db[.='terrastore']" >|)
      (http when {_ == 409})((doc <<< cassandra where "jxpath:/db[.='terrastore']") ># ('message ? str)) must_== List("Unsatisfied condition: jxpath:/db[.='terrastore'] for key: doc")
    }
  }
}