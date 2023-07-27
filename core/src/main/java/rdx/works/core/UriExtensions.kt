package rdx.works.core

import android.net.Uri
import java.net.URLEncoder

fun Uri.toEncodedString(): String = URLEncoder.encode(toString(), "UTF-8")
