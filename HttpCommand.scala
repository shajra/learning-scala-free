package com.github.shajra.http


import scalaz.Functor


private[http] sealed trait HttpCommand[C, E, +O] {

  def visit[OO]
      (call: (CallDesc[C], HttpUnmarshal[E, O]) => OO)
      (fork: ForkDesc[C, E, O] => OO)
      : OO =
    this match {
      case Fork(forkImpl) =>
        fork(forkImpl)
      case Call(ct, ep, m, req, resType, res) =>
        call(CallDesc(ct, ep, m, req, resType), res)
    }

  def map[OO](f: O => OO): HttpCommand[C, E, OO]

}


private case class Call[C, E, O]
   (callType: C,
      endpoint: Endpoint,
      method: Method,
      request: Media,
      responseType: MediaType,
      response: HttpUnmarshal[E, O])
    extends HttpCommand[C, E, O] {
  def map[OO](f: O => OO) = copy(response = response andThen { _ map f })
}


private case class Fork[C, E, O](impl: ForkDesc[C, E, O])
    extends HttpCommand[C, E, O] {
  def map[OO](g: O => OO) =
    HttpCommand.fork(impl.call1, impl.call2) { (a, b) => g(impl.f(a, b)) }
}


private sealed trait ForkDesc[C, E, +O] { self =>
  type A
  type B
  def call1: HttpPlan[C, E, A]
  def call2: HttpPlan[C, E, B]
  def f(a: A, b: B): O
}


private object HttpCommand {

  def call[C, E, O]
      (callType: C,
        endpoint: Endpoint,
        method: Method,
        request: Media,
        responseType: MediaType,
        responseMarshal: HttpUnmarshal[E, O])
      : HttpCommand[C, E, O] =
    Call(callType, endpoint,method, request, responseType, responseMarshal)

  def fork[C, E, A, B, O]
      (call1: HttpPlan[C, E, A],
        call2: HttpPlan[C, E, B])
      (f: (A, B) => O = Function.untupled(identity[(A, B)] _))
      : HttpCommand[C, E, O] = {
    type AA = A
    type BB = B
    val c1 = call1
    val c2 = call2
    val ff = f
    Fork(new ForkDesc[C, E, O] {
      type A = AA
      type B = BB
      val call1 = c1
      val call2 = c2
      def f(a: AA, b: BB) = ff(a, b)
    })
  }

  implicit def functor[C, E]
      : Functor[({type λ[o] = HttpCommand[C, E, o]})#λ] = {
    type F[o] = HttpCommand[C, E, o]
    new Functor[F] { def map[A, B](plan: F[A])(f: A => B) = plan map f }
  }

}
