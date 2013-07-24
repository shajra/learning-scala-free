HTTP Free
=========

We're building out a small language to make HTTP calls where I work.  We're
using free monads to build out this language.  Right now, it just got two
commands, "call" and "fork."

I've gotten permission from my employer to post this reduced version.  Consider
it public domain (not that I'd recommend you use it as is).

What we'd really like is to get is some code review from experienced Scala
developers about our approach and style.

A small example of how the API is intended to be consumed is in play.scala.
We're a little confused by why the execution doesn't blow stack without further
trampolining of HttpPlan#execute.

You can find me on irc://irc.freenode.net/scala as "tnks."  Or if you're in the
mood, you can submit pull requests of issues to communicate your feedback.

Thanks for helping out,
Sukant
