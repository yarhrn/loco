package loco

import cats.effect.IO
import doobie.util.log._

object RecordingLogHandler {

  case class Events(var events: List[LogEvent] = List()) {
    def append(event: LogEvent): Unit = {
      events = events :+ event
    }

    def clear(): Unit = events = List()
  }

  def logHandler: (Events, LogHandler[IO]) = {
    val events = Events()
    val logHandler = new LogHandler[IO] {
      override def run(logEvent: LogEvent): IO[Unit] = IO {
        events.append(logEvent)
      }
    }
    (events, logHandler)
  }

}
