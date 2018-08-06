package loco

import doobie.util.log._

object RecordingLogHandler {

  case class Events(var events: List[LogEvent] = List()) {
    def append(event: LogEvent): Unit = {
      events = events :+ event
    }

    def clear(): Unit = events = List()
  }

  def logHandler: (Events, LogHandler) = {
    val events = Events()
    val logHandler = LogHandler(any => events.append(any))
    (events, logHandler)
  }

}
