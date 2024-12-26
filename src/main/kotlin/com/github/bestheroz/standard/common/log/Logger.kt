package com.github.bestheroz.standard.common.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)