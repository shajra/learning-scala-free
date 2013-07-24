package com.github.shajra.http


import scalaz.{\/, EitherT, Validation}
import scalaz.concurrent.Future
import scalaz.std.string.parseInt


trait CallType
case object OneCallType extends CallType
case object AnotherCallType extends CallType


object CallType {
  val dcx: CallType = OneCallType
  val core: CallType = AnotherCallType
}


object Calls {

  val dcx =
      HttpPlan.call(CallType.dcx, Endpoint("http://dcx/"), Get, Json)
        { (i: Int) => Media(Json, i.toString) }
        { case Media(_, s) => parseInt(s).toValidationNel } _

  val core =
      HttpPlan.call(CallType.core, Endpoint("http://core/"), Get, Json)
        { (i: Int) => Media(Json, i.toString) }
        { case Media(_, s) => parseInt(s).toValidationNel } _

}


object Play extends App {

  val plan =
    for {
      a <- Calls dcx 1
      b <- HttpPlan.fork(Calls core 2, Calls core 3) { (a, b) => (a, b) }
      c <- HttpPlan.fork(Calls core 2, Calls core 3) { (a, b) => (a, b) }
      d <- HttpPlan.fork(Calls core 6, Calls core 7) { (a, b) => (a, b) }
      e <- Calls dcx 8
    } yield (a, b, c, d, e)

  val strategy: CallStrategy[CallType, NumberFormatException] =
    (cd, pool) => {
      Future(\/ right Media(Json, cd.request.raw + "0"))(pool)
    }

  import scalaz.std.list._
  import scalaz.syntax.traverse._
  val stacking = ((1 to 300000) map Calls.dcx toList).sequenceU

  // QUESTION: Why doesn't this blow task.  I though the mutual recursion in
  // the "execute" interpretter would cause a problem.
  println((stacking execute strategy run).run)

}
