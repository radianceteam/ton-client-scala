package com.radiance.scala.methods

import org.scalatest.flatspec.AnyFlatSpec

class AbiModuleTest extends AnyFlatSpec with Config {

  behavior of "AbiModule"

  it should "have size 0" in {
    assert(Set.empty.size === 0)
  }

//  it should "produce NoSuchElementException when head is invoked" in {
//    assertThrows[NoSuchElementException] {
//      Set.empty.head
//    }
//  }
}