package munit

import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap._
import cats.syntax.functor._
import munit.internal.FutureCompat._

import scala.util.{Failure, Success}

trait TaglessFinalFunFixtures[F[_]] extends FunFixtures {
  self: TaglessFinalSuite[F] =>

  object ResourceFixture {

    def apply[T](
                  resource: Resource[F, T]
                )(implicit ConcurrentF: Concurrent[F]): F[FunFixture[T]] =
      apply(
        resource,
        (_, _) => ConcurrentF.unit,
        _ => ConcurrentF.unit
      )

    def apply[T](
                  resource: Resource[F, T],
                  setup: (TestOptions, T) => F[Unit],
                  teardown: T => F[Unit]
                )(implicit ConcurrentF: Concurrent[F]): F[FunFixture[T]] = {
      for {
        deferred <- Deferred[F, F[Unit]]
        result <- ConcurrentF.delay {
          FunFixture.async(
            setup = { testOptions =>
              val resourceEffect = resource.allocated
              val setupEffect =
                resourceEffect
                  .flatMap { case (t, release) =>
                    deferred.complete(release).as(t)
                  }
                  .flatTap(t => setup(testOptions, t))

              self.toFuture[T](setupEffect)
            },
            teardown = { (argument: T) =>
              self.toFuture[Unit](
                teardown(argument)
                  .flatMap(_ => deferred.get.flatten)
              )
            }
          )
        }
      } yield
        result
    }

  }

  implicit class FFunFixtureOps[T](private val fixture: F[FunFixture[T]]) {
    def test(name: String)(
      body: T => Any
    )(implicit loc: Location): Unit = {
      test(TestOptions(name))(body)(loc)
    }

    def test(options: TestOptions)(
      body: T => Any
    )(implicit loc: Location): Unit = {
      self.test(options) {
        implicit val ec = munitExecutionContext
        // the setup, test and teardown need to keep the happens-before execution order
        self.toFuture[FunFixture[T]](fixture).flatMap { fixture =>
          fixture.setup(options).flatMap { argument =>
            munitValueTransform(body(argument))
              .transformWithCompat(testValue =>
                fixture.teardown(argument).transformCompat {
                  case Success(_) => testValue
                  case teardownFailure@Failure(teardownException) =>
                    testValue match {
                      case testFailure@Failure(testException) =>
                        testException.addSuppressed(teardownException)
                        testFailure
                      case _ =>
                        teardownFailure
                    }
                }
              )
          }
        }
      }(loc)
    }
  }

}