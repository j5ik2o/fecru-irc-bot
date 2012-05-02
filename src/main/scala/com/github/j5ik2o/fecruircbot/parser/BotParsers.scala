package com.github.j5ik2o.fecruircbot.parser

import util.parsing.combinator.RegexParsers

trait Expr

trait SortOrder extends Expr

object SortOrder {
  def resolve(value: String) = value.toLowerCase match {
    case "asc" => Asc
    case "desc" => Desc
  }
}

case object Asc extends SortOrder

case object Desc extends SortOrder

trait SortType extends Expr

case class CommentSortType(sortOrder: SortOrder = Asc) extends SortType

case class TimeSortType(sortOrder: SortOrder = Asc) extends SortType

trait Opecode extends Expr {
  val operand: Operand
}

trait Operand extends Expr

case class ListOpecode(operand: Operand) extends Opecode

case class ReviewOperand(sortType: SortType) extends Operand

case class BotParseException(msg: String) extends Exception(msg)

class BotParsers extends RegexParsers {

  def parse(source: String) = parseAll(instruction, source) match {
    case Success(result, _) => result
    case Failure(msg, _) => throw new BotParseException(msg)
    case Error(msg, _) => throw new BotParseException(msg)
  }

  private lazy val instruction: Parser[Expr] = listReview

  private lazy val ascOrDesc: Parser[SortOrder] = ("asc" | "desc") ^^ {
    case s => SortOrder.resolve(s)
  }

  // order by comment desc
  private lazy val orderBy: Parser[SortType] = "order" ~> "by" ~> ("comment" | "time") ~ opt(ascOrDesc) ^^ {
    case "comment" ~ aos => new CommentSortType(aos.getOrElse(Asc))
    case "time" ~ aos => new TimeSortType(aos.getOrElse(Asc))
  }

  private lazy val listReview: Parser[Opecode] = "list" ~> "review" ~> opt(orderBy) ^^ {
    case Some(v) => ListOpecode(ReviewOperand(v))
    case None => ListOpecode(ReviewOperand(CommentSortType(Asc)))
  }

}
