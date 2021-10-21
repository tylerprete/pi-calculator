package pi

import org.scalajs.dom
import org.scalajs.dom.ImageData
import org.scalajs.dom.html.{Button, Canvas, Form, Input}

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle

object PiCalculator {

  var incrementStep = 1

  val green: String = "#008000"
  val red: String = "#B0171F"

  var piState = PiState.initialPiState

  val canvas = dom.document.getElementById("piCanvas").asInstanceOf[Canvas]

  val ctx = canvas.getContext("2d")
    .asInstanceOf[dom.CanvasRenderingContext2D]

  var canvasData = ctx.getImageData(0, 0, canvas.width, canvas.height)

  @JSExport("App")
  def main(args: Array[String]): Unit = {
    //    dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
    val button = dom.document.getElementById("next-pi").asInstanceOf[Button]
    val auto = dom.document.getElementById("auto-increment").asInstanceOf[Button]
    val stop = dom.document.getElementById("stop-auto").asInstanceOf[Button]

    val reset = dom.document.getElementById("reset").asInstanceOf[Button]

    val form = dom.document.getElementById("multiplier").asInstanceOf[Form]
//    val inc = form.elements.item("inc").asInstanceOf[Input]
    val t = dom.document.getElementsByName("inc")
    t.foreach {
      i =>
        val r = i.asInstanceOf[Input]
        r.onclick = (e: dom.MouseEvent) => incrementStep = r.value.toInt
    }

    //
    //    val b1x = dom.document.getElementById("1x").asInstanceOf[Button]
    //    val b10x = dom.document.getElementById("10x").asInstanceOf[Button]
    //    val b100x = dom.document.getElementById("100x").asInstanceOf[Button]
    //    val b1000x = dom.document.getElementById("1000x").asInstanceOf[Button]
    //
    //    b1x.onclick = (e: dom.MouseEvent) => incrementStep = 1
    //    b10x.onclick = (e: dom.MouseEvent) => incrementStep = 10
    //    b100x.onclick = (e: dom.MouseEvent) => incrementStep = 100
    //    b1000x.onclick = (e: dom.MouseEvent) => incrementStep = 1000


    button.onclick = (e: dom.MouseEvent) => incrementNtimes(incrementStep)
    auto.onclick = (e: dom.MouseEvent) => { println("Start!"); AutoIncrementer.startIncrementing() }
    stop.onclick = (e: dom.MouseEvent) => { println("Stop!"); AutoIncrementer.stopIncrementing() }
    reset.onclick = (e: dom.MouseEvent) => { println("Reset!"); resetState() }

    setupCanvas()
    canvasData = ctx.getImageData(0, 0, canvas.width, canvas.height)
  }

  private def resetState() = {
    AutoIncrementer.stopIncrementing()
    piState = PiState.initialPiState
    renderPiState(piState)
    setupCanvas()
    canvasData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    ctx.putImageData(canvasData, 0, 0)
  }

  private def setupCanvas() = {
    /*setup*/
    val height = canvas.height
    val width = canvas.width

    ctx.fillStyle = "#f8f8f8"
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    ctx.beginPath()
    ctx.arc(height / 2,width / 2,height / 2 - 1,0,2*Math.PI)
    ctx.stroke()
  }

  def incrementNtimes(n: Int = 1) = {
    for (_ <- 0 until n) {
      piState = incrementPiState(canvasData: ImageData)
    }
    ctx.putImageData(canvasData, 0, 0)
    renderPiState(piState)
  }

  def drawPixel(imgData: ImageData, x: Int, y: Int, c: Color) = {
    val index = (x + y * canvas.width) * 4
    imgData.data(index + 0) = c.r
    imgData.data(index + 1) = c.g
    imgData.data(index + 2) = c.b
    imgData.data(index + 3) = 255
  }

  def incrementPiState(canvasData: ImageData): PiState = {
    val (newState, point, inCircle) = piState.next()

    val scaledX = Math.round(point.x * (canvas.width / 2.0))
    val scaledY = Math.round(point.y * (canvas.height / 2.0))
    //    println(s"scaledX: $scaledX -- scaledY: $scaledY")

    val c = if (inCircle) Color(green) else Color(red)


    //    ctx.fillStyle = if (inCircle) {
    //      green
    //    } else {
    //      red
    //    }
    //    ctx.fillStyle = "#000000"
    //    ctx.fillRect(scaledX, scaledY, 1, 1)
    drawPixel(canvasData, scaledX.toInt, scaledY.toInt, c)

    newState
  }

  def renderPiState(piState: PiState): Unit = {
    dom.document.getElementById("total-points").textContent = piState.totalPoints.toString
    dom.document.getElementById("total-points2").textContent = piState.totalPoints.toString
    dom.document.getElementById("in-circle").textContent = piState.pointsInCircle.toString
    dom.document.getElementById("in-circle2").textContent = piState.pointsInCircle.toString
    dom.document.getElementById("pi").textContent = piState.pi.toString
  }
}

@JSExportTopLevel("AutoIncrementer")
@JSExportAll
object AutoIncrementer {
  var maybeIntervalHandle: Option[SetIntervalHandle] = None

  def startIncrementing(): Unit = {
    maybeIntervalHandle =
      maybeIntervalHandle.orElse {
        Some(timers.setInterval(20)(PiCalculator.incrementNtimes(PiCalculator.incrementStep)))
      }
  }

  def stopIncrementing(): Unit = {
    maybeIntervalHandle.foreach { handle =>
      timers.clearInterval(handle)
    }
    maybeIntervalHandle = None
  }
}
