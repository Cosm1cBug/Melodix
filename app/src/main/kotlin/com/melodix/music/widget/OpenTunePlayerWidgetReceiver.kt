/*
 * Melodix Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.melodix.music.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MelodixPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodixPlayerWidget()
}
