package munit

import cats.effect.{Async, Resource}
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

trait TaglessFinalFunFixtures[F[_]] extends FunFixtures {
  self: TaglessFinalSuite[F] =>

  object ResourceFixture {

    def apply[T](
                  resource: Resource[F, T]
                )(implicit AsyncF: Async[F]): F[FunFixture[T]] =
      apply(
        resource,
        (_, _) => AsyncF.unit,
        _ => AsyncF.unit
      )

    def apply[T](
                  resource: Resource[F, T],
                  setup: (TestOptions, T) => F[Unit],
                  teardown: T => F[Unit]
                )(implicit AsyncF: Async[F]): F[FunFixture[T]] = AsyncF.delay {
      val promise = Promise[F[Unit]]()

      FunFixture.async(
        setup = { testOptions =>
          val resourceEffect = resource.allocated
          val setupEffect =
            resourceEffect
              .map { case (t, release) =>
                promise.success(release)
                t
              }
              .flatTap(t => setup(testOptions, t))

          self.toFuture[T](setupEffect)
        },
        teardown = { (argument: T) =>
          self.toFuture[Unit](
            teardown(argument)
              .flatMap(_ => AsyncF.asyncF[F[Unit]](callback => AsyncF.delay[Unit](promise.future.onComplete(result => callback(result.toEither))(null))).flatten) // TODO
          )
        }
      )
    }

  }

  implicit class FFunFixtureOps[T](private val fixture: F[FunFixture[T]]) {
    def test(name: String)(
      body: T => Any
    )(implicit loc: Location): Unit = {
      Await.result(self.toFuture[FunFixture[T]](fixture), Duration.Inf).test(TestOptions(name))(body)
    }

    def test(options: TestOptions)(
      body: T => Any
    )(implicit loc: Location): Unit = {
      Await.result(self.toFuture[FunFixture[T]](fixture), Duration.Inf).test(options)(body)
    }
  }

}