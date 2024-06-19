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

import cats.effect.kernel.{Resource, _}

trait TaglessFinalFixtures[F[_]]   {
  object ResourceTestLocalFixture {
    def apply[  A](name: String, resource: Resource[F, A])(implicit AsyncF: Async[F]): TaglessFixture[F, A] =
      ResourceFixture.testLocal(name, resource)
  }

  object ResourceSuiteLocalFixture {
    def apply[A](name: String, resource: Resource[F, A])(implicit AsyncF: Async[F]): TaglessFixture[F, A] =
      ResourceFixture.suiteLocal(name, resource)
  }

  @deprecated("Use ResourceSuiteLocalFixture", "2.0.0")
  object UnsafeResourceSuiteLocalDeferredFixture {
    def apply[  A](name: String, resource: Resource[F, A])(implicit AsyncF: Async[F]): TaglessFixture[F, F[A]] =
      ResourceSuiteLocalFixture(name, resource.map(AsyncF.pure(_)))
  }
}