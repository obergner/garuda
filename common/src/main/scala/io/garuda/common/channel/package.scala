package io.garuda.common

import io.netty.util.concurrent.{Future, GenericFutureListener}

import scala.language.implicitConversions

/**
 * Created by obergner on 13.10.14.
 */
package object channel {

  implicit def toGenericFutureListener[T, U](f: Future[T] => U): GenericFutureListener[Future[T]] =
    new GenericFutureListener[Future[T]] {
      def operationComplete(future: Future[T]): Unit = f(future)
    }
}
