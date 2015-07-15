/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dia.b

import breeze.linalg.{DenseMatrix, sum}
import org.dia.NetCDFUtils
import org.dia.TRMMUtils.Constants._
import org.dia.core.ArrayLib
import org.slf4j.Logger
import ucar.ma2
import ucar.nc2.dataset.NetcdfDataset

import scala.collection.mutable
import scala.language.implicitConversions

/**
 * Functions needed to perform operations with Breeze
 * We map every dimension to an index ex : dimension 1 -> Int 1, dimension 2 -> Int 2 etc.
 */
object BreezeLib extends ArrayLib{

  // Class logger
  val LOG : Logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  /**
   * Breeze implementation
   * @param url where the netcdf file is located
   * @param variable the NetCDF variable to search for
   * @return
   */
  def getNetCDFTRMMVars (url : String, variable : String) : DenseMatrix[Double] = {
    val netcdfFile = NetCDFUtils.loadNetCDFDataSet(url)

    val rowDim = NetCDFUtils.getDimensionSize(netcdfFile, X_AXIS_NAMES(0))
    val columnDim = NetCDFUtils.getDimensionSize(netcdfFile, Y_AXIS_NAMES(0))

    val coordinateArray = NetCDFUtils.convertMa2ArrayTo1DJavaArray(netcdfFile, variable)
    val matrix = new DenseMatrix[Double](rowDim, columnDim, coordinateArray, 0)
    matrix
  }

  /**
   * Gets an NDimensional array of Breeze's DenseMatrices from a NetCDF file
   * @param url where the netcdf file is located
   * @param variable the NetCDF variable to search for
   * @return
   */
  def getNetCDFNDVars (url : String, variable : String) : Array[DenseMatrix[Double]] = {
    val netcdfFile = NetCDFUtils.loadNetCDFDataSet(url)
    val SearchVariable: ma2.Array = NetCDFUtils.getNetCDFVariableArray(netcdfFile, variable)
    val ArrayClass = Array.ofDim[Float](240, 1, 201 ,194)
    val NDArray = SearchVariable.copyToNDJavaArray().asInstanceOf[ArrayClass.type]
    // we can only do this because the height dimension is 1
    val j = NDArray(0)(0).flatMap(f => f)
    val any = NDArray.map(p => new DenseMatrix[Double](201, 194, p(0).flatMap(f => f).map(d => d.toDouble), 0))
    any
  }

  /**
   * Reduces the resolution of a DenseMatrix
   * @param largeArray the array that will be reduced
   * @param blockSize the size of n x n size of blocks.
   * @return
   */
  def reduceResolution(largeArray: DenseMatrix[Double], blockSize: Int): DenseMatrix[Double] = {
    val numRows = largeArray.rows
    val numCols = largeArray.cols

    val reducedSize = numRows * numCols / (blockSize * blockSize)
    val reducedMatrix = DenseMatrix.zeros[Double](numRows / blockSize, numCols / blockSize)

    for(row <- 0 to reducedMatrix.rows - 1){
      for(col <- 0 to reducedMatrix.cols - 1){
        val rowIndices = (row * blockSize) to ((row + 1) * blockSize - 1)
        val colIndices = (col * blockSize) to ((col + 1) * blockSize - 1)
        val block = largeArray(rowIndices, colIndices)
        val totalsum = sum(block)
        val validCount = block.findAll(p => p != 0.0).size.toDouble
        val average = if(validCount > 0) totalsum / validCount else 0.0
        reducedMatrix(row to row, col to col) := average
        reducedMatrix
      }
    }
    reducedMatrix
  }

  /**
   * Creates a 2D array from a list of dimensions using a variable
   * @param dimensionSizes hashmap of (dimension, size) pairs
   * @param netcdfFile the NetcdfDataset to read
   * @param variable the variable array to extract
   * @return DenseMatrix
   */
  def create2dArray(dimensionSizes: mutable.HashMap[Int, Int], netcdfFile: NetcdfDataset, variable: String): DenseMatrix[Double] = {
    //TODO make sure that the dimensions are always in the order we want them to be
    try {
      val x = dimensionSizes.get(1).get
      val y = dimensionSizes.get(2).get
      val coordinateArray = NetCDFUtils.convertMa2ArrayTo1DJavaArray(netcdfFile, variable)
      new DenseMatrix[Double](x, y, coordinateArray)
    } catch {
      case e :
        java.util.NoSuchElementException => LOG.error("Required dimensions not found. Found:%s".format(dimensionSizes.toString()))
        null
    }
  }

  /**
   * Creates a 4D dimensional array from a list of dimensions
   * Note that this as return type gets boxed into Array[Array[Array[Array[Double]]]]
   *
   * TODO :: Find a better way to index the dimensions. However we may never need to use this function
   * @param dimensionSizes hashmap of (dimension, size) pairs
   * @param netcdfFile the NetcdfDataset to read
   * @param variable the variable array to extract
   * @return Array.ofDim[x,y,z,u]
   */
  def create4dArray(dimensionSizes: mutable.HashMap[Int, Int], netcdfFile: NetcdfDataset, variable: String): Array[Array[Array[Array[Float]]]] = {

    val SearchVariable: ma2.Array = NetCDFUtils.getNetCDFVariableArray(netcdfFile, variable)

    val sortedDimensions = dimensionSizes.toArray.sortBy(_._1).map(_._2)
    val x = sortedDimensions(0)
    val y = sortedDimensions(1)
    val z = sortedDimensions(2)
    val u = sortedDimensions(3)

    val ArrayClass = Array.ofDim[Float](x, y, z, u)
    val NDArray = SearchVariable.copyToNDJavaArray().asInstanceOf[Array[Array[Array[Array[Float]]]]]

    NDArray
  }
}

