/*
* Copyright 2021 Kári Magnússon
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

package kuzminki.api

import zio._
import zio.ZIO.attemptBlocking
import kuzminki.api._
import kuzminki.jdbc.SingleConnection
import kuzminki.render.{
  RenderedQuery,
  RenderedOperation
}


object Kuzminki {

  Class.forName("org.postgresql.Driver")

  private def createPool(conf: DbConfig): RIO[Any, Pool] = for {
    connections <- ZIO.foreach(1 to conf.poolSize) { _ =>
                     attemptBlocking {
                       SingleConnection.create(conf.url, conf.props)
                     }
                   }
    queue       <- Queue.bounded[SingleConnection](conf.poolSize)
    _           <- queue.offerAll(connections)
  } yield Pool(queue, connections.toList)

  def forConfig(conf: DbConfig) = create(conf)

  def create(conf: DbConfig): RIO[Any, Kuzminki] = for {
    pool <- createPool(conf)
  } yield new DefaultApi(pool)

  def layer(conf: DbConfig): ZLayer[Any, Throwable, Kuzminki] = {
    ZLayer.scoped(ZIO.acquireRelease(create(conf))(_.close))
  }

  def createSplit(getConf: DbConfig,
                  setConf: DbConfig): RIO[Any, Kuzminki] = for {
    getPool <- createPool(getConf)
    setPool <- createPool(setConf)
  } yield new SplitApi(getPool, setPool)

  def layerSplit(getConf: DbConfig,
                 setConf: DbConfig): ZLayer[Any, Throwable, Kuzminki] = {
    ZLayer.scoped(ZIO.acquireRelease(createSplit(getConf, setConf))(_.close))
  }

  def get = ZIO.service[Kuzminki]
}


trait Kuzminki {

  def query[R](render: => RenderedQuery[R]): RIO[Any, List[R]]

  def queryHead[R](render: => RenderedQuery[R]): RIO[Any, R]

  def queryHeadOpt[R](render: => RenderedQuery[R]): RIO[Any, Option[R]]

  def exec(render: => RenderedOperation): RIO[Any, Unit]

  def execNum(render: => RenderedOperation): RIO[Any, Int]

  def close: URIO[Any, Unit]
}


private case class Pool(
  queue: Queue[SingleConnection],
  all: List[SingleConnection]
)


private class DefaultApi(pool: Pool) extends Kuzminki {

  def query[R](render: => RenderedQuery[R]): RIO[Any, List[R]] = for {
    stm  <- ZIO.attempt { render }
    conn <- pool.queue.take
    rows <- conn.query(stm).ensuring { pool.queue.offer(conn) }
  } yield rows

  def queryHead[R](render: => RenderedQuery[R]): RIO[Any, R] = for {
    stm  <- ZIO.attempt { render }
    conn <- pool.queue.take
    rows <- conn.query(stm).ensuring { pool.queue.offer(conn) }
    head <- ZIO.attempt { rows.head }
  } yield head

  def queryHeadOpt[R](render: => RenderedQuery[R]): RIO[Any, Option[R]] = for {
    stm     <- ZIO.attempt { render }
    conn    <- pool.queue.take
    rows    <- conn.query(stm).ensuring { pool.queue.offer(conn) }
    headOpt <- ZIO.attempt { rows.headOption }
  } yield headOpt

  def exec(render: => RenderedOperation): RIO[Any, Unit] = for {
    stm  <- ZIO.attempt { render }
    conn <- pool.queue.take
    _    <- conn.exec(stm).ensuring { pool.queue.offer(conn) }
  } yield ()

  def execNum(render: => RenderedOperation): RIO[Any, Int] = for {
    stm  <- ZIO.attempt { render }
    conn <- pool.queue.take
    num  <- conn.execNum(stm).ensuring { pool.queue.offer(conn) }
  } yield num

  def close: URIO[Any, Unit] = for {
    _ <- ZIO.foreach(pool.all)(_.close()).orDie
    _ <- pool.queue.shutdown
  } yield ()
}


private class SplitApi(getPool: Pool, setPool: Pool) extends Kuzminki {

  def query[R](render: => RenderedQuery[R]): RIO[Any, List[R]] = for {
    stm  <- ZIO.attempt { render }
    conn <- getPool.queue.take
    rows <- conn.query(stm).ensuring { getPool.queue.offer(conn) }
  } yield rows

  def queryHead[R](render: => RenderedQuery[R]): RIO[Any, R] = for {
    stm  <- ZIO.attempt { render }
    conn <- getPool.queue.take
    rows <- conn.query(stm).ensuring { getPool.queue.offer(conn) }
    head <- ZIO.attempt { rows.head }
  } yield head

  def queryHeadOpt[R](render: => RenderedQuery[R]): RIO[Any, Option[R]] = for {
    stm     <- ZIO.attempt { render }
    conn    <- getPool.queue.take
    rows    <- conn.query(stm).ensuring { getPool.queue.offer(conn) }
    headOpt <- ZIO.attempt { rows.headOption }
  } yield headOpt

  def exec(render: => RenderedOperation): RIO[Any, Unit] = for {
    stm  <- ZIO.attempt { render }
    conn <- setPool.queue.take
    _    <- conn.exec(stm).ensuring { setPool.queue.offer(conn) }
  } yield ()

  def execNum(render: => RenderedOperation): RIO[Any, Int] = for {
    stm  <- ZIO.attempt { render }
    conn <- setPool.queue.take
    num  <- conn.execNum(stm).ensuring { setPool.queue.offer(conn) }
  } yield num

  def close: URIO[Any, Unit] = for {
    _ <- ZIO.foreach(getPool.all)(_.close()).orDie
    _ <- getPool.queue.shutdown
    _ <- ZIO.foreach(setPool.all)(_.close()).orDie
    _ <- setPool.queue.shutdown
  } yield ()
}
























