package org.falaeapp.falae.model

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by marce on 03/02/2018.
 */
data class DownloadCache(val name: String, val sources: ConcurrentHashMap<String, String>)