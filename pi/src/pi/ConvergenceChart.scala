package pi

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import scala.scalajs.js

/** Keeps checkpoints spaced across sample counts, so long runs use bounded memory. */
final class ConvergenceChart(canvas: Canvas) {
  private val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private var history = Vector.empty[PiState]
  private var nextCheckpoint = 1
  private var recentDetail = true

  def setRecentDetail(enabled: Boolean, state: PiState): Unit = {
    recentDetail = enabled
    render(state)
  }

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
    val left = 64.0
    val right = canvas.width - 100.0
    val top = 20.0
    val bottom = canvas.height - 52.0
    val maxLog = math.max(1.0, math.log10(math.max(1, state.totalPoints).toDouble))
    val minLog = if (recentDetail) math.max(0.0, maxLog - 1.0) else 0.0
    val samples = if (state.totalPoints == 0) Vector.empty else {
      if (history.lastOption.contains(state)) history else history :+ state
    }
    val points = samples.filter(point => math.log10(point.totalPoints.toDouble) >= minLog)
    val values = points.map(_.pi) :+ math.Pi
    val padding = math.max(0.002, (values.max - values.min) * 0.2)
    val minY = if (recentDetail && points.nonEmpty) math.max(0.0, values.min - padding) else 0.0
    val maxY = if (recentDetail && points.nonEmpty) math.min(4.0, values.max + padding) else 4.0
    def x(count: Int): Double = left + (math.log10(count.toDouble) - minLog) / (maxLog - minLog) * (right - left)
    def y(estimate: Double): Double = bottom - (estimate - minY) / (maxY - minY) * (bottom - top)
    def countLabel(count: Double): String = {
      if (count >= 1000000) f"${count / 1000000}%.1fM"
      else if (count >= 1000) f"${count / 1000}%.1fk"
      else math.round(count).toString
    }

    ctx.fillStyle = "#ffffff"
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.font = "12px sans-serif"
    ctx.lineWidth = 1
    ctx.textAlign = "right"
    for (tick <- 0 to 4) {
      val value = minY + tick * (maxY - minY) / 4
      ctx.strokeStyle = "#e2e8f0"
      ctx.beginPath()
      ctx.moveTo(left, y(value))
      ctx.lineTo(right, y(value))
      ctx.stroke()
      ctx.fillStyle = "#475569"
      val label = if (maxY - minY < 1) f"$value%.4f" else f"$value%.1f"
      ctx.fillText(label, left - 10, y(value) + 4)
    }
    ctx.textAlign = "center"
    if (recentDetail) {
      for (tick <- 0 to 4) {
        val fraction = tick / 4.0
        val count = math.pow(10, minLog + fraction * (maxLog - minLog))
        ctx.fillText(countLabel(count), left + fraction * (right - left), bottom + 20)
      }
    } else {
      for (power <- 0 to math.floor(maxLog).toInt) {
        val count = math.pow(10, power).toInt
        ctx.fillText(countLabel(count), x(count), bottom + 20)
      }
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
      ctx.fillText("No samples yet", left + 18, top + 28)
    }
    val description = if (state.totalPoints == 0) {
      "Convergence chart. No samples yet. Reference pi is 3.14159."
    } else {
      s"Convergence chart after ${state.totalPoints} samples. Estimate ${state.pi}. Reference pi is 3.14159."
    }
    val view = if (recentDetail) "Recent detail" else "Full history"
    canvas.setAttribute("aria-label", s"$description $view. Vertical axis from $minY to $maxY.")
  }
}
