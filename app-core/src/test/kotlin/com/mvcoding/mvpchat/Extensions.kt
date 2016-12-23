package com.mvcoding.mvpchat

import java.util.Random

fun anInt(limitExclusive: Int) = Random().nextInt(limitExclusive)
fun aString(string: String = "") = "$string${anInt(1000)}"