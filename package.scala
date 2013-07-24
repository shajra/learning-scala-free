package com.github.shajra


import java.util.concurrent.ExecutorService

import scalaz.{\/, ValidationNel}
import scalaz.concurrent.Future


package object http {

  type CallStrategy[C, E] =
    (CallDesc[C], ExecutorService) => Future[E \/ Media]

  type HttpUnmarshal[E, +O] = Media => ValidationNel[E, O]

}
