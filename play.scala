package com.github.shajra.http


import scalaz.{\/, EitherT, Validation}
import scalaz.concurrent.Future
import scalaz.std.string.parseInt


trait CallType
case object OneCallType extends CallType
case object AnotherCallType extends CallType


object CallType {
  val one: CallType = OneCallType
  val another: CallType = AnotherCallType
}


trait Calls {

  val oneCall =
      HttpPlan.call(CallType.one, Endpoint("http://one/"), Get, Json)
        { (i: Int) => Media(Json, i.toString) }
        { case Media(_, s) => parseInt(s).toValidationNel } _

  val anotherCall =
      HttpPlan.call(CallType.another, Endpoint("http://another/"), Get, Json)
        { (i: Int) => Media(Json, i.toString) }
        { case Media(_, s) => parseInt(s).toValidationNel } _

}


object Play extends App with Calls {

  def plan(i: Int) =
    for {
      a <- oneCall(i)
      b <- HttpPlan.fork(anotherCall(a), anotherCall(i)) { _ - _ }
      c <- HttpPlan.fork(anotherCall(b), anotherCall(a)) { _ - _ }
      d <- oneCall(c)
    } yield d

  val strategy: CallStrategy[CallType, NumberFormatException] =
    (cd, pool) => {
      Future(\/ right Media(Json, cd.request.raw + "0"))(pool)
    }

  import scalaz.std.list._
  import scalaz.syntax.traverse._
  val stacking = ((1 to 100000) map plan toList).sequenceU

  // QUESTION: Why doesn't this blow task.  I though the mutual recursion in
  // the "execute" interpretter would cause a problem.
  println((stacking execute strategy run).run)

}
