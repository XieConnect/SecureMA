package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Test Data Owners
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 9/24/12
 */

import org.scalatest.FunSuite
import java.io.File

class OwnerSuite extends FunSuite {
  val rawDataFile = Helpers.property("raw_data_file")

  test("file containing raw data is not empty") {
    expect(true) (new File(rawDataFile).exists())
    assert(io.Source.fromFile(rawDataFile).getLines().drop(1).size > 0)
  }

  test("prepareData() outputs non-empty result") {
    val encryptedFile = Helpers.property("encrypted_data_file")
    Owner.prepareData(rawDataFile, encryptedFile)
    val encryptedLines = io.Source.fromFile(encryptedFile).getLines().toArray
    assert(encryptedLines.size > 1)
    expect(Helpers.getMultiplier()) (encryptedLines(0).split(",")(1).toDouble)
  }

  test("verify correctness of encrypted result") {
    val encryptedFile = Helpers.property("encrypted_data_file")
    expect(true) (Owner.verifyEncryption(encryptedFile))
  }

}
