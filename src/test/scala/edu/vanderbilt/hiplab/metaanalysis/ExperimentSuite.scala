package edu.vanderbilt.hiplab.metaanalysis

import org.scalatest.FunSuite

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/19/12
 */

class ExperimentSuite extends FunSuite {
  def readPoints() = {
    List("start_n", "end_n").map(i => Helpers.property(i).toInt)
  }

  test("generates points around turning point") {
    val List(startN, endN) = readPoints()
    val cases = Experiment.pointsAroundTurning(startN, endN)
    assert(cases.length >= endN - startN)
  }

  test("generates test cases from highest to lowest") {
    val List(startN, endN) = readPoints()
    val cases = Experiment.generateTestCases(startN, endN)
    //println(cases.length)
    //cases.map(println)
    assert(cases.length >= endN - startN)
  }

  test("splits test cases into multiple instances") {
    val instances = Helpers.property("total_instances").toInt
    assert(instances > 0)

    val List(startN, endN) = readPoints()
    val cases = Experiment.generateTestCases(startN, endN)

    val perInstance = cases.length / instances
    val currentIndex = Helpers.property("current_instance").toInt * perInstance
    val currentInstances = cases.slice(currentIndex, currentIndex + perInstance)

    expect(perInstance)(currentInstances.length)
    expect(false)(currentInstances.isEmpty)
  }
}
