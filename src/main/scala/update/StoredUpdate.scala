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

package kuzminki.update

import zio._
import kuzminki.api.{db, Kuzminki}
import kuzminki.shape.ParamConv
import kuzminki.render.RenderedOperation


class StoredUpdate[P1, P2](
    statement: String,
    changes: ParamConv[P1],
    filters: ParamConv[P2],
  ) {

  def render(changeArgs: P1, filterArgs: P2) = {
    RenderedOperation(
      statement,
      changes.fromShape(changeArgs) ++ filters.fromShape(filterArgs)
    )
  }

  // run

  def run(changeArgs: P1, filterArgs: P2): RIO[Kuzminki, Unit] =
    db.exec(render(changeArgs, filterArgs))

  def runNum(changeArgs: P1, filterArgs: P2): RIO[Kuzminki, Int] =
    db.execNum(render(changeArgs, filterArgs))

  // debug

  def debugSql(handler: String => Unit) = {
    handler(statement)
    this
  }
}
