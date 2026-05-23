package loco.repository.persistent.skunk

case class EventsTableConfiguration(
    eventsTable: String,
    aggregateIdColumn: String,
    aggregateVersionColumn: String,
    eventColumn: String,
    createdAtColumn: String) {

  val setup: String =
    s"""
       create table $eventsTable(
        $aggregateIdColumn varchar(36) not null,
        $aggregateVersionColumn int not null,
        $createdAtColumn timestamp not null,
        $eventColumn bytea not null,
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
