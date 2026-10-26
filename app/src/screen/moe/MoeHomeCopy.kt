/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.*

@Composable
internal fun MoeHomeCopyBlock(
    nowMillis: Long,
    quoteText: String,
    quoteAuthor: String,
    greetingColor: Color,
    color: Color,
    modifier: Modifier = Modifier,
    launchContent: @Composable (() -> Unit)? = null,
) {
    val greeting = remember(nowMillis) { moeGreetingText(nowMillis) }
    Column(modifier, Arrangement.spacedBy(MoeUi.Quote.contentGap)) {
        Text(
            text = greeting,
            color = greetingColor,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
            fontSize = MoeUi.Quote.greetingSize,
            lineHeight = MoeUi.Quote.greetingLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = MoeUi.Quote.quoteTopGap),
            verticalArrangement = Arrangement.spacedBy(MoeUi.Quote.authorTopGap),
        ) {
            Text(
                text = quoteText,
                color = color.copy(alpha = 0.88f),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Medium,
                fontSize = MoeUi.Quote.textSize,
                lineHeight = MoeUi.Quote.lineHeight,
                softWrap = true,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
            if (quoteAuthor.isNotBlank()) {
                Text(
                    text = "— $quoteAuthor",
                    modifier = Modifier.fillMaxWidth(),
                    color = color.copy(alpha = MoeUi.Quote.authorAlpha),
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Medium,
                    fontSize = MoeUi.Quote.authorSize,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        launchContent?.let { content ->
            Box(Modifier
                .fillMaxWidth()
                .padding(top = MoeUi.Hero.launchTopGap)) { content() }
        }
    }
}

private fun moeGreetingText(nowMillis: Long): String {
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..23 -> "Good evening"
        else -> "Good night"
    }
}
