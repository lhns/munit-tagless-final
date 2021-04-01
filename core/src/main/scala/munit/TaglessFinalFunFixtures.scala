/*
 * Copyright 2021 Typelevel
 * Modifications by Pierre Kisters
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package munit

import cats.effect.kernel._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._

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
                )(implicit AsyncF: Async[F]): F[FunFixture[T]] = {
      for {
        deferred <- Deferred[F, F[Unit]]
        result <- AsyncF.delay {
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
    )(implicit AsyncF: Async[F], loc: Location): Unit = {
      test(TestOptions(name))(body)
    }

    def test(options: TestOptions)(
      body: T => Any
    )(implicit AsyncF: Async[F], loc: Location): Unit = {
      self.test(options) {
        // the setup, test and teardown need to keep the happens-before execution order
        fixture.flatMap { fixture =>
          bracketAddSuppressed {
            AsyncF.fromFuture(AsyncF.delay(fixture.setup(options)))
          } { argument =>
            AsyncF.fromFuture(AsyncF.delay(munitValueTransform(body(argument))))
          } { argument =>
            AsyncF.fromFuture(AsyncF.delay(fixture.teardown(argument)))
          }
        }
      }(loc)
    }

    private def bracketAddSuppressed[F[_], A, B](acquire: F[A])(use: A => F[B])(release: A => F[Unit])
                                                (implicit MonadCancelF: MonadCancel[F, Throwable]): F[B] =
      MonadCancelF.bracketCase(acquire)(use) { (a, outcome) =>
        release(a).handleErrorWith { releaseException =>
          outcome.fold(
            MonadCancelF.pure(()),
            { useException =>
              useException.addSuppressed(releaseException)
              MonadCancelF.raiseError(useException)
            },
            _ => MonadCancelF.raiseError[Unit](releaseException)
          )
        }
      }
  }

}