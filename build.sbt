val scala212 = "2.12.15"
val scala213 = "2.13.8"

crossScalaVersions := Seq(scala212, scala213)

name := "kuzminki-zio-2"

version := "0.9.2-uuid3-zio2-rc6"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

lazy val root = (project in file("."))
  .settings(
    name := "kuzminki-zio",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.3.3",
      "dev.zio" %% "zio" % "2.0.0-RC6"
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => Seq("org.scala-lang" % "scala-reflect" % scala212)
        case Some((2, 13)) => Seq("org.scala-lang" % "scala-reflect" % scala213)
        case _ => Nil
      }
    }
  )

