package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.ActiveExam

/** Same-boot monotonic time is unaffected by wall-clock changes; after reboot use wall time. */
fun elapsedExamSeconds(exam: ActiveExam, wallMs: Long, monotonicMs: Long, bootCount: Int?): Int {
    val duration = exam.pack.durationMinutes.coerceAtLeast(0).toLong() * 60
    val sameBoot = bootCount != null && bootCount == exam.bootCount && monotonicMs >= exam.startedAtElapsedMs
    val elapsedMs = if (sameBoot) monotonicMs - exam.startedAtElapsedMs else wallMs - exam.startedAtEpochMs
    return (elapsedMs.coerceAtLeast(0) / 1000).coerceAtMost(duration).toInt()
}
