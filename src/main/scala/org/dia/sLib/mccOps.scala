package org.dia.sLib

import breeze.linalg.DenseMatrix
import org.dia.core.sciTensor
import org.dia.tensors.AbstractTensor

object mccOps {

  val BACKGROUND=0.0

  def reduceResolution(tensor: AbstractTensor, blockSize: Int): AbstractTensor = {
    val largeArray = tensor
    val numRows = largeArray.rows
    val numCols = largeArray.cols
    val reducedSize = numRows * numCols / (blockSize * blockSize)

    val reducedMatrix = tensor.zeros(numRows / blockSize, numCols / blockSize)

    for (row <- 0 to (reducedMatrix.rows - 1)) {
      for (col <- 0 to (reducedMatrix.cols - 1)) {
        val rowRange = (row * blockSize) -> (((row + 1) * blockSize))
        val columnRange = (col * blockSize) -> (((col + 1) * blockSize))
        val block = tensor(rowRange, columnRange)
        val numNonZero = block.data.filter(p => p != 0).size
        val avg = if (numNonZero > 0) (block.cumsum / numNonZero) else 0.0
        reducedMatrix.put(avg, row, col)
      }
    }
    reducedMatrix
  }

  def labelConnectedComponents(tensor: AbstractTensor): (AbstractTensor, Int) = {
    val fourVector = List((1,0), (-1,0), (0,1), (0,-1))
    val rows = tensor.rows
    val cols = tensor.cols
    val labels = tensor.zeros(tensor.shape :_*)
    var label = 1

    /**
     * If the coordinates are within bounds,
     * the input is not 0, and it hasn't been labelled yet
     * @param row
     * @param col
     * @return
     */
    def isLabeled(row : Int, col : Int) : Boolean = {
      if(row < 0 || col < 0 || row >= rows || col >= cols) return true
      tensor(row, col) == BACKGROUND || labels(row, col) != BACKGROUND
    }

    def dfs(row : Int, col : Int, currentLabel : Int) : Unit = {
      if(isLabeled(row, col)) return
      labels.put(currentLabel, row, col)

      val neighbors = fourVector.map(p => (p._1 + row, p._2 + col))
      for(neighbor <- neighbors) dfs(neighbor._1, neighbor._2, currentLabel)
    }

    //First Pass
    for(row <- 0 to (rows - 1)) {
      for(col <- 0 to (cols - 1)) {
          if(!isLabeled(row, col)) {
            dfs(row, col, label)
            label += 1
          }
        }
      }
    (labels, label - 1)
  }

  def findCloudElements(tensor: AbstractTensor): List[AbstractTensor] = {
    val tuple = labelConnectedComponents(tensor)
    val labelled = tuple._1
    val maxVal = tuple._2
    val maskedLabels = (1 to maxVal).toArray.map(labelled := _.toDouble)
    maskedLabels.toList
  }

  def findCloudElements(tensor: sciTensor): List[sciTensor] = {
    val labelledTensors = findCloudElements(tensor.tensor)
    val absT : AbstractTensor = tensor.tensor

    val seq = (0 to labelledTensors.size - 1).map(p => {
      val masked : AbstractTensor = labelledTensors(p).map(a => if(a != 0.0) 1.0 else a)

      val metaTensor = tensor.tensor * masked
      val max = metaTensor.max
      val min = metaTensor.min
      val area = areaFilled(masked)
      val metadata = tensor.metaData += (("AREA", "" + area)) += (("DIFFERENCE", "" + (max - min))) += (("COMPONENT", "" + p))
      val k = new sciTensor(tensor.varInUse, masked, metadata)
      k
    })
    seq.toList
  }

  /////=================/////=================/////=================/////=================
  /////=================/////=================/////=================/////=================
  def findCloudElementsX(tensor: AbstractTensor): (AbstractTensor, Int) = {
    val tuple = labelConnectedComponents(tensor)
//    val labelled = tuple._1
//    val maxVal = tuple._2
//    val maskedLabels = (1 to maxVal).toArray.map(labelled := _.toDouble)
//    maskedLabels.toList
    tuple
  }

  def findCloudElementsX(tensor: sciTensor): sciTensor = {
    // list of connected components separated in maskes matrices
    val labelledTensor = findCloudElementsX(tensor.tensor)
    val metadata = tensor.metaData += (("NUM_COMPONENTS", "" + labelledTensor._2))
    new sciTensor(tensor.varInUse, labelledTensor._1, metadata)

    // adding metadata
//    val seq = (0 to labelledTensors.size - 1).map(p => {
//      val masked : AbstractTensor = labelledTensors(p).map(a => if(a != 0.0) 1.0 else a)

//      val metaTensor = tensor.tensor * masked
//      val max = metaTensor.max
//      val min = metaTensor.min
//      val area = areaFilled(masked)
//      val metadata = tensor.metaData += (("AREA", "" + area)) += (("DIFFERENCE", "" + (max - min))) += (("COMPONENT", "" + p))
//      val k = new sciTensor(tensor.varInUse, masked, metadata)
//      k
//    })
//    seq.toList
  }

  /////=================/////=================/////=================/////=================
  /////=================/////=================/////=================/////=================

  def areaFilled(tensor : AbstractTensor) : Double = {
    var count = 0
    val masked = tensor.map(p => if(p != 0.0) 1.0 else p)
    val sum = masked.cumsum
    sum
  }
}