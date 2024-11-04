package com.babylon.wallet.android.presentation.transaction.analysis.summary

import com.babylon.wallet.android.presentation.transaction.PreviewType

interface SummaryToPreviewTypeAnalyzer<S: Summary> {

    suspend fun analyze(summary: S): PreviewType

}