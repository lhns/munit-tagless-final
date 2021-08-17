lazy val scalaVersions = Seq("2.13.6", "2.12.14")

ThisBuild / scalaVersion := scalaVersions.head
ThisBuild / versionScheme := Some("early-semver")

lazy val commonSettings: SettingsDefinition = Def.settings(
  organization := "de.lolhens",
  name := "munit-tagless-final",
  version := "0.0.1-SNAPSHOT",

  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),

  homepage := Some(url("https://github.com/LolHens/munit-tagless-final")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/LolHens/munit-tagless-final"),
      "scm:git@github.com:LolHens/munit-tagless-final.git"
    )
  ),
  developers := List(
    Developer(id = "LolHens", name = "Pierre Kisters", email = "pierrekisters@gmail.com", url = url("https://github.com/LolHens/"))
  ),

  Compile / doc / sources := Seq.empty,

  version := {
    val tagPrefix = "refs/tags/"
    sys.env.get("CI_VERSION").filter(_.startsWith(tagPrefix)).map(_.drop(tagPrefix.length)).getOrElse(version.value)
  },

  publishMavenStyle := true,

  publishTo := sonatypePublishToBundle.value,

  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )).toList
)

name := (root / name).value

lazy val root: Project =
  project
    .in(file("."))
    .settings(commonSettings)
    .settings(
      publishArtifact := false,
      publish / skip := true
    )
    .aggregate(core.projectRefs: _*)

lazy val core = projectMatrix.in(file("core"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % "0.7.28",
      "org.typelevel" %%% "cats-effect-kernel" % "3.2.3",
      "org.typelevel" %%% "cats-effect" % "3.2.3" % Test,
    ),

    testFrameworks += new TestFramework("munit.Framework"),

    Test / scalaJSLinkerConfig := (Test / scalaJSLinkerConfig).value.withModuleKind(ModuleKind.CommonJSModule)
  )
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions)
