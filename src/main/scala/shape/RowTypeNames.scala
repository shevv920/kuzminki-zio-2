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

package kuzminki.shape

import scala.language.implicitConversions
import scala.util.{ Try, Success, Failure }
import scala.reflect.{classTag, ClassTag}
import scala.reflect.runtime.universe._

import kuzminki.api.KuzminkiError
import kuzminki.column._


trait RowTypeNames {

  protected val colTypeName: TypeCol[_] => String = {
    case col: StringCol             => "String"
    case col: BooleanCol            => "Boolean"
    case col: ShortCol              => "Short"
    case col: IntCol                => "Int"
    case col: LongCol               => "Long"
    case col: FloatCol              => "Float"
    case col: DoubleCol             => "Double"
    case col: BigDecimalCol         => "BigDecimal"
    case col: TimeCol               => "Time"
    case col: DateCol               => "Date"
    case col: TimestampCol          => "Timestamp"
    case col: UUIDCol               => "java.util.UUID"
    case col: StringOptCol          => "Option[String]"
    case col: BooleanOptCol         => "Option[Boolean]"
    case col: ShortOptCol           => "Option[Short]"
    case col: IntOptCol             => "Option[Int]"
    case col: LongOptCol            => "Option[Long]"
    case col: FloatOptCol           => "Option[Float]"
    case col: DoubleOptCol          => "Option[Double]"
    case col: BigDecimalOptCol      => "Option[BigDecimal]"
    case col: TimeOptCol            => "Option[Time]"
    case col: DateOptCol            => "Option[Date]"
    case col: TimestampOptCol       => "Option[Timestamp]"
    case col: UUIDOptCol            => "Option[java.util.UUID]"
    case col => throw KuzminkiError(s"Unsupported column type: [$col]")
  }

  protected val simplifyName: String => String = {
    case "java.sql.Time"                    => "Time"
    case "java.sql.Date"                    => "Date"
    case "java.sql.Timestamp"               => "Timestamp"
    case "Option[java.sql.Time"             => "Option[Time]"
    case "Option[java.sql.Date]"            => "Option[Date]"
    case "Option[java.sql.Timestamp]"       => "Option[Timestamp]"
    case name                               => name
  }

  protected def productTypeNames[T](implicit tag: TypeTag[T]) = {
    typeOf[T]
      .members
      .sorted
      .collect { case m: MethodSymbol if m.isCaseAccessor => m }
      .map(_.returnType.toString)
      .toSeq
      .map(simplifyName)
  }

  protected def validate[T](rti: RowTypeInfo[T]) = {

    val colTypeNames = rti.cols.map(colTypeName)
    val memberTypeNames = productTypeNames(rti.tTag)

    if (colTypeNames != memberTypeNames) {
      throw KuzminkiError(
        Seq(
          "Read error",
          "Column types: (%s)".format(colTypeNames.mkString(", ")),
          "Type members: (%s)".format(memberTypeNames.mkString(", "))
        ).mkString("\n")
      )
    }
  }
}