// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "3.1.0"
  def scalaJSVersion = "1.7.1"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::2.0.0"
  )
}
