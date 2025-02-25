/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2018 Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.spark.sql.rf

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types._
import org.locationtech.rasterframes.ref.RFRasterSource
import org.locationtech.rasterframes.util.KryoSupport

import java.nio.ByteBuffer

/**
 * Catalyst representation of a RasterSource.
 *
 * @since 9/5/18
 */
// TODO: remove it
@SQLUserDefinedType(udt = classOf[RasterSourceUDT])
class RasterSourceUDT extends UserDefinedType[RFRasterSource] {
  override def typeName = "rastersource"

  override def pyUDT: String = "pyrasterframes.rf_types.RasterSourceUDT"

  def userClass: Class[RFRasterSource] = classOf[RFRasterSource]

  def sqlType: DataType = StructType(Seq(
    StructField("raster_source_kryo", BinaryType, false)
  ))

  def serialize(obj: RFRasterSource): InternalRow =
    Option(obj)
      .map { rs => InternalRow(KryoSupport.serialize(rs).array()) }
      .orNull

  def deserialize(datum: Any): RFRasterSource =
    Option(datum)
      .collect {
        case ir: InternalRow =>
          val bytes = ir.getBinary(0)
          KryoSupport.deserialize[RFRasterSource](ByteBuffer.wrap(bytes))
        case bytes: Array[Byte] =>
          KryoSupport.deserialize[RFRasterSource](ByteBuffer.wrap(bytes))

      }
      .orNull

  private[sql] override def acceptsType(dataType: DataType): Boolean = dataType match {
    case _: RasterSourceUDT => true
    case _ => super.acceptsType(dataType)
  }
}

object RasterSourceUDT {
  UDTRegistration.register(classOf[RFRasterSource].getName, classOf[RasterSourceUDT].getName)

  /** Deserialize a byte array, also used inside the Python API */
  def from(byteArray: Array[Byte]): RFRasterSource =
    KryoSupport.deserialize[RFRasterSource](ByteBuffer.wrap(byteArray))
}
