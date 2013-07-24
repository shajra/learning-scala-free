HTTP Free
=========

I'm building out a small language to make HTTP calls.  I'm using free monads to
build out this language.  Right now, it just got two commands, "call" and
"fork."

This project is a stripped version of something I'm working on in my day job.
I've gotten permission from my employer to post this reduced version.  What
we'd like to get is some code review from experienced Scala developers about
our approach and style.

A small example of how the API is intended to be consumed is in play.scala.
We're a little confused by why the execution doesn't blow stack without further
trampolining of HttpPlan#execute.

You can find me on irc://irc.freenode.net/scala as "tnks."  Or if you're in the
mood, you can submit pull requests of issues to communicate your feedback.

Thanks for helping out,
Sukant
