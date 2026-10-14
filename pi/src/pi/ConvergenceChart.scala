package pi

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import scala.scalajs.js

/** Keeps checkpoints spaced across sample counts, so long runs use bounded memory. */
final class ConvergenceChart(canvas: Canvas) {
  private val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private var history = Vector.empty[PiState]
  private var nextCheckpoint = 1

  def record(state: PiState): Unit = {
    if (state.totalPoints >= nextCheckpoint) {
      history :+= state
      nextCheckpoint = math.max(state.totalPoints + 1, math.ceil(state.totalPoints * 1.04).toInt)
    }
  }

  def reset(): Unit = {
    history = Vector.empty
    nextCheckpoint = 1
    render(PiState.initialPiState)
  }

  def render(state: PiState): Unit = {
    // Match the displayed width so chart labels stay readable on small screens.
    val displayedWidth = canvas.clientWidth
    if (displayedWidth > 0 && canvas.width != displayedWidth) canvas.width = displayedWidth
    val left = 44.0
    val right = canvas.width - 100.0
    val top = 20.0
    val bottom = canvas.height - 52.0
    val maxLog = math.max(1.0, math.log10(math.max(1, state.totalPoints).toDouble))
    def x(count: Int): Double = left + math.log10(count.toDouble) / maxLog * (right - left)
    def y(estimate: Double): Double = bottom - estimate / 4.0 * (bottom - top)

    ctx.fillStyle = "#ffffff"
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.font = "12px sans-serif"
    ctx.lineWidth = 1
    ctx.textAlign = "right"
    for (value <- 0 to 4) {
      ctx.strokeStyle = "#e2e8f0"
      ctx.beginPath()
      ctx.moveTo(left, y(value))
      ctx.lineTo(right, y(value))
      ctx.stroke()
      ctx.fillStyle = "#475569"
      ctx.fillText(value.toString, left - 10, y(value) + 4)
    }
    ctx.textAlign = "center"
    for (power <- 0 to math.floor(maxLog).toInt) {
      val count = math.pow(10, power).toInt
      ctx.fillText(count.toString, x(count), bottom + 20)
    }
    ctx.fillText("Number of samples (log scale)", (left + right) / 2, canvas.height - 10)

    ctx.strokeStyle = "#b45309"
    ctx.setLineDash(js.Array(5.0, 4.0))
    ctx.beginPath()
    ctx.moveTo(left, y(math.Pi))
    ctx.lineTo(right, y(math.Pi))
    ctx.stroke()
    ctx.setLineDash(js.Array[Double]())
    ctx.textAlign = "left"
    ctx.fillStyle = "#b45309"
    ctx.fillText("π = 3.14159…", right + 8, y(math.Pi) + 4)

    if (state.totalPoints > 0) {
      val points = if (history.lastOption.contains(state)) history else history :+ state
      ctx.strokeStyle = "#2563eb"
      ctx.lineWidth = 2
      ctx.beginPath()
      points.zipWithIndex.foreach { (point, index) =>
        if (index == 0) ctx.moveTo(x(point.totalPoints), y(point.pi))
        else ctx.lineTo(x(point.totalPoints), y(point.pi))
      }
      ctx.stroke()
      ctx.fillStyle = "#2563eb"
      ctx.beginPath()
      ctx.arc(x(state.totalPoints), y(state.pi), 3, 0, 2 * math.Pi)
      ctx.fill()
    } else {
      ctx.fillStyle = "#475569"
      ctx.fillText("Sample points to start the chart", left + 18, top + 28)
    }
    canvas.setAttribute("aria-label", if (state.totalPoints == 0) {
      "Convergence chart. No samples yet. Reference pi is 3.14159."
    } else {
      s"Convergence chart after ${state.totalPoints} samples. Estimate ${state.pi}. Reference pi is 3.14159."
    })
  }
}
