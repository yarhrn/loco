package loco.repository.persistent.sql

case class EventsTableConfiguration(eventsTable: String,
                                    aggregateIdColumn: String,
                                    aggregateVersionColumn: String,
                                    eventColumn: String,
                                    createdAtColumn: String) {

  val setup: String =
    s"""
       create table $eventsTable(
        $aggregateIdColumn varchar(36) not null,
        $aggregateVersionColumn int not null,
        $createdAtColumn datetime(3) not null,
        $eventColumn text not null,
        primary key($aggregateIdColumn,$aggregateVersionColumn)
       )
    """


}

object EventsTableConfiguration {
  def base(event: String) = {
    EventsTableConfiguration(
      eventsTable = s"${event}_events",
      aggregateVersionColumn = s"version",
      aggregateIdColumn = s"id",
      eventColumn = s"event",
      createdAtColumn = s"created_at"
    )
  }
}
