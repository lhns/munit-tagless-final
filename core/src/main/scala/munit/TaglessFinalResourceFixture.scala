/*
 * Copyright 2021 Typelevel
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

import cats.syntax.flatMap._
import cats.effect.kernel._

object ResourceFixture {

  final class FixtureNotInstantiatedException(name: String)
    extends Exception(
      s"The fixture `$name` was not instantiated. Override `munitFixtures` and include a reference to this fixture."
    )

  def testLocal[F[_], A](name: String, resource: Resource[F, A])(implicit AsyncF: Async[F]): TaglessFixture[F, A] =
    new TaglessFixture[F, A](name) {
      @volatile var value: Option[(A, F[Unit])] = None

      def apply(): A = value match {
        case Some(v) => v._1
        case None    => throw new FixtureNotInstantiatedException(name)
      }

      override def beforeEach(context: BeforeEach): F[Unit] = resource.allocated.flatMap { value =>
        AsyncF.delay(this.value = Some(value))
      }

      override def afterEach(context: AfterEach): F[Unit] = value.fold(AsyncF.unit)(_._2)
    }

  def suiteLocal[F[_], A](name: String, resource: Resource[F, A])(implicit AsyncF: Async[F]): TaglessFixture[F, A] =
    new TaglessFixture[F, A](name) {
      @volatile var value: Option[(A, F[Unit])] = None

      def apply(): A = value match {
        case Some(v) => v._1
        case None    => throw new FixtureNotInstantiatedException(name)
      }

      override def beforeAll(): F[Unit] = resource.allocated.flatMap { value =>
        AsyncF.delay(this.value = Some(value))
      }

      override def afterAll(): F[Unit] = value.fold(AsyncF.unit)(_._2)
    }

}

abstract class TaglessFixture[F[_], A](name: String)(implicit AsyncF: Async[F]) extends AnyFixture[A](name: String) {

  override def beforeAll(): F[Unit] = AsyncF.unit

  override def beforeEach(context: BeforeEach): F[Unit] = AsyncF.unit

  override def afterEach(context: AfterEach): F[Unit] = AsyncF.unit

  override def afterAll(): F[Unit] = AsyncF.unit

}
