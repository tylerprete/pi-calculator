// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "3.4.1"
  def scalaJSVersion = "1.16.0"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::2.8.0"
  )
}
