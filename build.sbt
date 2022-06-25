val scala213 = "2.13.8"

scalaVersion := scala213

name := "kuzminki-zio-2-fork"
organization := "shevv920"

version := "0.9.3"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

lazy val root = (project in file("."))
  .settings(
    name := "kuzminki-zio-2-fork",
    githubOwner := "shevv920",
    githubRepository := "kuzminki-zio-2",
    githubTokenSource := TokenSource.GitConfig("github.tokenw"),
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.3.6",
      "dev.zio" %% "zio" % "2.0.0",
      "org.scala-lang" % "scala-reflect" % scala213
    ),
  )

