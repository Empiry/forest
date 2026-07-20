package com.empire.forest.util

import com.empire.forest.ForestContext
import com.empire.ignite.util.KEvent

class ForestMatchStartEvent(val context: ForestContext) : KEvent(false)