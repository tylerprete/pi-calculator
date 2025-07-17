// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "3.7.1"
  def scalaJSVersion = "1.19.0"

  def mvnDeps = Seq(
    mvn"org.scala-js::scalajs-dom::2.8.0"
  )
}
