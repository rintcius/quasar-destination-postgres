/*
 * Copyright 2014–2019 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.plugin.postgres

import slamdata.Predef._

import doobie.enum.JdbcType
import doobie.postgres.implicits._
import doobie.util.Meta

import java.time._

import org.postgresql.util.PGInterval

import qdata.time._

import scala.Predef.classOf

/** doobie.util.Meta instances for additional temporal types supported by Postgres. */
trait PGTemporalMeta {
  implicit val localTimeMeta: Meta[LocalTime] =
    Meta.Basic.one(
      JdbcType.Time,
      Nil,
      _.getObject(_, classOf[LocalTime]),
      _.setObject(_, _),
      _.updateObject(_, _))

  implicit val localDateTimeMeta: Meta[LocalDateTime] =
    Meta.Basic.one(
      JdbcType.Timestamp,
      Nil,
      _.getObject(_, classOf[LocalDateTime]),
      _.setObject(_, _),
      _.updateObject(_, _))

  implicit val offsetDateTimeMeta: Meta[OffsetDateTime] =
    Meta.Basic.one(
      JdbcType.TimestampWithTimezone,
      Nil,
      _.getObject(_, classOf[OffsetDateTime]),
      _.setObject(_, _),
      _.updateObject(_, _))

  implicit val dateTimeIntervalMeta: Meta[DateTimeInterval] = {
    def from(pg: PGInterval): DateTimeInterval = {
      val s = pg.getSeconds
      val ns = ((s - s.toLong) * 1000000000).toLong
      val ss = (pg.getHours.toLong * 3600L) + (pg.getMinutes.toLong * 60L) + s.toLong

      DateTimeInterval.make(pg.getYears, pg.getMonths, pg.getDays, ss, ns)
    }

    def to(i: DateTimeInterval): PGInterval = {
      val d = i.duration
      val p = i.period

      val ns = (d.getNano * 0.000000001).floor
      val ss = d.getSeconds.toDouble + ns

      new PGInterval(p.getYears, p.getMonths, p.getDays, 0, 0, ss)
    }

    Meta[PGInterval].timap(from)(to)
  }
}

object PGTemporalMeta extends PGTemporalMeta
