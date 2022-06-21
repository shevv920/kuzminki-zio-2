val scala213 = "2.13.8"

scalaVersion := scala213

name := "kuzminki-zio-2-fork"

version := "0.9.2"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

lazy val root = (project in file("."))
  .settings(
    name := "kuzminki-zio-2-fork",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.3.6",
      "dev.zio" %% "zio" % "2.0.0-RC6",
      "org.scala-lang" % "scala-reflect" % scala213
    ),
  )

