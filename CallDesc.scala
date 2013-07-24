package com.github.shajra.http


case class CallDesc[C]
    (callType: C,
      endpoint: Endpoint,
      method: Method,
      request: Media,
      response: MediaType)
