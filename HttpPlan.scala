package com.github.shajra.http


import java.util.concurrent.ExecutorService

import scalaz.{-\/, \/-, EitherT, Free, Nondeterminism, Monad, ValidationNel,
  NonEmptyList, Validation}
import scalaz.Free.{Return, Suspend}
import scalaz.concurrent.{Future, Strategy}
import scalaz.syntax.applicative.^


final class HttpPlan[C, E, O] private
    (private val impl: Free[({type λ[+o] = HttpCommand[C, E, o]})#λ, O]) {

  def map[OO](f: O => OO): HttpPlan[C, E, OO] =
    new HttpPlan(Monad[M].map(this.impl)(f))

  def flatMap[OO](f: O => HttpPlan[C, E, OO]): HttpPlan[C, E, OO] =
    new HttpPlan(Monad[M].bind(this.impl)(f andThen { _.impl }))

  // TODO: trampoline away the non-tail recursion
  def execute(strategy: CallStrategy[C, E])
      (implicit pool: ExecutorService = Strategy.DefaultExecutorService)
      : EitherT[Future, NonEmptyList[E], O] = {

    def ifFork[OO](fork: ForkDesc[C, E, M[OO]]) =
      EitherT(
        Nondeterminism[Future]
          .mapBoth
            (EitherT(Future.fork(loop(fork.call1.impl).run)(pool)).validation,
              EitherT(Future.fork(loop(fork.call2.impl).run)(pool)).validation)
            { (a, b) =>
              type V[o] = Validation[NonEmptyList[E], o]
              ^[V, fork.A, fork.B, M[OO]](a, b)(fork.f).disjunction
            }
      ) flatMap loop

    def ifCall[OO](call: CallDesc[C], res: HttpUnmarshal[E, M[OO]]) =
      EitherT(strategy(call, pool))
        .leftMap { NonEmptyList(_) }
        .flatMap(res andThen { v => EitherT(Future.now(v.disjunction)) })
        .flatMap(loop)

    def loop[OO](m: M[OO]): EitherT[Future, NonEmptyList[E], OO] =
      m.resume match {
        case -\/(cmd) => cmd.visit(ifCall)(ifFork)
        case \/-(a) => EitherT right Future.now(a)
      }

    loop(impl)

  }

  private type F[+o] = HttpCommand[C, E, o]
  private type M[o] = Free[F, o]

}


object HttpPlan {

  def call[C, E, I, O]
      (callType: C,
        endpoint: Endpoint,
        method: Method,
        responseType: MediaType)
      (requestMarshal: I => Media)
      (responseMarshal: Media => ValidationNel[E, O])
      (i: I)
      : HttpPlan[C, E, O] =
    HttpPlan(
      HttpCommand.call(
        callType, endpoint,method, requestMarshal(i), responseType,
          responseMarshal))

  def fork[C, E, A, B, O]
      (call1: HttpPlan[C, E, A],
        call2: HttpPlan[C, E, B])
      (f: (A, B) => O)
      : HttpPlan[C, E, O] =
    HttpPlan(HttpCommand.fork(call1, call2)(f))

  implicit def monad[C, E]
      : Monad[({type λ[o] = HttpPlan[C, E, o]})#λ] = {
    type F[+o] = HttpCommand[C, E, o]
    type M[o] = HttpPlan[C, E, o]
    new Monad[M] {
      def bind[A, B](m: M[A])(f: A => M[B]): M[B] = m flatMap f
      def point[A](a: => A): M[A] = new HttpPlan(Return[F, A](a))
    }
  }

  private def apply[C, E, O](p: HttpCommand[C, E, O]): HttpPlan[C, E, O] = {
    type F[+o] = HttpCommand[C, E, o]
    new HttpPlan(Suspend[F, O](p map { Return(_) }))
  }

}
