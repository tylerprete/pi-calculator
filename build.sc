// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "3.6.2"
  def scalaJSVersion = "1.17.0"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::2.8.0"
  )
}
