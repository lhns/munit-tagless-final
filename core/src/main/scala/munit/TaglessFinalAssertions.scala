package munit

import cats.effect.kernel.{Async, Sync}
import cats.syntax.applicativeError._
import cats.syntax.eq._
import cats.syntax.flatMap._

import scala.reflect.ClassTag
import scala.util.control.NonFatal

trait TaglessFinalAssertions[F[_]] {
  self: Assertions =>

  /** Asserts that an `F` returns an expected value.
    *
    * The "returns" value (second argument) must have the same type or be a
    * subtype of the one "contained" inside the `F` (first argument). For example:
    * {{{
    *   assertF(F(Option(1)), returns = Some(1)) // OK
    *   assertF(F(Some(1)), returns = Option(1)) // Error: Option[Int] is not a subtype of Some[Int]
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the
    * assertion fails.
    *
    * @param obtained the F under testing
    * @param returns  the expected value
    * @param clue     a value that will be printed in case the assertions fails
    */
  def assertF[A, B](
                     obtained: F[A],
                     returns: B,
                     clue: => Any = "values are not the same"
                   )(implicit SyncF: Sync[F], loc: Location, ev: B <:< A): F[Unit] =
    obtained.flatMap(a => SyncF.delay(assertEquals(a, returns, clue)))

  /** Asserts that an `F[Boolean]` returns true.
    *
    * For example:
    * {{{
    *   assertFBoolean(F(true))
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the
    * assertion fails.
    *
    * @param obtained the F[Boolean] under testing
    * @param clue     a value that will be printed in case the assertions fails
    */
  protected def assertFBoolean(
                                obtained: F[Boolean],
                                clue: => Any = "values are not the same"
                              )(implicit SyncF: Sync[F], loc: Location): F[Unit] =
    assertF(obtained, true, clue)

  /** Intercepts a `Throwable` being thrown inside the provided `F`.
    *
    * @example
    * {{{
    *   val io = F.raiseError[Unit](MyException("BOOM!"))
    *
    *   interceptF[MyException](io)
    * }}}
    *
    * or
    *
    * {{{
    *   interceptF[MyException] {
    *       F.raiseError[Unit](MyException("BOOM!"))
    *   }
    * }}}
    */
  def interceptF[T <: Throwable](f: F[_])(implicit AsyncF: Async[F], T: ClassTag[T], loc: Location): F[T] = {
    f.attempt.flatMap[T](runInterceptF[F, T](None))
  }

  /** Intercepts a `Throwable` with a certain message being thrown inside the provided `F`.
    *
    * @example
    * {{{
    *   val io = F.raiseError[Unit](MyException("BOOM!"))
    *
    *   interceptF[MyException]("BOOM!")(io)
    * }}}
    *
    * or
    *
    * {{{
    *   interceptF[MyException] {
    *       F.raiseError[Unit](MyException("BOOM!"))
    *   }
    * }}}
    */
  def interceptMessageF[T <: Throwable](
                                         expectedExceptionMessage: String
                                       )(f: F[_])(implicit AsyncF: Async[F], T: ClassTag[T], loc: Location): F[T] =
    f.attempt.flatMap[T](runInterceptF[F, T](Some(expectedExceptionMessage)))

  /** Copied from `munit.Assertions` and adapted to return `F[T]` instead of `T`.
    */
  private def runInterceptF[F[_] : Sync, T <: Throwable](
                                                          expectedExceptionMessage: Option[String]
                                                        )(implicit T: ClassTag[T], loc: Location): Either[Throwable, Any] => F[T] = {
    case Right(value) =>
      Sync[F].delay {
        fail(
          s"expected exception of type '${T.runtimeClass.getName}' but body evaluated successfully",
          clues(value)
        )
      }
    case Left(e: FailException) if !T.runtimeClass.isAssignableFrom(e.getClass) =>
      Sync[F].raiseError[T](e)
    case Left(NonFatal(e: T)) if expectedExceptionMessage.forall(_ === e.getMessage) =>
      Sync[F].pure(e)
    case Left(NonFatal(e: T)) =>
      Sync[F].raiseError[T] {
        val obtained = e.getClass.getName

        new FailException(
          s"intercept failed, exception '$obtained' had message '${e.getMessage}', " +
            s"which was different from expected message '${expectedExceptionMessage.get}'",
          cause = e,
          isStackTracesEnabled = false,
          location = loc
        )
      }
    case Left(NonFatal(e)) =>
      Sync[F].raiseError[T] {
        val obtained = e.getClass.getName
        val expected = T.runtimeClass.getName

        new FailException(
          s"intercept failed, exception '$obtained' is not a subtype of '$expected",
          cause = e,
          isStackTracesEnabled = false,
          location = loc
        )
      }
  }

  implicit class MUnitAssertionsForFOps[A](f: F[A]) {

    /** Asserts that this effect returns an expected value.
      *
      * The "expected" value (second argument) must have the same type or be a
      * subtype of the one "contained" inside the effect. For example:
      * {{{
      *   F(Option(1)).assertEquals(Some(1)) // OK
      *   F(Some(1)).assertEquals(Option(1)) // Error: Option[Int] is not a subtype of Some[Int]
      * }}}
      *
      * The "clue" value can be used to give extra information about the failure in case the
      * assertion fails.
      *
      * @param expected the expected value
      * @param clue     a value that will be printed in case the assertions fails
      */
    def assertEquals[B](
                         expected: B,
                         clue: => Any = "values are not the same"
                       )(implicit SyncF: Sync[F], loc: Location, ev: B <:< A): F[Unit] =
      assertF(f, expected, clue)

    /** Intercepts a `Throwable` being thrown inside this effect.
      *
      * @example
      * {{{
      *   val io = F.raiseError[Unit](MyException("BOOM!"))
      *
      *   io.intercept[MyException]
      * }}}
      */
    def intercept[T <: Throwable](implicit AsyncF: Async[F], T: ClassTag[T], loc: Location): F[T] =
      interceptF[T](f)

    /** Intercepts a `Throwable` with a certain message being thrown inside this effect.
      *
      * @example
      * {{{
      *   val io = F.raiseError[Unit](MyException("BOOM!"))
      *
      *   io.intercept[MyException]("BOOM!")
      * }}}
      */
    def interceptMessage[T <: Throwable](
                                          expectedExceptionMessage: String
                                        )(implicit AsyncF: Async[F], T: ClassTag[T], loc: Location): F[T] =
      interceptMessageF[T](expectedExceptionMessage)(f)

  }

  implicit class MUnitAssertionsForFBooleanOps(f: F[Boolean]) {

    /** Asserts that this effect returns an expected value.
      *
      * For example:
      * {{{
      *   F(true).assert // OK
      * }}}
      */
    def assert(implicit SyncF: Sync[F], loc: Location): F[Unit] =
      assertFBoolean(f, "value is not true")
  }

}
