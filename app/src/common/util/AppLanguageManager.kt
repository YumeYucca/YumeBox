/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

@file:Suppress("AndroidLintObsoleteSdkInt")

package com.github.yumeyucca.yumebox.common.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.yumeyucca.yumebox.data.model.AppLanguage
import com.github.yumeyucca.yumebox.core.locale.YumeLocaleManager
import java.util.*

@SuppressLint("AppBundleLocaleChanges")
object AppLanguageManager {
    @Volatile
    private var activeLanguage: AppLanguage = AppLanguage.System

    @Volatile
    private var activeLocale: Locale = Locale.getDefault()

    fun apply(language: AppLanguage) {
        activeLanguage = language
        val locale = resolveLocale(language)
        activeLocale = locale

        AppCompatDelegate.setApplicationLocales(
            when (language) {
                AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.Zh -> LocaleListCompat.forLanguageTags("zh-Hans")
                AppLanguage.ZhHant -> LocaleListCompat.forLanguageTags("zh-Hant")
                AppLanguage.En -> LocaleListCompat.forLanguageTags("en")
                AppLanguage.Ja -> LocaleListCompat.forLanguageTags("ja")
                AppLanguage.Ru -> LocaleListCompat.forLanguageTags("ru")
            }
        )

        Locale.setDefault(locale)
        LocaleUtil.setCurrentLocale(locale)
        val lyricistTag = when (language) {
            AppLanguage.System ->
                if (locale.language.equals("zh", ignoreCase = true) &&
                    (locale.script.equals("Hant", ignoreCase = true) ||
                     locale.country.equals("TW", ignoreCase = true) ||
                     locale.country.equals("HK", ignoreCase = true) ||
                     locale.country.equals("MO", ignoreCase = true))
                ) {
                    "zh-tw"
                } else {
                    locale.toLanguageTag().lowercase()
                }
            AppLanguage.Zh -> "zh-cn"
            AppLanguage.ZhHant -> "zh-tw"
            AppLanguage.En -> "en"
            AppLanguage.Ja -> "ja"
            AppLanguage.Ru -> "ru"
        }
        YumeLocaleManager.updateLocale(lyricistTag)
    }

    fun wrap(base: Context): Context {
        val configuration = Configuration(base.resources.configuration)
        applyLocale(configuration, activeLocale)
        return base.createConfigurationContext(configuration)
    }

    fun refreshSystemLanguage() {
        if (activeLanguage == AppLanguage.System) {
            apply(AppLanguage.System)
        }
    }

    private fun resolveLocale(language: AppLanguage): Locale =
        when (language) {
            AppLanguage.System -> systemLocale()
            AppLanguage.Zh -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.ZhHant -> Locale.TRADITIONAL_CHINESE
            AppLanguage.En -> Locale.ENGLISH
            AppLanguage.Ja -> Locale.JAPANESE
            AppLanguage.Ru -> Locale.forLanguageTag("ru")
        }

    private fun systemLocale(): Locale {
        val resources = Resources.getSystem()
        return resources.configuration.locales[0] ?: Locale.getDefault()
    }

    private fun applyLocale(configuration: Configuration, locale: Locale) {
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
    }
}
