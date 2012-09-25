package main

/**
 * @description Helper methods for project
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/30/12
 */

import java.io.{File, FileInputStream}
import java.util.Properties
import paillierp.key.{KeyGen, PaillierKey}
import java.math.BigInteger
import paillierp.{PaillierThreshold, Paillier}

object Helpers {
  val MyProperties = new Properties()

  /**
   * Find value corresponding to queried property in system config
   * @param key property name
   * @return value corresponding to the queried property
   */
  def property(key: String) = {
    MyProperties.load(new FileInputStream("conf.properties"))
    MyProperties.getProperty(key)
  }

  /**
   * Add data-directory prefix to queried file path
   * We don't like the overhead introduced by new File(bla1, bla2).toString
   * @param key property name
   * @return file path with data-directory prefixed
   */
  def propertyFullPath(key: String) = {
    MyProperties.load(new FileInputStream("conf.properties"))
    MyProperties.getProperty("data_directory") + System.getProperty("file.separator") +
      MyProperties.getProperty(key)
  }

  /**
   * Read in public keys from file
   * @param filename  file storing public key
   * @return
   */
  def getPublicKey(filename: String = "") = {
    val filename_ = if (filename.equals("")) {
      new File(Helpers.property("data_directory"), Helpers.property("public_keys")).toString
    } else {
      filename
    }

    val buffer = new java.io.BufferedInputStream(new java.io.FileInputStream(filename_))
    val input = new java.io.ObjectInputStream(buffer)
    val publicKey = input.readObject().asInstanceOf[PaillierKey]

    input.close()
    buffer.close()

    publicKey
  }

  def toBigInteger(value: Double) = new BigInteger("%.0f" format value)
}
