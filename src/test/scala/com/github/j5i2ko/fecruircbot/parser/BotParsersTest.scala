package com.github.j5i2ko.fecruircbot.parser

import org.scalatest.FunSuite
import com.github.j5ik2o.fecruircbot.parser.BotParsers

class BotParsersTest extends FunSuite {
  test("list review"){
    val b = new BotParsers
    println(b.parse("list review"))
  }
  test("list review order(comment)"){
    val b = new BotParsers
    println(b.parse("list review order by comment asc"))
  }
}
