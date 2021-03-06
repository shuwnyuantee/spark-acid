/*
 * Copyright 2019 Qubole, Inc.  All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qubole.spark.hiveacid

import org.apache.spark.sql.{SaveMode, SqlUtils}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan

object HiveAcidErrors {

  def formatColumn(colName: String): String = s"`$colName`"

  def formatColumnList(colNames: Seq[String]): String =
    colNames.map(formatColumn).mkString("[", ", ", "]")

  def tableNotSpecifiedException(): Throwable = {
    new IllegalArgumentException("'table' is not specified in parameters")
  }

  def unsupportedFunction(function: String, caller: String): Throwable = {
    new java.lang.UnsupportedOperationException(s"Unsupported Function - $function with $caller")
  }

  def invalidOperationType(operation: String): Throwable = {
    new RuntimeException(s"Invalid operation type - $operation")
  }

  def unsupportedSaveMode(saveMode: SaveMode): Throwable = {
    new RuntimeException(s"Unsupported save mode - $saveMode")
  }

  def unsupportedOperationTypeInsertOnlyTable(operation: String, tableName: String): Throwable = {
    new RuntimeException(s"Unsupported operation type - $operation for InsertOnly table " + tableName)
  }

  def unsupportedOperationTypeBucketedTable(operation: String, tableName: String): Throwable = {
    new RuntimeException(s"Unsupported operation type - $operation for Bucketed table " + tableName)
  }

  def tableNotAcidException(tableName: String): Throwable = {
    new IllegalArgumentException(s"table $tableName is not an ACID table")
  }

  def couldNotAcquireLockException(exception: Exception = null): Throwable = {
    new RuntimeException(s"Could not acquire lock.", exception)
  }

  def couldNotAcquireLockException(state: String): Throwable = {
    new RuntimeException(s"Could not acquire lock. Lock State: $state")
  }

  def txnAlreadyClosed(txnId: Long): Throwable = {
    new RuntimeException(s"Transaction $txnId is already closed")
  }

  def txnAlreadyOpen(txnId: Long): Throwable = {
    new RuntimeException(s"Transaction already opened. Existing txnId: $txnId")
  }

  def txnNotStarted(table: String): Throwable = {
    new RuntimeException(s"Transaction on $table not started")
  }

  def txnNoTransaction(): Throwable = {
    new RuntimeException(s"No transaction found")
  }

  def tableSnapshotNonExistent(snapshotId: Long): Throwable = {
    new RuntimeException(s"Table snapshost $snapshotId does not exist")
  }

  def tableWriteIdRequestedBeforeTxnStart(table: String): Throwable = {
    new RuntimeException(s"Write id requested for table $table before txn was started")
  }

  def repeatedTxnId(txnId: Long, activeTxns: Seq[Long]): Throwable = {
    new RuntimeException(
      s"Repeated transaction id $txnId, active transactions are [${activeTxns.mkString(",")}]")
  }

  def unsupportedStreamingOutputMode(mode: String): Throwable = {
    new AnalysisException(
      s"mode is $mode: Hive Acid Sink supports only Append as OutputMode")
  }

  def updateSetColumnNotFound(col: String, colList: Seq[String]): Throwable = {
    new AnalysisException(
      s"SET column ${formatColumn(col)} not found among columns: ${formatColumnList(colList)}.")
  }

  def updateOnPartition(cols: Seq[String], table: String): Throwable = {
    val message = if (cols.length == 1) {
      s"SET column: ${cols.head} is partition column in table: ${table}"
    } else {
      s"SET columns: ${cols.mkString(",")} are partition columns in table: ${table}"
    }
    new AnalysisException(
      s"UPDATE on the partition columns are not allowed. $message"
    )
  }

  def txnOutdated(txnId: Long, tableName: String): Throwable = {
    new TransactionInvalidException(
      s"Transaction is $txnId is no longer valid for table $tableName", txnId, tableName)
  }

  def unexpectedReadError(cause: String): Throwable = {
    throw new RuntimeException(
      s"Unexpected error while reading the Hive Acid Data: $cause")
  }

  def mergeValidationError(cause: String): Throwable = {
    SqlUtils.analysisException(s"MERGE Validation Error: $cause")
  }

  def mergeResolutionError(cause: String): Throwable = {
    SqlUtils.analysisException(cause)
  }

  def mergeUnsupportedError(cause: String): Throwable = {
    throw new RuntimeException(cause)
  }
}

class TransactionInvalidException(val message:String,
                                  val txnId: Long,
                                  val tableName : String)
  extends Exception(message) {
  override def getMessage: String = {
    message
  }
}

class AnalysisException(
     val message: String,
     val line: Option[Int] = None,
     val startPosition: Option[Int] = None,
     // Some plans fail to serialize due to bugs in scala collections.
     @transient val plan: Option[LogicalPlan] = None,
     val cause: Option[Throwable] = None)
  extends Exception(message, cause.orNull) with Serializable {

  def withPosition(line: Option[Int], startPosition: Option[Int]): AnalysisException = {
    val newException = new AnalysisException(message, line, startPosition)
    newException.setStackTrace(getStackTrace)
    newException
  }

  override def getMessage: String = {
    val planAnnotation = Option(plan).flatten.map(p => s";\n$p").getOrElse("")
    getSimpleMessage + planAnnotation
  }

  // Outputs an exception without the logical plan.
  // For testing only
  def getSimpleMessage: String = {
    val lineAnnotation = line.map(l => s" line $l").getOrElse("")
    val positionAnnotation = startPosition.map(p => s" pos $p").getOrElse("")
    s"$message;$lineAnnotation$positionAnnotation"
  }
}