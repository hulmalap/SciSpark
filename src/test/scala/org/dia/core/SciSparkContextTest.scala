package org.dia.core

import org.scalatest.FunSuite

/**
 * Created by rahulsp on 7/9/15.
 */
class SciSparkContextTest extends FunSuite {

  test("testOpenDapURLFile") {

    val scisparkContext = SparkTestConstants.sc
    val srdd = scisparkContext.OpenDapURLFile("TestLinks", "TotCldLiqH2O_A")

    val collected = srdd.collect
    collected.map(p => println(p))
    assert(true)
  }

}
