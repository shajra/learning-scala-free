package com.github.shajra.http


// IDEA: Consider making this types instead of values to support extension
sealed trait MediaType
case object Xml extends MediaType
case object Json extends MediaType
