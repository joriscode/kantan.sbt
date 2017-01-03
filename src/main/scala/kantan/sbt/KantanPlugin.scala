/*
 * Copyright 2017 Nicolas Rinaudo
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

package kantan.sbt

import com.github.tkawachi.doctest.DoctestPlugin.autoImport._
import com.github.tkawachi.doctest.DoctestPlugin.DoctestTestFramework
import com.typesafe.sbt.SbtGit.git
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.headers
import de.heikoseeberger.sbtheader.license.Apache2_0
import org.scalastyle.sbt.ScalastylePlugin
import sbt._
import sbt.Keys._

/** Settings common to all projects.
  *
  * Among other things, this plugin will set sane scalac options, import whatever is required for the current version
  * of scala to support macros, ...
  *
  * It's usually necessary to enable one of the following plugins:
  *  - [[PublishedPlugin]]    : configures projects whose artifacts are intended for publication on maven central.
  *  - [[UnpublishedPlugin]]  : configures projects whose artifacts are not meant to be published, such as tests and
  *                             documentation.
  *  - [[DocumentationPlugin]]: configures projects whose output is a documentation website.
  */
object KantanPlugin extends AutoPlugin {
  // - Common dependency versions --------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val kindProjectorVersion = "0.9.3"
  val macroParadiseVersion = "2.1.0"



  // - Public settings -------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  object autoImport {
    val kantanProject: SettingKey[String] = settingKey("Name of the kantan project")

    def ifJava8[A](projects: A*): Seq[A] =
      if(supportsJava8) projects else Nil

    def ifNotJava8[A](projects: A*): Seq[A] =
      if(!supportsJava8) projects else Nil

  }
  import autoImport._


  // - Helper functions ------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  lazy val javaSpecificationVersion: Double = System.getProperty("java.specification.version").toDouble
  lazy val supportsJava8: Boolean = javaSpecificationVersion >= 1.8D



  // - AutoPlugin implementation ---------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  override def trigger = allRequirements

  override def requires = HeaderPlugin

  override lazy val projectSettings =
    generalSettings ++ scalacSettings ++ commonDependencies ++ remoteSettings ++
    ScalastylePlugin.projectSettings

  override def globalSettings =
    addCommandAlias("validate", ";clean;scalastyle;test:scalastyle;coverage;test;coverageReport;coverageAggregate;doc")



  // - Custom settings -------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /** Remote identifiers, computed from [[autoImport.kantanProject]]. */
  lazy val remoteSettings: Seq[Setting[_]] = Seq(
    homepage       := Some(url(s"https://nrinaudo.github.io/kantan.${kantanProject.value}")),
    apiURL         := Some(url(s"https://nrinaudo.github.io/kantan.${kantanProject.value}/api/")),
    git.remoteRepo := s"git@github.com:nrinaudo/kantan.${kantanProject.value}.git",
    scmInfo        := Some(ScmInfo(
      url(s"https://github.com/nrinaudo/kantan.${kantanProject.value}"),
      s"scm:git:git@github.com:nrinaudo/kantan.${kantanProject.value}.git"
    ))
  )

  lazy val filter = ScopeFilter(inAnyProject, inAnyConfiguration)

  /** General settings. */
  lazy val generalSettings: Seq[Setting[_]] = {
    val license = Apache2_0("2017", "Nicolas Rinaudo")

    Seq(
      organization            := "com.nrinaudo",
      scalaVersion            := "2.12.1",
      crossScalaVersions      := Seq("2.10.6", "2.11.8", "2.12.1"),
      autoAPIMappings         := true,
      incOptions              := incOptions.value.withNameHashing(true),
      doctestWithDependencies := false,
      doctestTestFramework    := DoctestTestFramework.ScalaTest,
      headers                 := Map(
        "scala" → license,
        "java"  → license
      ),
      resolvers               := Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots")
      )
    )
  }

  /** Sane, version dependent scalac settings. */
  def scalacSettings: Seq[Setting[_]] = Seq(
    scalacOptions := Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture"
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((_, x)) if x > 10 ⇒
        "-Ywarn-unused-import" :: (if(x >= 12) List("-Ypartial-unification") else List.empty)
      case Some((_, 10)) ⇒ Seq("-Xdivergence211")
      case _             ⇒ Nil
    }),

    // Disable -Ywarn-unused-imports in the console.
    scalacOptions in (Compile, console) ~= { _.filterNot(Set("-Ywarn-unused-import")) }
  )

  /** Includes common dependencies (macros and kind-projector). */
  lazy val commonDependencies: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      compilerPlugin("org.spire-math" % "kind-projector" % kindProjectorVersion cross CrossVersion.binary),
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    ) ++ {
      if(scalaVersion.value.startsWith("2.10"))
        List(compilerPlugin("org.scalamacros" % "paradise" % macroParadiseVersion cross CrossVersion.full))
      else Nil
    }
  )
}
