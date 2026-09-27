package com.github.yumeyucca.yumebox.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.lyricist.Lyricist
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings

public val LocalYumeStrings: ProvidableCompositionLocal<YumeStrings> =
    staticCompositionLocalOf { ZhHansStrings }

public val yumeStrings: Map<String, YumeStrings> = mapOf(
    "zh-cn" to ZhHansStrings,
    "zh-hans" to ZhHansStrings,
    "zh-sg" to ZhHansStrings,
    "zh" to ZhHansStrings,
    "zh-tw" to ZhStrings,
    "zh-hant" to ZhStrings,
    "zh-hk" to ZhStrings,
    "zh-mo" to ZhStrings,
    "en" to EnStrings,
    "ja" to JaStrings,
    "ru" to RuStrings,
)

public val currentYumeStrings: YumeStrings
    @Composable
    get() = LocalYumeStrings.current

@Composable
public fun rememberYumeStrings(
    defaultLanguageTag: String = "zh-cn",
    currentLanguageTag: String = java.util.Locale.getDefault().toLanguageTag().lowercase(),
): Lyricist<YumeStrings> =
    rememberStrings(yumeStrings, defaultLanguageTag, currentLanguageTag)

@Composable
public fun ProvideYumeStrings(
    lyricist: Lyricist<YumeStrings> = rememberYumeStrings(),
    content: @Composable () -> Unit,
) {
    ProvideStrings(lyricist, LocalYumeStrings, content)
}

public object YumeLocaleManager {
    @Volatile
    public var activeStrings: YumeStrings = ZhHansStrings
        internal set

    public fun updateLocale(tag: String) {
        val lower = tag.lowercase()
        val resolved = yumeStrings[lower]
            ?: yumeStrings[lower.split("-")[0]]
            ?: ZhHansStrings
        activeStrings = resolved
    }
}

public object YumeTxt {
    public object About {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.About.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Title, *args)
        public object App {
            public val Description: String
                get() = YumeLocaleManager.activeStrings.About.App.Description
            @androidx.compose.runtime.Composable
            public fun Description(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.App.Description else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.App.Description, *args)
            public val VersionLoading: String
                get() = YumeLocaleManager.activeStrings.About.App.VersionLoading
            @androidx.compose.runtime.Composable
            public fun VersionLoading(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.App.VersionLoading else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.App.VersionLoading, *args)
            public val VersionFailed: String
                get() = YumeLocaleManager.activeStrings.About.App.VersionFailed
            @androidx.compose.runtime.Composable
            public fun VersionFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.App.VersionFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.App.VersionFailed, *args)
        }
        public object Debug {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.About.Debug.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Debug.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Debug.Title, *args)
            public val TestInitialization: String
                get() = YumeLocaleManager.activeStrings.About.Debug.TestInitialization
            @androidx.compose.runtime.Composable
            public fun TestInitialization(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Debug.TestInitialization else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Debug.TestInitialization, *args)
            public val InitializationReset: String
                get() = YumeLocaleManager.activeStrings.About.Debug.InitializationReset
            @androidx.compose.runtime.Composable
            public fun InitializationReset(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Debug.InitializationReset else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Debug.InitializationReset, *args)
        }
        public object Section {
            public val ProjectLinks: String
                get() = YumeLocaleManager.activeStrings.About.Section.ProjectLinks
            @androidx.compose.runtime.Composable
            public fun ProjectLinks(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Section.ProjectLinks else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Section.ProjectLinks, *args)
            public val More: String
                get() = YumeLocaleManager.activeStrings.About.Section.More
            @androidx.compose.runtime.Composable
            public fun More(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Section.More else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Section.More, *args)
            public val Support: String
                get() = YumeLocaleManager.activeStrings.About.Section.Support
            @androidx.compose.runtime.Composable
            public fun Support(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Section.Support else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Section.Support, *args)
            public val License: String
                get() = YumeLocaleManager.activeStrings.About.Section.License
            @androidx.compose.runtime.Composable
            public fun License(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Section.License else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Section.License, *args)
        }
        public object Link {
            public val TelegramGroup: String
                get() = YumeLocaleManager.activeStrings.About.Link.TelegramGroup
            @androidx.compose.runtime.Composable
            public fun TelegramGroup(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Link.TelegramGroup else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Link.TelegramGroup, *args)
            public val TelegramChannel: String
                get() = YumeLocaleManager.activeStrings.About.Link.TelegramChannel
            @androidx.compose.runtime.Composable
            public fun TelegramChannel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Link.TelegramChannel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Link.TelegramChannel, *args)
        }
        public object Support {
            public val ExportLogs: String
                get() = YumeLocaleManager.activeStrings.About.Support.ExportLogs
            @androidx.compose.runtime.Composable
            public fun ExportLogs(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Support.ExportLogs else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Support.ExportLogs, *args)
            public val ReportIssue: String
                get() = YumeLocaleManager.activeStrings.About.Support.ReportIssue
            @androidx.compose.runtime.Composable
            public fun ReportIssue(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Support.ReportIssue else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Support.ReportIssue, *args)
            public val ExportSuccess: String
                get() = YumeLocaleManager.activeStrings.About.Support.ExportSuccess
            @androidx.compose.runtime.Composable
            public fun ExportSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Support.ExportSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Support.ExportSuccess, *args)
            public val ExportFailed: String
                get() = YumeLocaleManager.activeStrings.About.Support.ExportFailed
            @androidx.compose.runtime.Composable
            public fun ExportFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Support.ExportFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Support.ExportFailed, *args)
        }
        public object License {
            public val Libraries: String
                get() = YumeLocaleManager.activeStrings.About.License.Libraries
            @androidx.compose.runtime.Composable
            public fun Libraries(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.License.Libraries else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.License.Libraries, *args)
            public val LibrariesSummary: String
                get() = YumeLocaleManager.activeStrings.About.License.LibrariesSummary
            @androidx.compose.runtime.Composable
            public fun LibrariesSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.License.LibrariesSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.License.LibrariesSummary, *args)
            public val AgplName: String
                get() = YumeLocaleManager.activeStrings.About.License.AgplName
            @androidx.compose.runtime.Composable
            public fun AgplName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.License.AgplName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.License.AgplName, *args)
            public val AgplDescription: String
                get() = YumeLocaleManager.activeStrings.About.License.AgplDescription
            @androidx.compose.runtime.Composable
            public fun AgplDescription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.About.License.AgplDescription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.License.AgplDescription, *args)
        }
        public val Copyright: String
            get() = YumeLocaleManager.activeStrings.About.Copyright
        @androidx.compose.runtime.Composable
        public fun Copyright(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.About.Copyright else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.About.Copyright, *args)
    }
    public object AccessControl {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.AccessControl.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Title, *args)
        public object Search {
            public val Placeholder: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Search.Placeholder
            @androidx.compose.runtime.Composable
            public fun Placeholder(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Search.Placeholder else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Search.Placeholder, *args)
            public val Empty: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Search.Empty
            @androidx.compose.runtime.Composable
            public fun Empty(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Search.Empty else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Search.Empty, *args)
        }
        public object AppList {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.AccessControl.AppList.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.AppList.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.AppList.Title, *args)
            public val Loading: String
                get() = YumeLocaleManager.activeStrings.AccessControl.AppList.Loading
            @androidx.compose.runtime.Composable
            public fun Loading(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.AppList.Loading else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.AppList.Loading, *args)
        }
        public object Settings {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.Title, *args)
            public val ShowSystemApps: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.ShowSystemApps
            @androidx.compose.runtime.Composable
            public fun ShowSystemApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.ShowSystemApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.ShowSystemApps, *args)
            public val SelectedFirst: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.SelectedFirst
            @androidx.compose.runtime.Composable
            public fun SelectedFirst(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.SelectedFirst else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.SelectedFirst, *args)
            public val SortMode: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.SortMode
            @androidx.compose.runtime.Composable
            public fun SortMode(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.SortMode else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.SortMode, *args)
            public val SortModeCurrent: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.SortModeCurrent
            @androidx.compose.runtime.Composable
            public fun SortModeCurrent(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.SortModeCurrent else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.SortModeCurrent, *args)
            public val BatchOperation: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.BatchOperation
            @androidx.compose.runtime.Composable
            public fun BatchOperation(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.BatchOperation else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.BatchOperation, *args)
            public val SelectAll: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.SelectAll
            @androidx.compose.runtime.Composable
            public fun SelectAll(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.SelectAll else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.SelectAll, *args)
            public val DeselectAll: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.DeselectAll
            @androidx.compose.runtime.Composable
            public fun DeselectAll(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.DeselectAll else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.DeselectAll, *args)
            public val Invert: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.Invert
            @androidx.compose.runtime.Composable
            public fun Invert(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.Invert else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.Invert, *args)
            public val ImportExport: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.ImportExport
            @androidx.compose.runtime.Composable
            public fun ImportExport(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.ImportExport else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.ImportExport, *args)
            public val Import: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.Import
            @androidx.compose.runtime.Composable
            public fun Import(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.Import else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.Import, *args)
            public val Export: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.Export
            @androidx.compose.runtime.Composable
            public fun Export(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.Export else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.Export, *args)
            public val ImportSuccess: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.ImportSuccess
            @androidx.compose.runtime.Composable
            public fun ImportSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.ImportSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.ImportSuccess, *args)
            public val ExportSuccess: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.ExportSuccess
            @androidx.compose.runtime.Composable
            public fun ExportSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.ExportSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.ExportSuccess, *args)
            public val ImportFailed: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.ImportFailed
            @androidx.compose.runtime.Composable
            public fun ImportFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.ImportFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.ImportFailed, *args)
            public val RegionQuickSelect: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.RegionQuickSelect
            @androidx.compose.runtime.Composable
            public fun RegionQuickSelect(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.RegionQuickSelect else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.RegionQuickSelect, *args)
            public val ChinaApps: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.ChinaApps
            @androidx.compose.runtime.Composable
            public fun ChinaApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.ChinaApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.ChinaApps, *args)
            public val OverseasApps: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.OverseasApps
            @androidx.compose.runtime.Composable
            public fun OverseasApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.OverseasApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.OverseasApps, *args)
            public val RegionSelectResult: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Settings.RegionSelectResult
            @androidx.compose.runtime.Composable
            public fun RegionSelectResult(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Settings.RegionSelectResult else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Settings.RegionSelectResult, *args)
        }
        public object SortMode {
            public val PackageName: String
                get() = YumeLocaleManager.activeStrings.AccessControl.SortMode.PackageName
            @androidx.compose.runtime.Composable
            public fun PackageName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.SortMode.PackageName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.SortMode.PackageName, *args)
            public val Label: String
                get() = YumeLocaleManager.activeStrings.AccessControl.SortMode.Label
            @androidx.compose.runtime.Composable
            public fun Label(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.SortMode.Label else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.SortMode.Label, *args)
            public val InstallTime: String
                get() = YumeLocaleManager.activeStrings.AccessControl.SortMode.InstallTime
            @androidx.compose.runtime.Composable
            public fun InstallTime(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.SortMode.InstallTime else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.SortMode.InstallTime, *args)
            public val UpdateTime: String
                get() = YumeLocaleManager.activeStrings.AccessControl.SortMode.UpdateTime
            @androidx.compose.runtime.Composable
            public fun UpdateTime(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.SortMode.UpdateTime else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.SortMode.UpdateTime, *args)
        }
        public object Button {
            public val Cancel: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Button.Cancel
            @androidx.compose.runtime.Composable
            public fun Cancel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Button.Cancel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Button.Cancel, *args)
            public val Confirm: String
                get() = YumeLocaleManager.activeStrings.AccessControl.Button.Confirm
            @androidx.compose.runtime.Composable
            public fun Confirm(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AccessControl.Button.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AccessControl.Button.Confirm, *args)
        }
    }
    public object AppSettings {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.AppSettings.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Title, *args)
        public object Section {
            public val Behavior: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Behavior
            @androidx.compose.runtime.Composable
            public fun Behavior(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Behavior else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Behavior, *args)
            public val Interface: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Interface
            @androidx.compose.runtime.Composable
            public fun Interface(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Interface else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Interface, *args)
            public val Privacy: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Privacy
            @androidx.compose.runtime.Composable
            public fun Privacy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Privacy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Privacy, *args)
            public val Service: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Service
            @androidx.compose.runtime.Composable
            public fun Service(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Service else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Service, *args)
            public val Network: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Network
            @androidx.compose.runtime.Composable
            public fun Network(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Network else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Network, *args)
            public val Navigation: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Navigation
            @androidx.compose.runtime.Composable
            public fun Navigation(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Navigation else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Navigation, *args)
            public val Home: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Section.Home
            @androidx.compose.runtime.Composable
            public fun Home(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Section.Home else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Section.Home, *args)
        }
        public object Behavior {
            public val AutoStartTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Behavior.AutoStartTitle
            @androidx.compose.runtime.Composable
            public fun AutoStartTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Behavior.AutoStartTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Behavior.AutoStartTitle, *args)
            public val AutoUpdateOnStartTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Behavior.AutoUpdateOnStartTitle
            @androidx.compose.runtime.Composable
            public fun AutoUpdateOnStartTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Behavior.AutoUpdateOnStartTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Behavior.AutoUpdateOnStartTitle, *args)
        }
        public object Interface {
            public val ThemeModeTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeTitle
            @androidx.compose.runtime.Composable
            public fun ThemeModeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeTitle, *args)
            public val ThemeModeSummary: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeSummary
            @androidx.compose.runtime.Composable
            public fun ThemeModeSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeSummary, *args)
            public val ThemeModeSystem: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeSystem
            @androidx.compose.runtime.Composable
            public fun ThemeModeSystem(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeSystem else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeSystem, *args)
            public val ThemeModeLight: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeLight
            @androidx.compose.runtime.Composable
            public fun ThemeModeLight(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeLight else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeLight, *args)
            public val ThemeModeDark: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeDark
            @androidx.compose.runtime.Composable
            public fun ThemeModeDark(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeDark else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeModeDark, *args)
            public val LanguageTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageTitle
            @androidx.compose.runtime.Composable
            public fun LanguageTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageTitle, *args)
            public val LanguageSystem: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageSystem
            @androidx.compose.runtime.Composable
            public fun LanguageSystem(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageSystem else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageSystem, *args)
            public val LanguageChinese: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageChinese
            @androidx.compose.runtime.Composable
            public fun LanguageChinese(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageChinese else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageChinese, *args)
            public val LanguageChineseTraditional: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageChineseTraditional
            @androidx.compose.runtime.Composable
            public fun LanguageChineseTraditional(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageChineseTraditional else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageChineseTraditional, *args)
            public val LanguageEnglish: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageEnglish
            @androidx.compose.runtime.Composable
            public fun LanguageEnglish(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageEnglish else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageEnglish, *args)
            public val LanguageJapanese: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageJapanese
            @androidx.compose.runtime.Composable
            public fun LanguageJapanese(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageJapanese else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageJapanese, *args)
            public val LanguageRussian: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageRussian
            @androidx.compose.runtime.Composable
            public fun LanguageRussian(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageRussian else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.LanguageRussian, *args)
            public val ColorThemeTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeTitle
            @androidx.compose.runtime.Composable
            public fun ColorThemeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeTitle, *args)
            public val ColorThemePickerTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemePickerTitle
            @androidx.compose.runtime.Composable
            public fun ColorThemePickerTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemePickerTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemePickerTitle, *args)
            public val ColorThemeCodeLabel: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeCodeLabel
            @androidx.compose.runtime.Composable
            public fun ColorThemeCodeLabel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeCodeLabel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeCodeLabel, *args)
            public val ColorThemeCustomSummary: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeCustomSummary
            @androidx.compose.runtime.Composable
            public fun ColorThemeCustomSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeCustomSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ColorThemeCustomSummary, *args)
            public val ThemeColorPolarityInvertTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeColorPolarityInvertTitle
            @androidx.compose.runtime.Composable
            public fun ThemeColorPolarityInvertTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeColorPolarityInvertTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ThemeColorPolarityInvertTitle, *args)
            public val TopBarBlurTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.TopBarBlurTitle
            @androidx.compose.runtime.Composable
            public fun TopBarBlurTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.TopBarBlurTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.TopBarBlurTitle, *args)
            public val AutoHideNavbarTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.AutoHideNavbarTitle
            @androidx.compose.runtime.Composable
            public fun AutoHideNavbarTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.AutoHideNavbarTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.AutoHideNavbarTitle, *args)
            public val PageScaleTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.PageScaleTitle
            @androidx.compose.runtime.Composable
            public fun PageScaleTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.PageScaleTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.PageScaleTitle, *args)
            public val PageScaleDialogSummary: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.PageScaleDialogSummary
            @androidx.compose.runtime.Composable
            public fun PageScaleDialogSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.PageScaleDialogSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.PageScaleDialogSummary, *args)
            public val PredictiveBackTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackTitle
            @androidx.compose.runtime.Composable
            public fun PredictiveBackTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackTitle, *args)
            public val PredictiveBackRestartSummary: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackRestartSummary
            @androidx.compose.runtime.Composable
            public fun PredictiveBackRestartSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackRestartSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackRestartSummary, *args)
            public val PredictiveBackProgressTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackProgressTitle
            @androidx.compose.runtime.Composable
            public fun PredictiveBackProgressTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackProgressTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.PredictiveBackProgressTitle, *args)
            public val ClassicHomeTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.ClassicHomeTitle
            @androidx.compose.runtime.Composable
            public fun ClassicHomeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.ClassicHomeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.ClassicHomeTitle, *args)
            public val SystemWallpaperTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.SystemWallpaperTitle
            @androidx.compose.runtime.Composable
            public fun SystemWallpaperTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.SystemWallpaperTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.SystemWallpaperTitle, *args)
            public val CustomIconTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.CustomIconTitle
            @androidx.compose.runtime.Composable
            public fun CustomIconTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.CustomIconTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.CustomIconTitle, *args)
            public val HomeQuoteTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.HomeQuoteTitle
            @androidx.compose.runtime.Composable
            public fun HomeQuoteTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.HomeQuoteTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.HomeQuoteTitle, *args)
            public val HomeQuoteDefault: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.HomeQuoteDefault
            @androidx.compose.runtime.Composable
            public fun HomeQuoteDefault(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.HomeQuoteDefault else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.HomeQuoteDefault, *args)
            public val EditHomeQuoteTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.EditHomeQuoteTitle
            @androidx.compose.runtime.Composable
            public fun EditHomeQuoteTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.EditHomeQuoteTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.EditHomeQuoteTitle, *args)
            public val HomeWallpaperImportFailed: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Interface.HomeWallpaperImportFailed
            @androidx.compose.runtime.Composable
            public fun HomeWallpaperImportFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Interface.HomeWallpaperImportFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Interface.HomeWallpaperImportFailed, *args)
        }
        public object Privacy {
            public val HideFromRecentsTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Privacy.HideFromRecentsTitle
            @androidx.compose.runtime.Composable
            public fun HideFromRecentsTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Privacy.HideFromRecentsTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Privacy.HideFromRecentsTitle, *args)
        }
        public object ServiceSection {
            public val TrafficNotificationTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.TrafficNotificationTitle
            @androidx.compose.runtime.Composable
            public fun TrafficNotificationTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.TrafficNotificationTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.TrafficNotificationTitle, *args)
            public val ExitUiWhenBackgroundTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle
            @androidx.compose.runtime.Composable
            public fun ExitUiWhenBackgroundTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle, *args)
            public val SuperIslandTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.SuperIslandTitle
            @androidx.compose.runtime.Composable
            public fun SuperIslandTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.SuperIslandTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.SuperIslandTitle, *args)
            public val ShizukuTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuTitle
            @androidx.compose.runtime.Composable
            public fun ShizukuTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuTitle, *args)
            public val ShizukuNotRunning: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuNotRunning
            @androidx.compose.runtime.Composable
            public fun ShizukuNotRunning(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuNotRunning else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuNotRunning, *args)
            public val ShizukuPermissionRequired: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuPermissionRequired
            @androidx.compose.runtime.Composable
            public fun ShizukuPermissionRequired(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuPermissionRequired else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuPermissionRequired, *args)
            public val ShizukuReady: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuReady
            @androidx.compose.runtime.Composable
            public fun ShizukuReady(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuReady else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuReady, *args)
            public val ShizukuStatusUnavailable: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusUnavailable
            @androidx.compose.runtime.Composable
            public fun ShizukuStatusUnavailable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusUnavailable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusUnavailable, *args)
            public val ShizukuStatusPending: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusPending
            @androidx.compose.runtime.Composable
            public fun ShizukuStatusPending(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusPending else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusPending, *args)
            public val ShizukuStatusGranted: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusGranted
            @androidx.compose.runtime.Composable
            public fun ShizukuStatusGranted(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusGranted else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.ShizukuStatusGranted, *args)
            public val BatteryOptimizationTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.BatteryOptimizationTitle
            @androidx.compose.runtime.Composable
            public fun BatteryOptimizationTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.BatteryOptimizationTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.BatteryOptimizationTitle, *args)
            public val BatteryOptimizationAlreadyDisabled: String
                get() = YumeLocaleManager.activeStrings.AppSettings.ServiceSection.BatteryOptimizationAlreadyDisabled
            @androidx.compose.runtime.Composable
            public fun BatteryOptimizationAlreadyDisabled(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.ServiceSection.BatteryOptimizationAlreadyDisabled else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.ServiceSection.BatteryOptimizationAlreadyDisabled, *args)
        }
        public object Network {
            public val CustomUserAgentTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Network.CustomUserAgentTitle
            @androidx.compose.runtime.Composable
            public fun CustomUserAgentTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Network.CustomUserAgentTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Network.CustomUserAgentTitle, *args)
            public val CustomUserAgentSummaryDefault: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Network.CustomUserAgentSummaryDefault
            @androidx.compose.runtime.Composable
            public fun CustomUserAgentSummaryDefault(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Network.CustomUserAgentSummaryDefault else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Network.CustomUserAgentSummaryDefault, *args)
        }
        public object WarningDialog {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.AppSettings.WarningDialog.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.WarningDialog.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.WarningDialog.Title, *args)
        }
        public object EditDialog {
            public val UserAgentTitle: String
                get() = YumeLocaleManager.activeStrings.AppSettings.EditDialog.UserAgentTitle
            @androidx.compose.runtime.Composable
            public fun UserAgentTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.EditDialog.UserAgentTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.EditDialog.UserAgentTitle, *args)
        }
        public object Button {
            public val Apply: String
                get() = YumeLocaleManager.activeStrings.AppSettings.Button.Apply
            @androidx.compose.runtime.Composable
            public fun Apply(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.AppSettings.Button.Apply else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.AppSettings.Button.Apply, *args)
        }
    }
    public object Component {
        public object ProfileCard {
            public val Update: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.Update
            @androidx.compose.runtime.Composable
            public fun Update(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.Update else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.Update, *args)
            public val Export: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.Export
            @androidx.compose.runtime.Composable
            public fun Export(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.Export else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.Export, *args)
            public val Edit: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.Edit
            @androidx.compose.runtime.Composable
            public fun Edit(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.Edit else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.Edit, *args)
            public val Delete: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.Delete
            @androidx.compose.runtime.Composable
            public fun Delete(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.Delete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.Delete, *args)
            public val RemoteSubscription: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.RemoteSubscription
            @androidx.compose.runtime.Composable
            public fun RemoteSubscription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.RemoteSubscription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.RemoteSubscription, *args)
            public val LocalFile: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.LocalFile
            @androidx.compose.runtime.Composable
            public fun LocalFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.LocalFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.LocalFile, *args)
            public val LocalConfig: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.LocalConfig
            @androidx.compose.runtime.Composable
            public fun LocalConfig(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.LocalConfig else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.LocalConfig, *args)
            public val ClickToUpdate: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.ClickToUpdate
            @androidx.compose.runtime.Composable
            public fun ClickToUpdate(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.ClickToUpdate else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.ClickToUpdate, *args)
            public val Traffic: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.Traffic
            @androidx.compose.runtime.Composable
            public fun Traffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.Traffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.Traffic, *args)
            public val UsedTraffic: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.UsedTraffic
            @androidx.compose.runtime.Composable
            public fun UsedTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.UsedTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.UsedTraffic, *args)
            public val ExpireAt: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.ExpireAt
            @androidx.compose.runtime.Composable
            public fun ExpireAt(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.ExpireAt else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.ExpireAt, *args)
            public val ExpireToday: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.ExpireToday
            @androidx.compose.runtime.Composable
            public fun ExpireToday(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.ExpireToday else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.ExpireToday, *args)
            public val Expired: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.Expired
            @androidx.compose.runtime.Composable
            public fun Expired(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.Expired else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.Expired, *args)
            public val JustNow: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.JustNow
            @androidx.compose.runtime.Composable
            public fun JustNow(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.JustNow else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.JustNow, *args)
            public val MinutesAgo: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.MinutesAgo
            @androidx.compose.runtime.Composable
            public fun MinutesAgo(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.MinutesAgo else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.MinutesAgo, *args)
            public val HoursAgo: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.HoursAgo
            @androidx.compose.runtime.Composable
            public fun HoursAgo(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.HoursAgo else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.HoursAgo, *args)
            public val DaysAgo: String
                get() = YumeLocaleManager.activeStrings.Component.ProfileCard.DaysAgo
            @androidx.compose.runtime.Composable
            public fun DaysAgo(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ProfileCard.DaysAgo else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ProfileCard.DaysAgo, *args)
        }
        public object WebView {
            public val InvalidUrl: String
                get() = YumeLocaleManager.activeStrings.Component.WebView.InvalidUrl
            @androidx.compose.runtime.Composable
            public fun InvalidUrl(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.WebView.InvalidUrl else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.WebView.InvalidUrl, *args)
        }
        public object Selector {
            public val NotModify: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.NotModify
            @androidx.compose.runtime.Composable
            public fun NotModify(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.NotModify else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.NotModify, *args)
            public val Enable: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.Enable
            @androidx.compose.runtime.Composable
            public fun Enable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.Enable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.Enable, *args)
            public val Disable: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.Disable
            @androidx.compose.runtime.Composable
            public fun Disable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.Disable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.Disable, *args)
            public val Replace: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.Replace
            @androidx.compose.runtime.Composable
            public fun Replace(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.Replace else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.Replace, *args)
            public val Prepend: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.Prepend
            @androidx.compose.runtime.Composable
            public fun Prepend(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.Prepend else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.Prepend, *args)
            public val Append: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.Append
            @androidx.compose.runtime.Composable
            public fun Append(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.Append else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.Append, *args)
            public val Merge: String
                get() = YumeLocaleManager.activeStrings.Component.Selector.Merge
            @androidx.compose.runtime.Composable
            public fun Merge(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Selector.Merge else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Selector.Merge, *args)
        }
        public object Navigation {
            public val Back: String
                get() = YumeLocaleManager.activeStrings.Component.Navigation.Back
            @androidx.compose.runtime.Composable
            public fun Back(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Navigation.Back else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Navigation.Back, *args)
        }
        public object Message {
            public val Confirm: String
                get() = YumeLocaleManager.activeStrings.Component.Message.Confirm
            @androidx.compose.runtime.Composable
            public fun Confirm(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Message.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Message.Confirm, *args)
            public val Hint: String
                get() = YumeLocaleManager.activeStrings.Component.Message.Hint
            @androidx.compose.runtime.Composable
            public fun Hint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Message.Hint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Message.Hint, *args)
            public val Error: String
                get() = YumeLocaleManager.activeStrings.Component.Message.Error
            @androidx.compose.runtime.Composable
            public fun Error(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Message.Error else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Message.Error, *args)
            public val Success: String
                get() = YumeLocaleManager.activeStrings.Component.Message.Success
            @androidx.compose.runtime.Composable
            public fun Success(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Message.Success else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Message.Success, *args)
        }
        public object Button {
            public val Cancel: String
                get() = YumeLocaleManager.activeStrings.Component.Button.Cancel
            @androidx.compose.runtime.Composable
            public fun Cancel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Button.Cancel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Button.Cancel, *args)
            public val Confirm: String
                get() = YumeLocaleManager.activeStrings.Component.Button.Confirm
            @androidx.compose.runtime.Composable
            public fun Confirm(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Button.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Button.Confirm, *args)
            public val Clear: String
                get() = YumeLocaleManager.activeStrings.Component.Button.Clear
            @androidx.compose.runtime.Composable
            public fun Clear(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Button.Clear else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Button.Clear, *args)
            public val Copy: String
                get() = YumeLocaleManager.activeStrings.Component.Button.Copy
            @androidx.compose.runtime.Composable
            public fun Copy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Button.Copy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Button.Copy, *args)
        }
        public object Flag {
            public val ContentDescription: String
                get() = YumeLocaleManager.activeStrings.Component.Flag.ContentDescription
            @androidx.compose.runtime.Composable
            public fun ContentDescription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Flag.ContentDescription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Flag.ContentDescription, *args)
        }
        public object Loading {
            public val Starting: String
                get() = YumeLocaleManager.activeStrings.Component.Loading.Starting
            @androidx.compose.runtime.Composable
            public fun Starting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Loading.Starting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Loading.Starting, *args)
        }
        public object ConfigInput {
            public val PortLabel: String
                get() = YumeLocaleManager.activeStrings.Component.ConfigInput.PortLabel
            @androidx.compose.runtime.Composable
            public fun PortLabel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ConfigInput.PortLabel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ConfigInput.PortLabel, *args)
            public val CountItems: String
                get() = YumeLocaleManager.activeStrings.Component.ConfigInput.CountItems
            @androidx.compose.runtime.Composable
            public fun CountItems(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.ConfigInput.CountItems else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.ConfigInput.CountItems, *args)
        }
        public object BottomBar {
            public val Home: String
                get() = YumeLocaleManager.activeStrings.Component.BottomBar.Home
            @androidx.compose.runtime.Composable
            public fun Home(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.BottomBar.Home else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.BottomBar.Home, *args)
            public val Proxy: String
                get() = YumeLocaleManager.activeStrings.Component.BottomBar.Proxy
            @androidx.compose.runtime.Composable
            public fun Proxy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.BottomBar.Proxy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.BottomBar.Proxy, *args)
            public val Config: String
                get() = YumeLocaleManager.activeStrings.Component.BottomBar.Config
            @androidx.compose.runtime.Composable
            public fun Config(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.BottomBar.Config else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.BottomBar.Config, *args)
            public val Setting: String
                get() = YumeLocaleManager.activeStrings.Component.BottomBar.Setting
            @androidx.compose.runtime.Composable
            public fun Setting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.BottomBar.Setting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.BottomBar.Setting, *args)
        }
        public object Editor {
            public val CountItems: String
                get() = YumeLocaleManager.activeStrings.Component.Editor.CountItems
            @androidx.compose.runtime.Composable
            public fun CountItems(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.CountItems else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.CountItems, *args)
            public object Action {
                public val Search: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Action.Search
                @androidx.compose.runtime.Composable
                public fun Search(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Action.Search else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Action.Search, *args)
            }
            public object Dialog {
                public val AddTitle: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Dialog.AddTitle
                @androidx.compose.runtime.Composable
                public fun AddTitle(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Dialog.AddTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Dialog.AddTitle, *args)
                public val EditTitle: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Dialog.EditTitle
                @androidx.compose.runtime.Composable
                public fun EditTitle(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Dialog.EditTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Dialog.EditTitle, *args)
                public val ResetTitle: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Dialog.ResetTitle
                @androidx.compose.runtime.Composable
                public fun ResetTitle(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Dialog.ResetTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Dialog.ResetTitle, *args)
                public val ResetMessage: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Dialog.ResetMessage
                @androidx.compose.runtime.Composable
                public fun ResetMessage(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Dialog.ResetMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Dialog.ResetMessage, *args)
            }
            public object Empty {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Empty.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Empty.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Empty.Title, *args)
                public val Hint: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Empty.Hint
                @androidx.compose.runtime.Composable
                public fun Hint(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Empty.Hint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Empty.Hint, *args)
            }
            public object Error {
                public val KeyEmpty: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Error.KeyEmpty
                @androidx.compose.runtime.Composable
                public fun KeyEmpty(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Error.KeyEmpty else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Error.KeyEmpty, *args)
                public val KeyExists: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Error.KeyExists
                @androidx.compose.runtime.Composable
                public fun KeyExists(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Error.KeyExists else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Error.KeyExists, *args)
            }
            public object Rule {
                public val Type: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.Type
                @androidx.compose.runtime.Composable
                public fun Type(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.Type else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.Type, *args)
                public val Target: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.Target
                @androidx.compose.runtime.Composable
                public fun Target(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.Target else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.Target, *args)
                public val Content: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.Content
                @androidx.compose.runtime.Composable
                public fun Content(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.Content else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.Content, *args)
                public val Src: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.Src
                @androidx.compose.runtime.Composable
                public fun Src(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.Src else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.Src, *args)
                public val NoResolve: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.NoResolve
                @androidx.compose.runtime.Composable
                public fun NoResolve(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.NoResolve else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.NoResolve, *args)
                public val TargetReject: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetReject
                @androidx.compose.runtime.Composable
                public fun TargetReject(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetReject else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetReject, *args)
                public val TargetDirect: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetDirect
                @androidx.compose.runtime.Composable
                public fun TargetDirect(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetDirect else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetDirect, *args)
                public val TargetMatch: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetMatch
                @androidx.compose.runtime.Composable
                public fun TargetMatch(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetMatch else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.TargetMatch, *args)
                public val ErrorContentRequired: String
                    get() = YumeLocaleManager.activeStrings.Component.Editor.Rule.ErrorContentRequired
                @androidx.compose.runtime.Composable
                public fun ErrorContentRequired(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Component.Editor.Rule.ErrorContentRequired else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Component.Editor.Rule.ErrorContentRequired, *args)
            }
        }
    }
    public object Connection {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Connection.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Title, *args)
        public val Summary: String
            get() = YumeLocaleManager.activeStrings.Connection.Summary
        @androidx.compose.runtime.Composable
        public fun Summary(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Summary, *args)
        public object Tab {
            public val Active: String
                get() = YumeLocaleManager.activeStrings.Connection.Tab.Active
            @androidx.compose.runtime.Composable
            public fun Active(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Tab.Active else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Tab.Active, *args)
            public val Closed: String
                get() = YumeLocaleManager.activeStrings.Connection.Tab.Closed
            @androidx.compose.runtime.Composable
            public fun Closed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Tab.Closed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Tab.Closed, *args)
        }
        public val Search: String
            get() = YumeLocaleManager.activeStrings.Connection.Search
        @androidx.compose.runtime.Composable
        public fun Search(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Search else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Search, *args)
        public val SearchHint: String
            get() = YumeLocaleManager.activeStrings.Connection.SearchHint
        @androidx.compose.runtime.Composable
        public fun SearchHint(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.SearchHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.SearchHint, *args)
        public val SortBy: String
            get() = YumeLocaleManager.activeStrings.Connection.SortBy
        @androidx.compose.runtime.Composable
        public fun SortBy(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.SortBy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.SortBy, *args)
        public object Sort {
            public val Time: String
                get() = YumeLocaleManager.activeStrings.Connection.Sort.Time
            @androidx.compose.runtime.Composable
            public fun Time(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Sort.Time else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Sort.Time, *args)
            public val Upload: String
                get() = YumeLocaleManager.activeStrings.Connection.Sort.Upload
            @androidx.compose.runtime.Composable
            public fun Upload(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Sort.Upload else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Sort.Upload, *args)
            public val Download: String
                get() = YumeLocaleManager.activeStrings.Connection.Sort.Download
            @androidx.compose.runtime.Composable
            public fun Download(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Sort.Download else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Sort.Download, *args)
            public val Host: String
                get() = YumeLocaleManager.activeStrings.Connection.Sort.Host
            @androidx.compose.runtime.Composable
            public fun Host(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Sort.Host else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Sort.Host, *args)
        }
        public val ChainCount: String
            get() = YumeLocaleManager.activeStrings.Connection.ChainCount
        @androidx.compose.runtime.Composable
        public fun ChainCount(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.ChainCount else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.ChainCount, *args)
        public object RelativeTime {
            public val JustNow: String
                get() = YumeLocaleManager.activeStrings.Connection.RelativeTime.JustNow
            @androidx.compose.runtime.Composable
            public fun JustNow(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.RelativeTime.JustNow else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.RelativeTime.JustNow, *args)
            public val MinutesAgo: String
                get() = YumeLocaleManager.activeStrings.Connection.RelativeTime.MinutesAgo
            @androidx.compose.runtime.Composable
            public fun MinutesAgo(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.RelativeTime.MinutesAgo else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.RelativeTime.MinutesAgo, *args)
            public val HoursAgo: String
                get() = YumeLocaleManager.activeStrings.Connection.RelativeTime.HoursAgo
            @androidx.compose.runtime.Composable
            public fun HoursAgo(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.RelativeTime.HoursAgo else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.RelativeTime.HoursAgo, *args)
            public val DaysAgo: String
                get() = YumeLocaleManager.activeStrings.Connection.RelativeTime.DaysAgo
            @androidx.compose.runtime.Composable
            public fun DaysAgo(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.RelativeTime.DaysAgo else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.RelativeTime.DaysAgo, *args)
            public val Date: String
                get() = YumeLocaleManager.activeStrings.Connection.RelativeTime.Date
            @androidx.compose.runtime.Composable
            public fun Date(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.RelativeTime.Date else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.RelativeTime.Date, *args)
        }
        public object Detail {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Connection.Detail.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Title, *args)
            public object Action {
                public val Interrupting: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Action.Interrupting
                @androidx.compose.runtime.Composable
                public fun Interrupting(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Action.Interrupting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Action.Interrupting, *args)
                public val Interrupt: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Action.Interrupt
                @androidx.compose.runtime.Composable
                public fun Interrupt(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Action.Interrupt else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Action.Interrupt, *args)
            }
            public object Section {
                public val Info: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Section.Info
                @androidx.compose.runtime.Composable
                public fun Info(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Section.Info else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Section.Info, *args)
                public val Rule: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Section.Rule
                @androidx.compose.runtime.Composable
                public fun Rule(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Section.Rule else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Section.Rule, *args)
                public val Chain: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Section.Chain
                @androidx.compose.runtime.Composable
                public fun Chain(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Section.Chain else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Section.Chain, *args)
            }
            public object Label {
                public val Host: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Host
                @androidx.compose.runtime.Composable
                public fun Host(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Host else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Host, *args)
                public val Protocol: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Protocol
                @androidx.compose.runtime.Composable
                public fun Protocol(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Protocol else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Protocol, *args)
                public val Process: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Process
                @androidx.compose.runtime.Composable
                public fun Process(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Process else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Process, *args)
                public val SourceAddress: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.SourceAddress
                @androidx.compose.runtime.Composable
                public fun SourceAddress(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.SourceAddress else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.SourceAddress, *args)
                public val DestinationAddress: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.DestinationAddress
                @androidx.compose.runtime.Composable
                public fun DestinationAddress(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.DestinationAddress else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.DestinationAddress, *args)
                public val Duration: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Duration
                @androidx.compose.runtime.Composable
                public fun Duration(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Duration else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Duration, *args)
                public val Upload: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Upload
                @androidx.compose.runtime.Composable
                public fun Upload(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Upload else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Upload, *args)
                public val Download: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Download
                @androidx.compose.runtime.Composable
                public fun Download(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Download else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Download, *args)
                public val Type: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Type
                @androidx.compose.runtime.Composable
                public fun Type(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Type else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Type, *args)
                public val Content: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Content
                @androidx.compose.runtime.Composable
                public fun Content(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Content else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Content, *args)
                public val Chain: String
                    get() = YumeLocaleManager.activeStrings.Connection.Detail.Label.Chain
                @androidx.compose.runtime.Composable
                public fun Chain(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Detail.Label.Chain else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Detail.Label.Chain, *args)
            }
        }
        public val Loading: String
            get() = YumeLocaleManager.activeStrings.Connection.Loading
        @androidx.compose.runtime.Composable
        public fun Loading(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Loading else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Loading, *args)
        public val Empty: String
            get() = YumeLocaleManager.activeStrings.Connection.Empty
        @androidx.compose.runtime.Composable
        public fun Empty(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.Empty else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.Empty, *args)
        public val NoResults: String
            get() = YumeLocaleManager.activeStrings.Connection.NoResults
        @androidx.compose.runtime.Composable
        public fun NoResults(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Connection.NoResults else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Connection.NoResults, *args)
    }
    public object Editor {
        public object Title {
            public val Config: String
                get() = YumeLocaleManager.activeStrings.Editor.Title.Config
            @androidx.compose.runtime.Composable
            public fun Config(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Title.Config else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Title.Config, *args)
            public val Preview: String
                get() = YumeLocaleManager.activeStrings.Editor.Title.Preview
            @androidx.compose.runtime.Composable
            public fun Preview(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Title.Preview else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Title.Preview, *args)
            public val Override: String
                get() = YumeLocaleManager.activeStrings.Editor.Title.Override
            @androidx.compose.runtime.Composable
            public fun Override(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Title.Override else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Title.Override, *args)
            public val Profile: String
                get() = YumeLocaleManager.activeStrings.Editor.Title.Profile
            @androidx.compose.runtime.Composable
            public fun Profile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Title.Profile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Title.Profile, *args)
        }
        public object Action {
            public val Format: String
                get() = YumeLocaleManager.activeStrings.Editor.Action.Format
            @androidx.compose.runtime.Composable
            public fun Format(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Action.Format else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Action.Format, *args)
            public val Save: String
                get() = YumeLocaleManager.activeStrings.Editor.Action.Save
            @androidx.compose.runtime.Composable
            public fun Save(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Action.Save else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Action.Save, *args)
        }
        public object Message {
            public val FormatSuccess: String
                get() = YumeLocaleManager.activeStrings.Editor.Message.FormatSuccess
            @androidx.compose.runtime.Composable
            public fun FormatSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Message.FormatSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Message.FormatSuccess, *args)
            public val FormatSkipped: String
                get() = YumeLocaleManager.activeStrings.Editor.Message.FormatSkipped
            @androidx.compose.runtime.Composable
            public fun FormatSkipped(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Message.FormatSkipped else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Message.FormatSkipped, *args)
            public val SaveFailed: String
                get() = YumeLocaleManager.activeStrings.Editor.Message.SaveFailed
            @androidx.compose.runtime.Composable
            public fun SaveFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Message.SaveFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Message.SaveFailed, *args)
        }
        public object Discard {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Editor.Discard.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Discard.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Discard.Title, *args)
            public val Summary: String
                get() = YumeLocaleManager.activeStrings.Editor.Discard.Summary
            @androidx.compose.runtime.Composable
            public fun Summary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Discard.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Discard.Summary, *args)
            public val Confirm: String
                get() = YumeLocaleManager.activeStrings.Editor.Discard.Confirm
            @androidx.compose.runtime.Composable
            public fun Confirm(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.Discard.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.Discard.Confirm, *args)
        }
        public val JsonSubtitle: String
            get() = YumeLocaleManager.activeStrings.Editor.JsonSubtitle
        @androidx.compose.runtime.Composable
        public fun JsonSubtitle(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Editor.JsonSubtitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Editor.JsonSubtitle, *args)
    }
    public object Feature {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Feature.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Title, *args)
        public object ServiceStatus {
            public val Section: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.Section
            @androidx.compose.runtime.Composable
            public fun Section(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.Section else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.Section, *args)
            public val SwitchStartSubStore: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.SwitchStartSubStore
            @androidx.compose.runtime.Composable
            public fun SwitchStartSubStore(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.SwitchStartSubStore else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.SwitchStartSubStore, *args)
            public val OpenSubStorePanel: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.OpenSubStorePanel
            @androidx.compose.runtime.Composable
            public fun OpenSubStorePanel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.OpenSubStorePanel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.OpenSubStorePanel, *args)
            public val AllowLan: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.AllowLan
            @androidx.compose.runtime.Composable
            public fun AllowLan(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.AllowLan else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.AllowLan, *args)
            public val AutoCloseModeTitle: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseModeTitle
            @androidx.compose.runtime.Composable
            public fun AutoCloseModeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseModeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseModeTitle, *args)
            public val AutoCloseModeAlwaysOn: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseModeAlwaysOn
            @androidx.compose.runtime.Composable
            public fun AutoCloseModeAlwaysOn(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseModeAlwaysOn else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseModeAlwaysOn, *args)
            public val AutoCloseMode5Min: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseMode5Min
            @androidx.compose.runtime.Composable
            public fun AutoCloseMode5Min(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseMode5Min else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseMode5Min, *args)
            public val AutoCloseMode10Min: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseMode10Min
            @androidx.compose.runtime.Composable
            public fun AutoCloseMode10Min(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseMode10Min else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoCloseMode10Min, *args)
            public val AutoClosed: String
                get() = YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoClosed
            @androidx.compose.runtime.Composable
            public fun AutoClosed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoClosed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.ServiceStatus.AutoClosed, *args)
        }
        public object Panel {
            public val Section: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.Section
            @androidx.compose.runtime.Composable
            public fun Section(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.Section else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.Section, *args)
            public val SelectPanel: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.SelectPanel
            @androidx.compose.runtime.Composable
            public fun SelectPanel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.SelectPanel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.SelectPanel, *args)
            public val OpenMode: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.OpenMode
            @androidx.compose.runtime.Composable
            public fun OpenMode(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.OpenMode else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.OpenMode, *args)
            public val OpenPanel: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.OpenPanel
            @androidx.compose.runtime.Composable
            public fun OpenPanel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.OpenPanel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.OpenPanel, *args)
            public val Unknown: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.Unknown
            @androidx.compose.runtime.Composable
            public fun Unknown(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.Unknown else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.Unknown, *args)
            public val CreateShortcut: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.CreateShortcut
            @androidx.compose.runtime.Composable
            public fun CreateShortcut(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.CreateShortcut else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.CreateShortcut, *args)
            public val CreateShortcutSummary: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.CreateShortcutSummary
            @androidx.compose.runtime.Composable
            public fun CreateShortcutSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.CreateShortcutSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.CreateShortcutSummary, *args)
            public val ShortcutTitle: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.ShortcutTitle
            @androidx.compose.runtime.Composable
            public fun ShortcutTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.ShortcutTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.ShortcutTitle, *args)
            public val ShortcutName: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.ShortcutName
            @androidx.compose.runtime.Composable
            public fun ShortcutName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.ShortcutName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.ShortcutName, *args)
            public val ShortcutPickIcon: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.ShortcutPickIcon
            @androidx.compose.runtime.Composable
            public fun ShortcutPickIcon(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.ShortcutPickIcon else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.ShortcutPickIcon, *args)
            public val ShortcutResetIcon: String
                get() = YumeLocaleManager.activeStrings.Feature.Panel.ShortcutResetIcon
            @androidx.compose.runtime.Composable
            public fun ShortcutResetIcon(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.Panel.ShortcutResetIcon else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.Panel.ShortcutResetIcon, *args)
        }
        public object RemoteController {
            public val Section: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Section
            @androidx.compose.runtime.Composable
            public fun Section(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Section else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Section, *args)
            public val ModeTitle: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.ModeTitle
            @androidx.compose.runtime.Composable
            public fun ModeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.ModeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.ModeTitle, *args)
            public val ControlBackend: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.ControlBackend
            @androidx.compose.runtime.Composable
            public fun ControlBackend(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.ControlBackend else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.ControlBackend, *args)
            public val AddBackend: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.AddBackend
            @androidx.compose.runtime.Composable
            public fun AddBackend(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.AddBackend else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.AddBackend, *args)
            public val EditBackend: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.EditBackend
            @androidx.compose.runtime.Composable
            public fun EditBackend(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.EditBackend else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.EditBackend, *args)
            public val Name: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Name
            @androidx.compose.runtime.Composable
            public fun Name(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Name else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Name, *args)
            public val Host: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Host
            @androidx.compose.runtime.Composable
            public fun Host(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Host else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Host, *args)
            public val Port: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Port
            @androidx.compose.runtime.Composable
            public fun Port(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Port else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Port, *args)
            public val Secret: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Secret
            @androidx.compose.runtime.Composable
            public fun Secret(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Secret else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Secret, *args)
            public val Protocol: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Protocol
            @androidx.compose.runtime.Composable
            public fun Protocol(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Protocol else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Protocol, *args)
            public val PortRangeError: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.PortRangeError
            @androidx.compose.runtime.Composable
            public fun PortRangeError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.PortRangeError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.PortRangeError, *args)
            public val Connected: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Connected
            @androidx.compose.runtime.Composable
            public fun Connected(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Connected else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Connected, *args)
            public val ConnectionFailed: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.ConnectionFailed
            @androidx.compose.runtime.Composable
            public fun ConnectionFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.ConnectionFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.ConnectionFailed, *args)
            public val Delete: String
                get() = YumeLocaleManager.activeStrings.Feature.RemoteController.Delete
            @androidx.compose.runtime.Composable
            public fun Delete(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.RemoteController.Delete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.RemoteController.Delete, *args)
        }
        public object SubStore {
            public val Section: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.Section
            @androidx.compose.runtime.Composable
            public fun Section(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.Section else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.Section, *args)
            public val JavetLibraryReady: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.JavetLibraryReady
            @androidx.compose.runtime.Composable
            public fun JavetLibraryReady(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.JavetLibraryReady else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.JavetLibraryReady, *args)
            public val JavetLibraryDownload: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.JavetLibraryDownload
            @androidx.compose.runtime.Composable
            public fun JavetLibraryDownload(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.JavetLibraryDownload else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.JavetLibraryDownload, *args)
            public val JavetAvailable: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.JavetAvailable
            @androidx.compose.runtime.Composable
            public fun JavetAvailable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.JavetAvailable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.JavetAvailable, *args)
            public val DownloadHint: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.DownloadHint
            @androidx.compose.runtime.Composable
            public fun DownloadHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.DownloadHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.DownloadHint, *args)
            public val JavetDownloadSuccess: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.JavetDownloadSuccess
            @androidx.compose.runtime.Composable
            public fun JavetDownloadSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.JavetDownloadSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.JavetDownloadSuccess, *args)
            public val JavetDownloadFailed: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.JavetDownloadFailed
            @androidx.compose.runtime.Composable
            public fun JavetDownloadFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.JavetDownloadFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.JavetDownloadFailed, *args)
            public val DownloadResources: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.DownloadResources
            @androidx.compose.runtime.Composable
            public fun DownloadResources(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.DownloadResources else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.DownloadResources, *args)
            public val DownloadResourcesSummary: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.DownloadResourcesSummary
            @androidx.compose.runtime.Composable
            public fun DownloadResourcesSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.DownloadResourcesSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.DownloadResourcesSummary, *args)
            public val Not32Bit: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.Not32Bit
            @androidx.compose.runtime.Composable
            public fun Not32Bit(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.Not32Bit else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.Not32Bit, *args)
            public val DownloadSubStoreFirst: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.DownloadSubStoreFirst
            @androidx.compose.runtime.Composable
            public fun DownloadSubStoreFirst(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.DownloadSubStoreFirst else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.DownloadSubStoreFirst, *args)
            public val JavetNotReady: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.JavetNotReady
            @androidx.compose.runtime.Composable
            public fun JavetNotReady(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.JavetNotReady else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.JavetNotReady, *args)
            public val FrontendDownloadSuccess: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.FrontendDownloadSuccess
            @androidx.compose.runtime.Composable
            public fun FrontendDownloadSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.FrontendDownloadSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.FrontendDownloadSuccess, *args)
            public val FrontendDownloadFailed: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.FrontendDownloadFailed
            @androidx.compose.runtime.Composable
            public fun FrontendDownloadFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.FrontendDownloadFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.FrontendDownloadFailed, *args)
            public val BackendDownloadSuccess: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.BackendDownloadSuccess
            @androidx.compose.runtime.Composable
            public fun BackendDownloadSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.BackendDownloadSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.BackendDownloadSuccess, *args)
            public val BackendDownloadFailed: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.BackendDownloadFailed
            @androidx.compose.runtime.Composable
            public fun BackendDownloadFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.BackendDownloadFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.BackendDownloadFailed, *args)
            public val DownloadError: String
                get() = YumeLocaleManager.activeStrings.Feature.SubStore.DownloadError
            @androidx.compose.runtime.Composable
            public fun DownloadError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.SubStore.DownloadError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.SubStore.DownloadError, *args)
        }
        public object BackupRestore {
            public val Section: String
                get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Section
            @androidx.compose.runtime.Composable
            public fun Section(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Section else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Section, *args)
            public val ExportTitle: String
                get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.ExportTitle
            @androidx.compose.runtime.Composable
            public fun ExportTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.ExportTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.ExportTitle, *args)
            public val ExportSummary: String
                get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.ExportSummary
            @androidx.compose.runtime.Composable
            public fun ExportSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.ExportSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.ExportSummary, *args)
            public val RestoreTitle: String
                get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreTitle
            @androidx.compose.runtime.Composable
            public fun RestoreTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreTitle, *args)
            public val RestoreSummary: String
                get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreSummary
            @androidx.compose.runtime.Composable
            public fun RestoreSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreSummary, *args)
            public val Cancel: String
                get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Cancel
            @androidx.compose.runtime.Composable
            public fun Cancel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Cancel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Cancel, *args)
            public object RestoreDialog {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreDialog.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreDialog.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreDialog.Title, *args)
                public val Message: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreDialog.Message
                @androidx.compose.runtime.Composable
                public fun Message(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreDialog.Message else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.RestoreDialog.Message, *args)
            }
            public object Message {
                public val ExportSuccess: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Message.ExportSuccess
                @androidx.compose.runtime.Composable
                public fun ExportSuccess(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Message.ExportSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Message.ExportSuccess, *args)
                public val RestoreSuccess: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Message.RestoreSuccess
                @androidx.compose.runtime.Composable
                public fun RestoreSuccess(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Message.RestoreSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Message.RestoreSuccess, *args)
            }
            public object Error {
                public val OpenOutputFailed: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OpenOutputFailed
                @androidx.compose.runtime.Composable
                public fun OpenOutputFailed(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OpenOutputFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OpenOutputFailed, *args)
                public val OpenInputFailed: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OpenInputFailed
                @androidx.compose.runtime.Composable
                public fun OpenInputFailed(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OpenInputFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OpenInputFailed, *args)
                public val OperationFailed: String
                    get() = YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OperationFailed
                @androidx.compose.runtime.Composable
                public fun OperationFailed(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OperationFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Feature.BackupRestore.Error.OperationFailed, *args)
            }
        }
    }
    public object Home {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Home.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Title, *args)
        public object Message {
            public val ConfigSwitched: String
                get() = YumeLocaleManager.activeStrings.Home.Message.ConfigSwitched
            @androidx.compose.runtime.Composable
            public fun ConfigSwitched(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.ConfigSwitched else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.ConfigSwitched, *args)
            public val ConfigSwitchFailed: String
                get() = YumeLocaleManager.activeStrings.Home.Message.ConfigSwitchFailed
            @androidx.compose.runtime.Composable
            public fun ConfigSwitchFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.ConfigSwitchFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.ConfigSwitchFailed, *args)
            public val Preparing: String
                get() = YumeLocaleManager.activeStrings.Home.Message.Preparing
            @androidx.compose.runtime.Composable
            public fun Preparing(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.Preparing else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.Preparing, *args)
            public val BuiltinGeoRequired: String
                get() = YumeLocaleManager.activeStrings.Home.Message.BuiltinGeoRequired
            @androidx.compose.runtime.Composable
            public fun BuiltinGeoRequired(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.BuiltinGeoRequired else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.BuiltinGeoRequired, *args)
            public val StartFailed: String
                get() = YumeLocaleManager.activeStrings.Home.Message.StartFailed
            @androidx.compose.runtime.Composable
            public fun StartFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.StartFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.StartFailed, *args)
            public val RuntimeUnavailable: String
                get() = YumeLocaleManager.activeStrings.Home.Message.RuntimeUnavailable
            @androidx.compose.runtime.Composable
            public fun RuntimeUnavailable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.RuntimeUnavailable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.RuntimeUnavailable, *args)
            public val StopFailed: String
                get() = YumeLocaleManager.activeStrings.Home.Message.StopFailed
            @androidx.compose.runtime.Composable
            public fun StopFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Message.StopFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Message.StopFailed, *args)
        }
        public object Control {
            public val HintAddProfile: String
                get() = YumeLocaleManager.activeStrings.Home.Control.HintAddProfile
            @androidx.compose.runtime.Composable
            public fun HintAddProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Control.HintAddProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Control.HintAddProfile, *args)
            public val HintEnableProfile: String
                get() = YumeLocaleManager.activeStrings.Home.Control.HintEnableProfile
            @androidx.compose.runtime.Composable
            public fun HintEnableProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Control.HintEnableProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Control.HintEnableProfile, *args)
            public val Start: String
                get() = YumeLocaleManager.activeStrings.Home.Control.Start
            @androidx.compose.runtime.Composable
            public fun Start(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Control.Start else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Control.Start, *args)
            public val Stop: String
                get() = YumeLocaleManager.activeStrings.Home.Control.Stop
            @androidx.compose.runtime.Composable
            public fun Stop(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Control.Stop else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Control.Stop, *args)
        }
        public object NodeInfo {
            public val Node: String
                get() = YumeLocaleManager.activeStrings.Home.NodeInfo.Node
            @androidx.compose.runtime.Composable
            public fun Node(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.NodeInfo.Node else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.NodeInfo.Node, *args)
            public val Delay: String
                get() = YumeLocaleManager.activeStrings.Home.NodeInfo.Delay
            @androidx.compose.runtime.Composable
            public fun Delay(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.NodeInfo.Delay else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.NodeInfo.Delay, *args)
            public val Unknown: String
                get() = YumeLocaleManager.activeStrings.Home.NodeInfo.Unknown
            @androidx.compose.runtime.Composable
            public fun Unknown(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.NodeInfo.Unknown else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.NodeInfo.Unknown, *args)
            public val DelayValue: String
                get() = YumeLocaleManager.activeStrings.Home.NodeInfo.DelayValue
            @androidx.compose.runtime.Composable
            public fun DelayValue(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.NodeInfo.DelayValue else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.NodeInfo.DelayValue, *args)
        }
        public object Traffic {
            public val UpShort: String
                get() = YumeLocaleManager.activeStrings.Home.Traffic.UpShort
            @androidx.compose.runtime.Composable
            public fun UpShort(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Traffic.UpShort else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Traffic.UpShort, *args)
            public val DownShort: String
                get() = YumeLocaleManager.activeStrings.Home.Traffic.DownShort
            @androidx.compose.runtime.Composable
            public fun DownShort(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Traffic.DownShort else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Traffic.DownShort, *args)
            public val Upload: String
                get() = YumeLocaleManager.activeStrings.Home.Traffic.Upload
            @androidx.compose.runtime.Composable
            public fun Upload(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Traffic.Upload else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Traffic.Upload, *args)
            public val Download: String
                get() = YumeLocaleManager.activeStrings.Home.Traffic.Download
            @androidx.compose.runtime.Composable
            public fun Download(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Traffic.Download else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Traffic.Download, *args)
            public val NoProfile: String
                get() = YumeLocaleManager.activeStrings.Home.Traffic.NoProfile
            @androidx.compose.runtime.Composable
            public fun NoProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Traffic.NoProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Traffic.NoProfile, *args)
        }
        public object IpInfo {
            public val ExitIp: String
                get() = YumeLocaleManager.activeStrings.Home.IpInfo.ExitIp
            @androidx.compose.runtime.Composable
            public fun ExitIp(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.IpInfo.ExitIp else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.IpInfo.ExitIp, *args)
        }
        public object Status {
            public val Connecting: String
                get() = YumeLocaleManager.activeStrings.Home.Status.Connecting
            @androidx.compose.runtime.Composable
            public fun Connecting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Status.Connecting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Status.Connecting, *args)
            public val Disconnecting: String
                get() = YumeLocaleManager.activeStrings.Home.Status.Disconnecting
            @androidx.compose.runtime.Composable
            public fun Disconnecting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Status.Disconnecting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Status.Disconnecting, *args)
            public val Running: String
                get() = YumeLocaleManager.activeStrings.Home.Status.Running
            @androidx.compose.runtime.Composable
            public fun Running(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Status.Running else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Status.Running, *args)
            public val Lost: String
                get() = YumeLocaleManager.activeStrings.Home.Status.Lost
            @androidx.compose.runtime.Composable
            public fun Lost(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Status.Lost else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Status.Lost, *args)
            public val TapToStart: String
                get() = YumeLocaleManager.activeStrings.Home.Status.TapToStart
            @androidx.compose.runtime.Composable
            public fun TapToStart(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Status.TapToStart else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Status.TapToStart, *args)
        }
        public object Settings {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Home.Settings.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Settings.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Settings.Title, *args)
            public val Quote: String
                get() = YumeLocaleManager.activeStrings.Home.Settings.Quote
            @androidx.compose.runtime.Composable
            public fun Quote(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Settings.Quote else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Settings.Quote, *args)
            public val ClassicHome: String
                get() = YumeLocaleManager.activeStrings.Home.Settings.ClassicHome
            @androidx.compose.runtime.Composable
            public fun ClassicHome(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Settings.ClassicHome else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Settings.ClassicHome, *args)
            public val ExpandSidebar: String
                get() = YumeLocaleManager.activeStrings.Home.Settings.ExpandSidebar
            @androidx.compose.runtime.Composable
            public fun ExpandSidebar(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Settings.ExpandSidebar else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Settings.ExpandSidebar, *args)
            public val ChangeWallpaper: String
                get() = YumeLocaleManager.activeStrings.Home.Settings.ChangeWallpaper
            @androidx.compose.runtime.Composable
            public fun ChangeWallpaper(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.Settings.ChangeWallpaper else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.Settings.ChangeWallpaper, *args)
        }
        public object ProxyMode {
            public val Vpn: String
                get() = YumeLocaleManager.activeStrings.Home.ProxyMode.Vpn
            @androidx.compose.runtime.Composable
            public fun Vpn(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.ProxyMode.Vpn else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.ProxyMode.Vpn, *args)
            public val Tun: String
                get() = YumeLocaleManager.activeStrings.Home.ProxyMode.Tun
            @androidx.compose.runtime.Composable
            public fun Tun(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.ProxyMode.Tun else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.ProxyMode.Tun, *args)
            public val Ebpf: String
                get() = YumeLocaleManager.activeStrings.Home.ProxyMode.Ebpf
            @androidx.compose.runtime.Composable
            public fun Ebpf(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.ProxyMode.Ebpf else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.ProxyMode.Ebpf, *args)
        }
        public object PreviewGuide {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.Title, *args)
            public val Description: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.Description
            @androidx.compose.runtime.Composable
            public fun Description(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.Description else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.Description, *args)
            public val Next: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.Next
            @androidx.compose.runtime.Composable
            public fun Next(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.Next else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.Next, *args)
            public val Start: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.Start
            @androidx.compose.runtime.Composable
            public fun Start(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.Start else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.Start, *args)
            public val WallpaperTitle: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.WallpaperTitle
            @androidx.compose.runtime.Composable
            public fun WallpaperTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.WallpaperTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.WallpaperTitle, *args)
            public val WallpaperDescription: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.WallpaperDescription
            @androidx.compose.runtime.Composable
            public fun WallpaperDescription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.WallpaperDescription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.WallpaperDescription, *args)
            public val SwipeTitle: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.SwipeTitle
            @androidx.compose.runtime.Composable
            public fun SwipeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.SwipeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.SwipeTitle, *args)
            public val SwipeDescription: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.SwipeDescription
            @androidx.compose.runtime.Composable
            public fun SwipeDescription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.SwipeDescription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.SwipeDescription, *args)
            public val AddTitle: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.AddTitle
            @androidx.compose.runtime.Composable
            public fun AddTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.AddTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.AddTitle, *args)
            public val AddDescription: String
                get() = YumeLocaleManager.activeStrings.Home.PreviewGuide.AddDescription
            @androidx.compose.runtime.Composable
            public fun AddDescription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Home.PreviewGuide.AddDescription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Home.PreviewGuide.AddDescription, *args)
        }
    }
    public object Log {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Log.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Title, *args)
        public val Summary: String
            get() = YumeLocaleManager.activeStrings.Log.Summary
        @androidx.compose.runtime.Composable
        public fun Summary(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Summary, *args)
        public val SearchHint: String
            get() = YumeLocaleManager.activeStrings.Log.SearchHint
        @androidx.compose.runtime.Composable
        public fun SearchHint(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.SearchHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.SearchHint, *args)
        public object Level {
            public val All: String
                get() = YumeLocaleManager.activeStrings.Log.Level.All
            @androidx.compose.runtime.Composable
            public fun All(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Level.All else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Level.All, *args)
            public val Debug: String
                get() = YumeLocaleManager.activeStrings.Log.Level.Debug
            @androidx.compose.runtime.Composable
            public fun Debug(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Level.Debug else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Level.Debug, *args)
            public val Info: String
                get() = YumeLocaleManager.activeStrings.Log.Level.Info
            @androidx.compose.runtime.Composable
            public fun Info(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Level.Info else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Level.Info, *args)
            public val Warning: String
                get() = YumeLocaleManager.activeStrings.Log.Level.Warning
            @androidx.compose.runtime.Composable
            public fun Warning(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Level.Warning else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Level.Warning, *args)
            public val Error: String
                get() = YumeLocaleManager.activeStrings.Log.Level.Error
            @androidx.compose.runtime.Composable
            public fun Error(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Level.Error else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Level.Error, *args)
            public val Filter: String
                get() = YumeLocaleManager.activeStrings.Log.Level.Filter
            @androidx.compose.runtime.Composable
            public fun Filter(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Level.Filter else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Level.Filter, *args)
        }
        public object Action {
            public val Export: String
                get() = YumeLocaleManager.activeStrings.Log.Action.Export
            @androidx.compose.runtime.Composable
            public fun Export(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Action.Export else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Action.Export, *args)
            public val Copied: String
                get() = YumeLocaleManager.activeStrings.Log.Action.Copied
            @androidx.compose.runtime.Composable
            public fun Copied(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Action.Copied else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Action.Copied, *args)
        }
        public object Empty {
            public val Connecting: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.Connecting
            @androidx.compose.runtime.Composable
            public fun Connecting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.Connecting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.Connecting, *args)
            public val ConnectingHint: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.ConnectingHint
            @androidx.compose.runtime.Composable
            public fun ConnectingHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.ConnectingHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.ConnectingHint, *args)
            public val Retrying: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.Retrying
            @androidx.compose.runtime.Composable
            public fun Retrying(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.Retrying else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.Retrying, *args)
            public val RetryingHint: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.RetryingHint
            @androidx.compose.runtime.Composable
            public fun RetryingHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.RetryingHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.RetryingHint, *args)
            public val NoLogs: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.NoLogs
            @androidx.compose.runtime.Composable
            public fun NoLogs(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.NoLogs else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.NoLogs, *args)
            public val LiveHint: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.LiveHint
            @androidx.compose.runtime.Composable
            public fun LiveHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.LiveHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.LiveHint, *args)
            public val NoMatch: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.NoMatch
            @androidx.compose.runtime.Composable
            public fun NoMatch(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.NoMatch else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.NoMatch, *args)
            public val NoResults: String
                get() = YumeLocaleManager.activeStrings.Log.Empty.NoResults
            @androidx.compose.runtime.Composable
            public fun NoResults(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Empty.NoResults else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Empty.NoResults, *args)
        }
        public object Detail {
            public val WaitingLog: String
                get() = YumeLocaleManager.activeStrings.Log.Detail.WaitingLog
            @androidx.compose.runtime.Composable
            public fun WaitingLog(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Detail.WaitingLog else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Detail.WaitingLog, *args)
            public val WillShowWhenGenerated: String
                get() = YumeLocaleManager.activeStrings.Log.Detail.WillShowWhenGenerated
            @androidx.compose.runtime.Composable
            public fun WillShowWhenGenerated(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Log.Detail.WillShowWhenGenerated else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Log.Detail.WillShowWhenGenerated, *args)
        }
    }
    public object MetaFeature {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.MetaFeature.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.Title, *args)
        public object Section {
            public val ConnectionAndTraffic: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.Section.ConnectionAndTraffic
            @androidx.compose.runtime.Composable
            public fun ConnectionAndTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.Section.ConnectionAndTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.Section.ConnectionAndTraffic, *args)
            public val Routing: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.Section.Routing
            @androidx.compose.runtime.Composable
            public fun Routing(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.Section.Routing else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.Section.Routing, *args)
        }
        public object GeoX {
            public val OnlineUpdateTitle: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.GeoX.OnlineUpdateTitle
            @androidx.compose.runtime.Composable
            public fun OnlineUpdateTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.GeoX.OnlineUpdateTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.GeoX.OnlineUpdateTitle, *args)
            public val OnlineUpdateSummary: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.GeoX.OnlineUpdateSummary
            @androidx.compose.runtime.Composable
            public fun OnlineUpdateSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.GeoX.OnlineUpdateSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.GeoX.OnlineUpdateSummary, *args)
        }
        public object CustomRouting {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Title, *args)
            public val Summary: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Summary
            @androidx.compose.runtime.Composable
            public fun Summary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Summary, *args)
            public val EditYaml: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.EditYaml
            @androidx.compose.runtime.Composable
            public fun EditYaml(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.EditYaml else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.EditYaml, *args)
            public val ManualYamlPresetDiscarded: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.ManualYamlPresetDiscarded
            @androidx.compose.runtime.Composable
            public fun ManualYamlPresetDiscarded(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.ManualYamlPresetDiscarded else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.ManualYamlPresetDiscarded, *args)
            public val GroupTypeTitle: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeTitle
            @androidx.compose.runtime.Composable
            public fun GroupTypeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeTitle, *args)
            public val GroupTypeUrlTest: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeUrlTest
            @androidx.compose.runtime.Composable
            public fun GroupTypeUrlTest(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeUrlTest else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeUrlTest, *args)
            public val GroupTypeFallback: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeFallback
            @androidx.compose.runtime.Composable
            public fun GroupTypeFallback(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeFallback else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.GroupTypeFallback, *args)
            public val UrlTestRegionGroupTitle: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.UrlTestRegionGroupTitle
            @androidx.compose.runtime.Composable
            public fun UrlTestRegionGroupTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.UrlTestRegionGroupTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.UrlTestRegionGroupTitle, *args)
            public val FallbackRegionGroupTitle: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.FallbackRegionGroupTitle
            @androidx.compose.runtime.Composable
            public fun FallbackRegionGroupTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.FallbackRegionGroupTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.FallbackRegionGroupTitle, *args)
            public object Region {
                public val HongKong: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.HongKong
                @androidx.compose.runtime.Composable
                public fun HongKong(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.HongKong else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.HongKong, *args)
                public val Taiwan: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Taiwan
                @androidx.compose.runtime.Composable
                public fun Taiwan(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Taiwan else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Taiwan, *args)
                public val Japan: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Japan
                @androidx.compose.runtime.Composable
                public fun Japan(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Japan else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Japan, *args)
                public val Singapore: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Singapore
                @androidx.compose.runtime.Composable
                public fun Singapore(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Singapore else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Singapore, *args)
                public val UnitedStates: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.UnitedStates
                @androidx.compose.runtime.Composable
                public fun UnitedStates(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.UnitedStates else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.UnitedStates, *args)
                public val Other: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Other
                @androidx.compose.runtime.Composable
                public fun Other(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Other else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Region.Other, *args)
            }
            public object Item {
                public val Proxy: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Proxy
                @androidx.compose.runtime.Composable
                public fun Proxy(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Proxy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Proxy, *args)
                public val Ads: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Ads
                @androidx.compose.runtime.Composable
                public fun Ads(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Ads else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Ads, *args)
                public val China: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.China
                @androidx.compose.runtime.Composable
                public fun China(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.China else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.China, *args)
                public val Global: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Global
                @androidx.compose.runtime.Composable
                public fun Global(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Global else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Global, *args)
                public val Match: String
                    get() = YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Match
                @androidx.compose.runtime.Composable
                public fun Match(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Match else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.CustomRouting.Item.Match, *args)
            }
        }
        public object RuntimeRules {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.RuntimeRules.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.RuntimeRules.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.RuntimeRules.Title, *args)
            public val Summary: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.RuntimeRules.Summary
            @androidx.compose.runtime.Composable
            public fun Summary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.RuntimeRules.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.RuntimeRules.Summary, *args)
        }
        public object Download {
            public val DialogTitle: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.Download.DialogTitle
            @androidx.compose.runtime.Composable
            public fun DialogTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.Download.DialogTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.Download.DialogTitle, *args)
            public val DownloadComplete: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.Download.DownloadComplete
            @androidx.compose.runtime.Composable
            public fun DownloadComplete(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.Download.DownloadComplete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.Download.DownloadComplete, *args)
        }
        public object AgeKey {
            public val Section: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.Section
            @androidx.compose.runtime.Composable
            public fun Section(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.Section else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.Section, *args)
            public val X25519Title: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.X25519Title
            @androidx.compose.runtime.Composable
            public fun X25519Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.X25519Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.X25519Title, *args)
            public val HybridTitle: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.HybridTitle
            @androidx.compose.runtime.Composable
            public fun HybridTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.HybridTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.HybridTitle, *args)
            public val SecretKey: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.SecretKey
            @androidx.compose.runtime.Composable
            public fun SecretKey(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.SecretKey else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.SecretKey, *args)
            public val PublicKey: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.PublicKey
            @androidx.compose.runtime.Composable
            public fun PublicKey(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.PublicKey else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.PublicKey, *args)
            public val Generate: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.Generate
            @androidx.compose.runtime.Composable
            public fun Generate(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.Generate else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.Generate, *args)
            public val DerivePublicKey: String
                get() = YumeLocaleManager.activeStrings.MetaFeature.AgeKey.DerivePublicKey
            @androidx.compose.runtime.Composable
            public fun DerivePublicKey(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.MetaFeature.AgeKey.DerivePublicKey else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.MetaFeature.AgeKey.DerivePublicKey, *args)
        }
    }
    public object NetworkSettings {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.NetworkSettings.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Title, *args)
        public object Section {
            public val VpnOptions: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Section.VpnOptions
            @androidx.compose.runtime.Composable
            public fun VpnOptions(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Section.VpnOptions else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Section.VpnOptions, *args)
            public val Advanced: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Section.Advanced
            @androidx.compose.runtime.Composable
            public fun Advanced(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Section.Advanced else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Section.Advanced, *args)
            public val ProxyOptions: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Section.ProxyOptions
            @androidx.compose.runtime.Composable
            public fun ProxyOptions(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Section.ProxyOptions else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Section.ProxyOptions, *args)
            public val Kernel: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Section.Kernel
            @androidx.compose.runtime.Composable
            public fun Kernel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Section.Kernel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Section.Kernel, *args)
        }
        public object Advanced {
            public val DisableOverrideTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Advanced.DisableOverrideTitle
            @androidx.compose.runtime.Composable
            public fun DisableOverrideTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Advanced.DisableOverrideTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Advanced.DisableOverrideTitle, *args)
        }
        public object RunMode {
            public val SectionTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.SectionTitle
            @androidx.compose.runtime.Composable
            public fun SectionTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.SectionTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.SectionTitle, *args)
            public val VpnServiceTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.VpnServiceTitle
            @androidx.compose.runtime.Composable
            public fun VpnServiceTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.VpnServiceTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.VpnServiceTitle, *args)
            public val VpnServiceSummary: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.VpnServiceSummary
            @androidx.compose.runtime.Composable
            public fun VpnServiceSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.VpnServiceSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.VpnServiceSummary, *args)
            public val TunTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.TunTitle
            @androidx.compose.runtime.Composable
            public fun TunTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.TunTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.TunTitle, *args)
            public val TunSummary: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.TunSummary
            @androidx.compose.runtime.Composable
            public fun TunSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.TunSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.TunSummary, *args)
            public val EbpfTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.EbpfTitle
            @androidx.compose.runtime.Composable
            public fun EbpfTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.EbpfTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.EbpfTitle, *args)
            public val EbpfSummary: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.RunMode.EbpfSummary
            @androidx.compose.runtime.Composable
            public fun EbpfSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.RunMode.EbpfSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.RunMode.EbpfSummary, *args)
        }
        public object VpnOptions {
            public val BypassPrivateTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.BypassPrivateTitle
            @androidx.compose.runtime.Composable
            public fun BypassPrivateTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.BypassPrivateTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.BypassPrivateTitle, *args)
            public val DnsHijackTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.DnsHijackTitle
            @androidx.compose.runtime.Composable
            public fun DnsHijackTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.DnsHijackTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.DnsHijackTitle, *args)
            public val AllowBypassTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.AllowBypassTitle
            @androidx.compose.runtime.Composable
            public fun AllowBypassTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.AllowBypassTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.AllowBypassTitle, *args)
            public val EnableIpv6Title: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.EnableIpv6Title
            @androidx.compose.runtime.Composable
            public fun EnableIpv6Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.EnableIpv6Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.EnableIpv6Title, *args)
            public val SystemProxyTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.SystemProxyTitle
            @androidx.compose.runtime.Composable
            public fun SystemProxyTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.SystemProxyTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.VpnOptions.SystemProxyTitle, *args)
        }
        public object EbpfOptions {
            public val BypassCnTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.EbpfOptions.BypassCnTitle
            @androidx.compose.runtime.Composable
            public fun BypassCnTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.EbpfOptions.BypassCnTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.EbpfOptions.BypassCnTitle, *args)
        }
        public object TunOptions {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.Title, *args)
            public val IfNameTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.IfNameTitle
            @androidx.compose.runtime.Composable
            public fun IfNameTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.IfNameTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.IfNameTitle, *args)
            public val MtuTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.MtuTitle
            @androidx.compose.runtime.Composable
            public fun MtuTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.MtuTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.MtuTitle, *args)
            public val StackTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackTitle
            @androidx.compose.runtime.Composable
            public fun StackTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackTitle, *args)
            public val StackSystem: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackSystem
            @androidx.compose.runtime.Composable
            public fun StackSystem(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackSystem else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackSystem, *args)
            public val StackGVisor: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackGVisor
            @androidx.compose.runtime.Composable
            public fun StackGVisor(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackGVisor else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackGVisor, *args)
            public val StackMixed: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackMixed
            @androidx.compose.runtime.Composable
            public fun StackMixed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackMixed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackMixed, *args)
            public val StackMips: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackMips
            @androidx.compose.runtime.Composable
            public fun StackMips(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackMips else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StackMips, *args)
            public val AutoRouteTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.AutoRouteTitle
            @androidx.compose.runtime.Composable
            public fun AutoRouteTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.AutoRouteTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.AutoRouteTitle, *args)
            public val StrictRouteTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StrictRouteTitle
            @androidx.compose.runtime.Composable
            public fun StrictRouteTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StrictRouteTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.StrictRouteTitle, *args)
            public val AutoRedirectTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.AutoRedirectTitle
            @androidx.compose.runtime.Composable
            public fun AutoRedirectTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.AutoRedirectTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.AutoRedirectTitle, *args)
            public val DnsModeTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsModeTitle
            @androidx.compose.runtime.Composable
            public fun DnsModeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsModeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsModeTitle, *args)
            public val DnsRedirHost: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsRedirHost
            @androidx.compose.runtime.Composable
            public fun DnsRedirHost(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsRedirHost else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsRedirHost, *args)
            public val DnsFakeIp: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsFakeIp
            @androidx.compose.runtime.Composable
            public fun DnsFakeIp(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsFakeIp else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.DnsFakeIp, *args)
            public val Ipv6Title: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.Ipv6Title
            @androidx.compose.runtime.Composable
            public fun Ipv6Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.Ipv6Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.TunOptions.Ipv6Title, *args)
        }
        public object Kernel {
            public val ActiveTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.ActiveTitle
            @androidx.compose.runtime.Composable
            public fun ActiveTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.ActiveTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.ActiveTitle, *args)
            public val BundledAlpha: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.BundledAlpha
            @androidx.compose.runtime.Composable
            public fun BundledAlpha(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.BundledAlpha else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.BundledAlpha, *args)
            public val RefreshTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.RefreshTitle
            @androidx.compose.runtime.Composable
            public fun RefreshTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.RefreshTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.RefreshTitle, *args)
            public val DownloadTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.DownloadTitle
            @androidx.compose.runtime.Composable
            public fun DownloadTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.DownloadTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.DownloadTitle, *args)
            public val FetchButton: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.FetchButton
            @androidx.compose.runtime.Composable
            public fun FetchButton(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.FetchButton else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.FetchButton, *args)
            public val DownloadButton: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.DownloadButton
            @androidx.compose.runtime.Composable
            public fun DownloadButton(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.DownloadButton else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.DownloadButton, *args)
            public val CustomTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomTitle
            @androidx.compose.runtime.Composable
            public fun CustomTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomTitle, *args)
            public val CustomMethodTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomMethodTitle
            @androidx.compose.runtime.Composable
            public fun CustomMethodTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomMethodTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomMethodTitle, *args)
            public val CustomUrlMethod: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomUrlMethod
            @androidx.compose.runtime.Composable
            public fun CustomUrlMethod(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomUrlMethod else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomUrlMethod, *args)
            public val CustomFileMethod: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomFileMethod
            @androidx.compose.runtime.Composable
            public fun CustomFileMethod(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomFileMethod else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomFileMethod, *args)
            public val CustomUrlLabel: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomUrlLabel
            @androidx.compose.runtime.Composable
            public fun CustomUrlLabel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomUrlLabel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomUrlLabel, *args)
            public val CustomChooseFile: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomChooseFile
            @androidx.compose.runtime.Composable
            public fun CustomChooseFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomChooseFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomChooseFile, *args)
            public val CustomInstallButton: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomInstallButton
            @androidx.compose.runtime.Composable
            public fun CustomInstallButton(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomInstallButton else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Kernel.CustomInstallButton, *args)
        }
        public object ProxyOptions {
            public val AccessControlModeTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AccessControlModeTitle
            @androidx.compose.runtime.Composable
            public fun AccessControlModeTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AccessControlModeTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AccessControlModeTitle, *args)
            public val AllowAll: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AllowAll
            @androidx.compose.runtime.Composable
            public fun AllowAll(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AllowAll else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AllowAll, *args)
            public val AllowSelected: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AllowSelected
            @androidx.compose.runtime.Composable
            public fun AllowSelected(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AllowSelected else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.AllowSelected, *args)
            public val RejectSelected: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.RejectSelected
            @androidx.compose.runtime.Composable
            public fun RejectSelected(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.RejectSelected else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.RejectSelected, *args)
            public val ManageAccessControlTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.ManageAccessControlTitle
            @androidx.compose.runtime.Composable
            public fun ManageAccessControlTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.ManageAccessControlTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.ProxyOptions.ManageAccessControlTitle, *args)
        }
        public object WifiAutomation {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Title, *args)
            public val EnabledTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EnabledTitle
            @androidx.compose.runtime.Composable
            public fun EnabledTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EnabledTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EnabledTitle, *args)
            public val WifiNameHeading: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.WifiNameHeading
            @androidx.compose.runtime.Composable
            public fun WifiNameHeading(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.WifiNameHeading else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.WifiNameHeading, *args)
            public val NetworkChangeSection: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NetworkChangeSection
            @androidx.compose.runtime.Composable
            public fun NetworkChangeSection(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NetworkChangeSection else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NetworkChangeSection, *args)
            public val OtherWifiTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.OtherWifiTitle
            @androidx.compose.runtime.Composable
            public fun OtherWifiTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.OtherWifiTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.OtherWifiTitle, *args)
            public val NoWifiTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoWifiTitle
            @androidx.compose.runtime.Composable
            public fun NoWifiTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoWifiTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoWifiTitle, *args)
            public val AddCurrent: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.AddCurrent
            @androidx.compose.runtime.Composable
            public fun AddCurrent(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.AddCurrent else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.AddCurrent, *args)
            public val AddManual: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.AddManual
            @androidx.compose.runtime.Composable
            public fun AddManual(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.AddManual else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.AddManual, *args)
            public val Scan: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Scan
            @androidx.compose.runtime.Composable
            public fun Scan(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Scan else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Scan, *args)
            public val Scanning: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Scanning
            @androidx.compose.runtime.Composable
            public fun Scanning(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Scanning else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Scanning, *args)
            public val ScanEmpty: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ScanEmpty
            @androidx.compose.runtime.Composable
            public fun ScanEmpty(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ScanEmpty else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ScanEmpty, *args)
            public val ScanUnavailable: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ScanUnavailable
            @androidx.compose.runtime.Composable
            public fun ScanUnavailable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ScanUnavailable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ScanUnavailable, *args)
            public val ManualDialogTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ManualDialogTitle
            @androidx.compose.runtime.Composable
            public fun ManualDialogTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ManualDialogTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ManualDialogTitle, *args)
            public val EditDialogTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EditDialogTitle
            @androidx.compose.runtime.Composable
            public fun EditDialogTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EditDialogTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EditDialogTitle, *args)
            public val ManualDialogLabel: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ManualDialogLabel
            @androidx.compose.runtime.Composable
            public fun ManualDialogLabel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ManualDialogLabel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ManualDialogLabel, *args)
            public val StartAction: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.StartAction
            @androidx.compose.runtime.Composable
            public fun StartAction(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.StartAction else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.StartAction, *args)
            public val StopAction: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.StopAction
            @androidx.compose.runtime.Composable
            public fun StopAction(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.StopAction else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.StopAction, *args)
            public val KeepAction: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.KeepAction
            @androidx.compose.runtime.Composable
            public fun KeepAction(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.KeepAction else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.KeepAction, *args)
            public val ActionTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ActionTitle
            @androidx.compose.runtime.Composable
            public fun ActionTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ActionTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ActionTitle, *args)
            public val ProfileAction: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ProfileAction
            @androidx.compose.runtime.Composable
            public fun ProfileAction(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ProfileAction else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ProfileAction, *args)
            public val NoSwitchAction: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoSwitchAction
            @androidx.compose.runtime.Composable
            public fun NoSwitchAction(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoSwitchAction else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoSwitchAction, *args)
            public val EmptyRules: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EmptyRules
            @androidx.compose.runtime.Composable
            public fun EmptyRules(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EmptyRules else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.EmptyRules, *args)
            public val VpnOnly: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.VpnOnly
            @androidx.compose.runtime.Composable
            public fun VpnOnly(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.VpnOnly else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.VpnOnly, *args)
            public val PermissionTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.PermissionTitle
            @androidx.compose.runtime.Composable
            public fun PermissionTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.PermissionTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.PermissionTitle, *args)
            public val PermissionMessage: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.PermissionMessage
            @androidx.compose.runtime.Composable
            public fun PermissionMessage(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.PermissionMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.PermissionMessage, *args)
            public val ApproximateTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ApproximateTitle
            @androidx.compose.runtime.Composable
            public fun ApproximateTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ApproximateTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ApproximateTitle, *args)
            public val ApproximateMessage: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ApproximateMessage
            @androidx.compose.runtime.Composable
            public fun ApproximateMessage(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ApproximateMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.ApproximateMessage, *args)
            public val DeniedTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.DeniedTitle
            @androidx.compose.runtime.Composable
            public fun DeniedTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.DeniedTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.DeniedTitle, *args)
            public val DeniedMessage: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.DeniedMessage
            @androidx.compose.runtime.Composable
            public fun DeniedMessage(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.DeniedMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.DeniedMessage, *args)
            public val SettingsTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.SettingsTitle
            @androidx.compose.runtime.Composable
            public fun SettingsTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.SettingsTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.SettingsTitle, *args)
            public val SettingsMessage: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.SettingsMessage
            @androidx.compose.runtime.Composable
            public fun SettingsMessage(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.SettingsMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.SettingsMessage, *args)
            public val LocationTitle: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.LocationTitle
            @androidx.compose.runtime.Composable
            public fun LocationTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.LocationTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.LocationTitle, *args)
            public val LocationMessage: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.LocationMessage
            @androidx.compose.runtime.Composable
            public fun LocationMessage(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.LocationMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.LocationMessage, *args)
            public val Grant: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Grant
            @androidx.compose.runtime.Composable
            public fun Grant(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Grant else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Grant, *args)
            public val OpenSettings: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.OpenSettings
            @androidx.compose.runtime.Composable
            public fun OpenSettings(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.OpenSettings else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.OpenSettings, *args)
            public val TurnOnLocation: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.TurnOnLocation
            @androidx.compose.runtime.Composable
            public fun TurnOnLocation(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.TurnOnLocation else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.TurnOnLocation, *args)
            public val Added: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Added
            @androidx.compose.runtime.Composable
            public fun Added(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Added else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Added, *args)
            public val Duplicate: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Duplicate
            @androidx.compose.runtime.Composable
            public fun Duplicate(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Duplicate else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Duplicate, *args)
            public val NoWifi: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoWifi
            @androidx.compose.runtime.Composable
            public fun NoWifi(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoWifi else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.NoWifi, *args)
            public val Unavailable: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Unavailable
            @androidx.compose.runtime.Composable
            public fun Unavailable(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Unavailable else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.WifiAutomation.Unavailable, *args)
        }
        public object Error {
            public val VpnDenied: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Error.VpnDenied
            @androidx.compose.runtime.Composable
            public fun VpnDenied(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Error.VpnDenied else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Error.VpnDenied, *args)
            public val RootRequired: String
                get() = YumeLocaleManager.activeStrings.NetworkSettings.Error.RootRequired
            @androidx.compose.runtime.Composable
            public fun RootRequired(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.NetworkSettings.Error.RootRequired else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.NetworkSettings.Error.RootRequired, *args)
        }
    }
    public object Onboarding {
        public object Navigation {
            public val Back: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Navigation.Back
            @androidx.compose.runtime.Composable
            public fun Back(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Navigation.Back else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Navigation.Back, *args)
            public val Next: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Navigation.Next
            @androidx.compose.runtime.Composable
            public fun Next(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Navigation.Next else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Navigation.Next, *args)
            public val Enter: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Navigation.Enter
            @androidx.compose.runtime.Composable
            public fun Enter(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Navigation.Enter else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Navigation.Enter, *args)
            public val Start: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Navigation.Start
            @androidx.compose.runtime.Composable
            public fun Start(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Navigation.Start else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Navigation.Start, *args)
        }
        public object Welcome {
            public val Tagline: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Welcome.Tagline
            @androidx.compose.runtime.Composable
            public fun Tagline(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Welcome.Tagline else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Welcome.Tagline, *args)
        }
        public object Permission {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Permission.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.Title, *args)
            public val Subtitle: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Permission.Subtitle
            @androidx.compose.runtime.Composable
            public fun Subtitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.Subtitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.Subtitle, *args)
            public object Common {
                public val Granted: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Permission.Common.Granted
                @androidx.compose.runtime.Composable
                public fun Granted(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.Common.Granted else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.Common.Granted, *args)
            }
            public object Notification {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.Title, *args)
                public val SummaryNeed: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.SummaryNeed
                @androidx.compose.runtime.Composable
                public fun SummaryNeed(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.SummaryNeed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.SummaryNeed, *args)
                public val SummaryNotRequired: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.SummaryNotRequired
                @androidx.compose.runtime.Composable
                public fun SummaryNotRequired(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.SummaryNotRequired else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.Notification.SummaryNotRequired, *args)
            }
            public object AppList {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Permission.AppList.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.AppList.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.AppList.Title, *args)
                public val SummaryNeed: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Permission.AppList.SummaryNeed
                @androidx.compose.runtime.Composable
                public fun SummaryNeed(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Permission.AppList.SummaryNeed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Permission.AppList.SummaryNeed, *args)
            }
        }
        public object Privacy {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.Title, *args)
            public val Subtitle: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.Subtitle
            @androidx.compose.runtime.Composable
            public fun Subtitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.Subtitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.Subtitle, *args)
            public val RichTextLead: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextLead
            @androidx.compose.runtime.Composable
            public fun RichTextLead(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextLead else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextLead, *args)
            public val RichTextPrefix: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextPrefix
            @androidx.compose.runtime.Composable
            public fun RichTextPrefix(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextPrefix else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextPrefix, *args)
            public val RichTextConnector: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextConnector
            @androidx.compose.runtime.Composable
            public fun RichTextConnector(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextConnector else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextConnector, *args)
            public val RichTextSuffix: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextSuffix
            @androidx.compose.runtime.Composable
            public fun RichTextSuffix(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextSuffix else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.RichTextSuffix, *args)
            public val TermsLink: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.TermsLink
            @androidx.compose.runtime.Composable
            public fun TermsLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.TermsLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.TermsLink, *args)
            public val PolicyLink: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.PolicyLink
            @androidx.compose.runtime.Composable
            public fun PolicyLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.PolicyLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.PolicyLink, *args)
            public object Accept {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Privacy.Accept.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Privacy.Accept.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Privacy.Accept.Title, *args)
            }
        }
        public object Personalize {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Personalize.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Personalize.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Personalize.Title, *args)
            public val Subtitle: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Personalize.Subtitle
            @androidx.compose.runtime.Composable
            public fun Subtitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Personalize.Subtitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Personalize.Subtitle, *args)
        }
        public object Finish {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Finish.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Finish.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Finish.Title, *args)
            public val Subtitle: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Finish.Subtitle
            @androidx.compose.runtime.Composable
            public fun Subtitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Finish.Subtitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Finish.Subtitle, *args)
        }
        public object Project {
            public object Github {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Project.Github.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Project.Github.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Project.Github.Title, *args)
                public val Summary: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Project.Github.Summary
                @androidx.compose.runtime.Composable
                public fun Summary(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Project.Github.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Project.Github.Summary, *args)
            }
            public object Community {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Project.Community.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Project.Community.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Project.Community.Title, *args)
                public val Summary: String
                    get() = YumeLocaleManager.activeStrings.Onboarding.Project.Community.Summary
                @androidx.compose.runtime.Composable
                public fun Summary(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Project.Community.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Project.Community.Summary, *args)
            }
        }
        public object Sheet {
            public val PrivacyPolicyTitle: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Sheet.PrivacyPolicyTitle
            @androidx.compose.runtime.Composable
            public fun PrivacyPolicyTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Sheet.PrivacyPolicyTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Sheet.PrivacyPolicyTitle, *args)
            public val LoadFailed: String
                get() = YumeLocaleManager.activeStrings.Onboarding.Sheet.LoadFailed
            @androidx.compose.runtime.Composable
            public fun LoadFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Onboarding.Sheet.LoadFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Onboarding.Sheet.LoadFailed, *args)
        }
    }
    public object OpenSourceLicenses {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.OpenSourceLicenses.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.OpenSourceLicenses.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.OpenSourceLicenses.Title, *args)
        public object LicenseSheet {
            public val NoContent: String
                get() = YumeLocaleManager.activeStrings.OpenSourceLicenses.LicenseSheet.NoContent
            @androidx.compose.runtime.Composable
            public fun NoContent(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.OpenSourceLicenses.LicenseSheet.NoContent else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.OpenSourceLicenses.LicenseSheet.NoContent, *args)
        }
    }
    public object Override {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Override.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Title, *args)
        public object Action {
            public val Create: String
                get() = YumeLocaleManager.activeStrings.Override.Action.Create
            @androidx.compose.runtime.Composable
            public fun Create(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Action.Create else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Action.Create, *args)
            public val New: String
                get() = YumeLocaleManager.activeStrings.Override.Action.New
            @androidx.compose.runtime.Composable
            public fun New(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Action.New else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Action.New, *args)
            public val NetworkImport: String
                get() = YumeLocaleManager.activeStrings.Override.Action.NetworkImport
            @androidx.compose.runtime.Composable
            public fun NetworkImport(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Action.NetworkImport else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Action.NetworkImport, *args)
        }
        public object BuiltIn {
            public val PreventDnsLeak: String
                get() = YumeLocaleManager.activeStrings.Override.BuiltIn.PreventDnsLeak
            @androidx.compose.runtime.Composable
            public fun PreventDnsLeak(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.BuiltIn.PreventDnsLeak else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.BuiltIn.PreventDnsLeak, *args)
            public val AddDirectRules: String
                get() = YumeLocaleManager.activeStrings.Override.BuiltIn.AddDirectRules
            @androidx.compose.runtime.Composable
            public fun AddDirectRules(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.BuiltIn.AddDirectRules else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.BuiltIn.AddDirectRules, *args)
            public val PuddingDog: String
                get() = YumeLocaleManager.activeStrings.Override.BuiltIn.PuddingDog
            @androidx.compose.runtime.Composable
            public fun PuddingDog(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.BuiltIn.PuddingDog else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.BuiltIn.PuddingDog, *args)
            public val Acl4ssrOnlineFull: String
                get() = YumeLocaleManager.activeStrings.Override.BuiltIn.Acl4ssrOnlineFull
            @androidx.compose.runtime.Composable
            public fun Acl4ssrOnlineFull(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.BuiltIn.Acl4ssrOnlineFull else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.BuiltIn.Acl4ssrOnlineFull, *args)
            public val CopyName: String
                get() = YumeLocaleManager.activeStrings.Override.BuiltIn.CopyName
            @androidx.compose.runtime.Composable
            public fun CopyName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.BuiltIn.CopyName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.BuiltIn.CopyName, *args)
        }
        public object Empty {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Override.Empty.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Empty.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Empty.Title, *args)
            public val Hint: String
                get() = YumeLocaleManager.activeStrings.Override.Empty.Hint
            @androidx.compose.runtime.Composable
            public fun Hint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Empty.Hint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Empty.Hint, *args)
            public val UserTitle: String
                get() = YumeLocaleManager.activeStrings.Override.Empty.UserTitle
            @androidx.compose.runtime.Composable
            public fun UserTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Empty.UserTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Empty.UserTitle, *args)
            public val UserHint: String
                get() = YumeLocaleManager.activeStrings.Override.Empty.UserHint
            @androidx.compose.runtime.Composable
            public fun UserHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Empty.UserHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Empty.UserHint, *args)
        }
        public object Section {
            public val BuiltIn: String
                get() = YumeLocaleManager.activeStrings.Override.Section.BuiltIn
            @androidx.compose.runtime.Composable
            public fun BuiltIn(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Section.BuiltIn else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Section.BuiltIn, *args)
            public val User: String
                get() = YumeLocaleManager.activeStrings.Override.Section.User
            @androidx.compose.runtime.Composable
            public fun User(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Section.User else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Section.User, *args)
        }
        public object Status {
            public val InUse: String
                get() = YumeLocaleManager.activeStrings.Override.Status.InUse
            @androidx.compose.runtime.Composable
            public fun InUse(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Status.InUse else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Status.InUse, *args)
            public val NotInUse: String
                get() = YumeLocaleManager.activeStrings.Override.Status.NotInUse
            @androidx.compose.runtime.Composable
            public fun NotInUse(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Status.NotInUse else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Status.NotInUse, *args)
            public val BuiltIn: String
                get() = YumeLocaleManager.activeStrings.Override.Status.BuiltIn
            @androidx.compose.runtime.Composable
            public fun BuiltIn(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Status.BuiltIn else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Status.BuiltIn, *args)
        }
        public object Card {
            public val Copy: String
                get() = YumeLocaleManager.activeStrings.Override.Card.Copy
            @androidx.compose.runtime.Composable
            public fun Copy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.Copy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.Copy, *args)
            public val Export: String
                get() = YumeLocaleManager.activeStrings.Override.Card.Export
            @androidx.compose.runtime.Composable
            public fun Export(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.Export else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.Export, *args)
            public val Edit: String
                get() = YumeLocaleManager.activeStrings.Override.Card.Edit
            @androidx.compose.runtime.Composable
            public fun Edit(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.Edit else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.Edit, *args)
            public val Delete: String
                get() = YumeLocaleManager.activeStrings.Override.Card.Delete
            @androidx.compose.runtime.Composable
            public fun Delete(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.Delete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.Delete, *args)
            public val EditButton: String
                get() = YumeLocaleManager.activeStrings.Override.Card.EditButton
            @androidx.compose.runtime.Composable
            public fun EditButton(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.EditButton else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.EditButton, *args)
            public val DeleteButton: String
                get() = YumeLocaleManager.activeStrings.Override.Card.DeleteButton
            @androidx.compose.runtime.Composable
            public fun DeleteButton(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.DeleteButton else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.DeleteButton, *args)
            public val Apply: String
                get() = YumeLocaleManager.activeStrings.Override.Card.Apply
            @androidx.compose.runtime.Composable
            public fun Apply(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.Apply else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.Apply, *args)
            public val ApplyButton: String
                get() = YumeLocaleManager.activeStrings.Override.Card.ApplyButton
            @androidx.compose.runtime.Composable
            public fun ApplyButton(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Card.ApplyButton else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Card.ApplyButton, *args)
        }
        public object ApplySheet {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.Override.ApplySheet.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.ApplySheet.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.ApplySheet.Title, *args)
            public val Empty: String
                get() = YumeLocaleManager.activeStrings.Override.ApplySheet.Empty
            @androidx.compose.runtime.Composable
            public fun Empty(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.ApplySheet.Empty else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.ApplySheet.Empty, *args)
            public val Success: String
                get() = YumeLocaleManager.activeStrings.Override.ApplySheet.Success
            @androidx.compose.runtime.Composable
            public fun Success(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.ApplySheet.Success else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.ApplySheet.Success, *args)
            public val Failed: String
                get() = YumeLocaleManager.activeStrings.Override.ApplySheet.Failed
            @androidx.compose.runtime.Composable
            public fun Failed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.ApplySheet.Failed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.ApplySheet.Failed, *args)
            public object Button {
                public val Cancel: String
                    get() = YumeLocaleManager.activeStrings.Override.ApplySheet.Button.Cancel
                @androidx.compose.runtime.Composable
                public fun Cancel(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.ApplySheet.Button.Cancel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.ApplySheet.Button.Cancel, *args)
                public val Confirm: String
                    get() = YumeLocaleManager.activeStrings.Override.ApplySheet.Button.Confirm
                @androidx.compose.runtime.Composable
                public fun Confirm(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.ApplySheet.Button.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.ApplySheet.Button.Confirm, *args)
            }
        }
        public object Import {
            public val ReadError: String
                get() = YumeLocaleManager.activeStrings.Override.Import.ReadError
            @androidx.compose.runtime.Composable
            public fun ReadError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.ReadError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.ReadError, *args)
            public val Failed: String
                get() = YumeLocaleManager.activeStrings.Override.Import.Failed
            @androidx.compose.runtime.Composable
            public fun Failed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.Failed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.Failed, *args)
            public val FileError: String
                get() = YumeLocaleManager.activeStrings.Override.Import.FileError
            @androidx.compose.runtime.Composable
            public fun FileError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.FileError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.FileError, *args)
            public val NetworkError: String
                get() = YumeLocaleManager.activeStrings.Override.Import.NetworkError
            @androidx.compose.runtime.Composable
            public fun NetworkError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.NetworkError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.NetworkError, *args)
            public val InvalidUrl: String
                get() = YumeLocaleManager.activeStrings.Override.Import.InvalidUrl
            @androidx.compose.runtime.Composable
            public fun InvalidUrl(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.InvalidUrl else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.InvalidUrl, *args)
            public val HttpError: String
                get() = YumeLocaleManager.activeStrings.Override.Import.HttpError
            @androidx.compose.runtime.Composable
            public fun HttpError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.HttpError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.HttpError, *args)
            public val UnsupportedType: String
                get() = YumeLocaleManager.activeStrings.Override.Import.UnsupportedType
            @androidx.compose.runtime.Composable
            public fun UnsupportedType(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.UnsupportedType else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.UnsupportedType, *args)
            public val EmptyJavaScript: String
                get() = YumeLocaleManager.activeStrings.Override.Import.EmptyJavaScript
            @androidx.compose.runtime.Composable
            public fun EmptyJavaScript(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Import.EmptyJavaScript else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Import.EmptyJavaScript, *args)
        }
        public object Export {
            public val Failed: String
                get() = YumeLocaleManager.activeStrings.Override.Export.Failed
            @androidx.compose.runtime.Composable
            public fun Failed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Export.Failed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Export.Failed, *args)
            public val Success: String
                get() = YumeLocaleManager.activeStrings.Override.Export.Success
            @androidx.compose.runtime.Composable
            public fun Success(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Export.Success else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Export.Success, *args)
        }
        public object Dialog {
            public object Create {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Create.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Create.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Create.Title, *args)
                public val Name: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Create.Name
                @androidx.compose.runtime.Composable
                public fun Name(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Create.Name else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Create.Name, *args)
                public val Url: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Create.Url
                @androidx.compose.runtime.Composable
                public fun Url(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Create.Url else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Create.Url, *args)
                public val Type: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Create.Type
                @androidx.compose.runtime.Composable
                public fun Type(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Create.Type else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Create.Type, *args)
            }
            public object Delete {
                public val Title: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Delete.Title
                @androidx.compose.runtime.Composable
                public fun Title(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Delete.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Delete.Title, *args)
                public val InUseMessage: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Delete.InUseMessage
                @androidx.compose.runtime.Composable
                public fun InUseMessage(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Delete.InUseMessage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Delete.InUseMessage, *args)
                public val Message: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Delete.Message
                @androidx.compose.runtime.Composable
                public fun Message(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Delete.Message else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Delete.Message, *args)
            }
            public object Button {
                public val Cancel: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Button.Cancel
                @androidx.compose.runtime.Composable
                public fun Cancel(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Button.Cancel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Button.Cancel, *args)
                public val Delete: String
                    get() = YumeLocaleManager.activeStrings.Override.Dialog.Button.Delete
                @androidx.compose.runtime.Composable
                public fun Delete(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Dialog.Button.Delete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Dialog.Button.Delete, *args)
            }
        }
        public object Draft {
            public val BasicRouting: String
                get() = YumeLocaleManager.activeStrings.Override.Draft.BasicRouting
            @androidx.compose.runtime.Composable
            public fun BasicRouting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Draft.BasicRouting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Draft.BasicRouting, *args)
            public val ServiceRouting: String
                get() = YumeLocaleManager.activeStrings.Override.Draft.ServiceRouting
            @androidx.compose.runtime.Composable
            public fun ServiceRouting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Draft.ServiceRouting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Draft.ServiceRouting, *args)
        }
        public object Save {
            public val ImportDefaultName: String
                get() = YumeLocaleManager.activeStrings.Override.Save.ImportDefaultName
            @androidx.compose.runtime.Composable
            public fun ImportDefaultName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Save.ImportDefaultName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Save.ImportDefaultName, *args)
            public val Failed: String
                get() = YumeLocaleManager.activeStrings.Override.Save.Failed
            @androidx.compose.runtime.Composable
            public fun Failed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Save.Failed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Save.Failed, *args)
        }
        public object Label {
            public val RulesReplace: String
                get() = YumeLocaleManager.activeStrings.Override.Label.RulesReplace
            @androidx.compose.runtime.Composable
            public fun RulesReplace(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Override.Label.RulesReplace else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Override.Label.RulesReplace, *args)
        }
    }
    public object ProfilesPage {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.ProfilesPage.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Title, *args)
        public object Action {
            public val UpdateAll: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Action.UpdateAll
            @androidx.compose.runtime.Composable
            public fun UpdateAll(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Action.UpdateAll else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Action.UpdateAll, *args)
            public val AddProfile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Action.AddProfile
            @androidx.compose.runtime.Composable
            public fun AddProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Action.AddProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Action.AddProfile, *args)
        }
        public object Empty {
            public val NoProfiles: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Empty.NoProfiles
            @androidx.compose.runtime.Composable
            public fun NoProfiles(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Empty.NoProfiles else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Empty.NoProfiles, *args)
            public val Hint: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Empty.Hint
            @androidx.compose.runtime.Composable
            public fun Hint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Empty.Hint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Empty.Hint, *args)
        }
        public object Sheet {
            public val AddTitle: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Sheet.AddTitle
            @androidx.compose.runtime.Composable
            public fun AddTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Sheet.AddTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Sheet.AddTitle, *args)
            public val EditTitle: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Sheet.EditTitle
            @androidx.compose.runtime.Composable
            public fun EditTitle(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Sheet.EditTitle else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Sheet.EditTitle, *args)
            public val Complete: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Sheet.Complete
            @androidx.compose.runtime.Composable
            public fun Complete(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Sheet.Complete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Sheet.Complete, *args)
        }
        public object Type {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Type.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Type.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Type.Title, *args)
            public val Subscription: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Type.Subscription
            @androidx.compose.runtime.Composable
            public fun Subscription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Type.Subscription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Type.Subscription, *args)
            public val LocalFile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Type.LocalFile
            @androidx.compose.runtime.Composable
            public fun LocalFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Type.LocalFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Type.LocalFile, *args)
            public val QrScan: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Type.QrScan
            @androidx.compose.runtime.Composable
            public fun QrScan(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Type.QrScan else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Type.QrScan, *args)
        }
        public object Input {
            public val ProfileName: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Input.ProfileName
            @androidx.compose.runtime.Composable
            public fun ProfileName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Input.ProfileName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Input.ProfileName, *args)
            public val SubscriptionUrl: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Input.SubscriptionUrl
            @androidx.compose.runtime.Composable
            public fun SubscriptionUrl(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Input.SubscriptionUrl else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Input.SubscriptionUrl, *args)
            public val SelectFile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Input.SelectFile
            @androidx.compose.runtime.Composable
            public fun SelectFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Input.SelectFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Input.SelectFile, *args)
            public val NewProfile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Input.NewProfile
            @androidx.compose.runtime.Composable
            public fun NewProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Input.NewProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Input.NewProfile, *args)
            public val AgeSecretKey: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Input.AgeSecretKey
            @androidx.compose.runtime.Composable
            public fun AgeSecretKey(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Input.AgeSecretKey else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Input.AgeSecretKey, *args)
        }
        public object QrScanner {
            public val NeedPermission: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.NeedPermission
            @androidx.compose.runtime.Composable
            public fun NeedPermission(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.NeedPermission else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.NeedPermission, *args)
            public val NeedCamera: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.NeedCamera
            @androidx.compose.runtime.Composable
            public fun NeedCamera(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.NeedCamera else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.NeedCamera, *args)
            public val SelectFromAlbum: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.SelectFromAlbum
            @androidx.compose.runtime.Composable
            public fun SelectFromAlbum(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.SelectFromAlbum else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.SelectFromAlbum, *args)
            public val RecognizeSuccess: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeSuccess
            @androidx.compose.runtime.Composable
            public fun RecognizeSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeSuccess, *args)
            public val RecognizeFailed: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeFailed
            @androidx.compose.runtime.Composable
            public fun RecognizeFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeFailed, *args)
            public val RecognizeError: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeError
            @androidx.compose.runtime.Composable
            public fun RecognizeError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.QrScanner.RecognizeError, *args)
        }
        public object Message {
            public val UnknownFile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Message.UnknownFile
            @androidx.compose.runtime.Composable
            public fun UnknownFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Message.UnknownFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Message.UnknownFile, *args)
        }
        public object Validation {
            public val EnterUrl: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Validation.EnterUrl
            @androidx.compose.runtime.Composable
            public fun EnterUrl(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Validation.EnterUrl else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Validation.EnterUrl, *args)
            public val SelectFile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Validation.SelectFile
            @androidx.compose.runtime.Composable
            public fun SelectFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Validation.SelectFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Validation.SelectFile, *args)
            public val YamlOnly: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Validation.YamlOnly
            @androidx.compose.runtime.Composable
            public fun YamlOnly(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Validation.YamlOnly else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Validation.YamlOnly, *args)
        }
        public object Progress {
            public val Downloading: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Progress.Downloading
            @androidx.compose.runtime.Composable
            public fun Downloading(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Progress.Downloading else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Progress.Downloading, *args)
        }
        public object Button {
            public val Cancel: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Button.Cancel
            @androidx.compose.runtime.Composable
            public fun Cancel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Button.Cancel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Button.Cancel, *args)
            public val Confirm: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.Button.Confirm
            @androidx.compose.runtime.Composable
            public fun Confirm(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.Button.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.Button.Confirm, *args)
        }
        public object DeleteDialog {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Title, *args)
            public val Message: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Message
            @androidx.compose.runtime.Composable
            public fun Message(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Message else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Message, *args)
            public val Confirm: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Confirm
            @androidx.compose.runtime.Composable
            public fun Confirm(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Confirm else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.DeleteDialog.Confirm, *args)
        }
        public object EditDialog {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.EditDialog.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.EditDialog.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.EditDialog.Title, *args)
        }
        public object LinkSettings {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Title, *args)
            public val OpenMode: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenMode
            @androidx.compose.runtime.Composable
            public fun OpenMode(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenMode else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenMode, *args)
            public val OpenModeInApp: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenModeInApp
            @androidx.compose.runtime.Composable
            public fun OpenModeInApp(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenModeInApp else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenModeInApp, *args)
            public val OpenModeExternal: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenModeExternal
            @androidx.compose.runtime.Composable
            public fun OpenModeExternal(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenModeExternal else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.OpenModeExternal, *args)
            public val DefaultLink: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.DefaultLink
            @androidx.compose.runtime.Composable
            public fun DefaultLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.DefaultLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.DefaultLink, *args)
            public val DefaultLinkSummary: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.DefaultLinkSummary
            @androidx.compose.runtime.Composable
            public fun DefaultLinkSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.DefaultLinkSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.DefaultLinkSummary, *args)
            public val AddLink: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.AddLink
            @androidx.compose.runtime.Composable
            public fun AddLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.AddLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.AddLink, *args)
            public val EditLink: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.EditLink
            @androidx.compose.runtime.Composable
            public fun EditLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.EditLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.EditLink, *args)
            public val Name: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Name
            @androidx.compose.runtime.Composable
            public fun Name(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Name else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Name, *args)
            public val Url: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Url
            @androidx.compose.runtime.Composable
            public fun Url(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Url else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Url, *args)
            public val Close: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Close
            @androidx.compose.runtime.Composable
            public fun Close(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Close else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Close, *args)
            public object Validation {
                public val EnterName: String
                    get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.EnterName
                @androidx.compose.runtime.Composable
                public fun EnterName(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.EnterName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.EnterName, *args)
                public val EnterUrl: String
                    get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.EnterUrl
                @androidx.compose.runtime.Composable
                public fun EnterUrl(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.EnterUrl else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.EnterUrl, *args)
                public val InvalidUrl: String
                    get() = YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.InvalidUrl
                @androidx.compose.runtime.Composable
                public fun InvalidUrl(vararg args: Any?): String =
                    if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.InvalidUrl else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.LinkSettings.Validation.InvalidUrl, *args)
            }
        }
        public object ShareDialog {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.Title, *args)
            public val ShareFile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ShareFile
            @androidx.compose.runtime.Composable
            public fun ShareFile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ShareFile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ShareFile, *args)
            public val ShareLink: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ShareLink
            @androidx.compose.runtime.Composable
            public fun ShareLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ShareLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ShareLink, *args)
            public val NoLink: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.NoLink
            @androidx.compose.runtime.Composable
            public fun NoLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.NoLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.NoLink, *args)
            public val ImportedConfigMissing: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ImportedConfigMissing
            @androidx.compose.runtime.Composable
            public fun ImportedConfigMissing(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ImportedConfigMissing else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.ShareDialog.ImportedConfigMissing, *args)
        }
        public object SettingsDialog {
            public val Title: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.Title
            @androidx.compose.runtime.Composable
            public fun Title(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.Title, *args)
            public val ChangeLink: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.ChangeLink
            @androidx.compose.runtime.Composable
            public fun ChangeLink(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.ChangeLink else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.ChangeLink, *args)
            public val CustomRouting: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.CustomRouting
            @androidx.compose.runtime.Composable
            public fun CustomRouting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.CustomRouting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.CustomRouting, *args)
            public val CustomRoutingSummary: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.CustomRoutingSummary
            @androidx.compose.runtime.Composable
            public fun CustomRoutingSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.CustomRoutingSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.CustomRoutingSummary, *args)
            public val NoDescription: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.NoDescription
            @androidx.compose.runtime.Composable
            public fun NoDescription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.NoDescription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.NoDescription, *args)
            public val EditProfile: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.EditProfile
            @androidx.compose.runtime.Composable
            public fun EditProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.EditProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.EditProfile, *args)
            public val OpenConfig: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.OpenConfig
            @androidx.compose.runtime.Composable
            public fun OpenConfig(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.OpenConfig else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.OpenConfig, *args)
            public val EditSettings: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.EditSettings
            @androidx.compose.runtime.Composable
            public fun EditSettings(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.EditSettings else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.EditSettings, *args)
            public val SectionType: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionType
            @androidx.compose.runtime.Composable
            public fun SectionType(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionType else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionType, *args)
            public val SectionSubscription: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionSubscription
            @androidx.compose.runtime.Composable
            public fun SectionSubscription(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionSubscription else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionSubscription, *args)
            public val SectionOverride: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionOverride
            @androidx.compose.runtime.Composable
            public fun SectionOverride(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionOverride else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SectionOverride, *args)
            public val SaveFailed: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SaveFailed
            @androidx.compose.runtime.Composable
            public fun SaveFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SaveFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.SaveFailed, *args)
            public val ConfigMissing: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.ConfigMissing
            @androidx.compose.runtime.Composable
            public fun ConfigMissing(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.ConfigMissing else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.ConfigMissing, *args)
            public val AgeSecretKey: String
                get() = YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.AgeSecretKey
            @androidx.compose.runtime.Composable
            public fun AgeSecretKey(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.AgeSecretKey else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesPage.SettingsDialog.AgeSecretKey, *args)
        }
    }
    public object ProfilesVM {
        public object Message {
            public val ProfileAdded: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileAdded
            @androidx.compose.runtime.Composable
            public fun ProfileAdded(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileAdded else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileAdded, *args)
            public val AddFailed: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.AddFailed
            @androidx.compose.runtime.Composable
            public fun AddFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.AddFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.AddFailed, *args)
            public val ProfileDeleted: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileDeleted
            @androidx.compose.runtime.Composable
            public fun ProfileDeleted(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileDeleted else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileDeleted, *args)
            public val DeleteFailed: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.DeleteFailed
            @androidx.compose.runtime.Composable
            public fun DeleteFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.DeleteFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.DeleteFailed, *args)
            public val ProfileUpdated: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileUpdated
            @androidx.compose.runtime.Composable
            public fun ProfileUpdated(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileUpdated else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.ProfileUpdated, *args)
            public val UpdateFailed: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.UpdateFailed
            @androidx.compose.runtime.Composable
            public fun UpdateFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.UpdateFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.UpdateFailed, *args)
            public val ToggleFailed: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.ToggleFailed
            @androidx.compose.runtime.Composable
            public fun ToggleFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.ToggleFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.ToggleFailed, *args)
            public val ProvidersPartial: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.ProvidersPartial
            @androidx.compose.runtime.Composable
            public fun ProvidersPartial(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.ProvidersPartial else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.ProvidersPartial, *args)
            public val ProvidersUndiscovered: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Message.ProvidersUndiscovered
            @androidx.compose.runtime.Composable
            public fun ProvidersUndiscovered(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Message.ProvidersUndiscovered else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Message.ProvidersUndiscovered, *args)
        }
        public object Progress {
            public val Preparing: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Progress.Preparing
            @androidx.compose.runtime.Composable
            public fun Preparing(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Progress.Preparing else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Progress.Preparing, *args)
            public val Verifying: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Progress.Verifying
            @androidx.compose.runtime.Composable
            public fun Verifying(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Progress.Verifying else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Progress.Verifying, *args)
            public val ImportComplete: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Progress.ImportComplete
            @androidx.compose.runtime.Composable
            public fun ImportComplete(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Progress.ImportComplete else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Progress.ImportComplete, *args)
        }
        public object Error {
            public val ProfileNotExist: String
                get() = YumeLocaleManager.activeStrings.ProfilesVM.Error.ProfileNotExist
            @androidx.compose.runtime.Composable
            public fun ProfileNotExist(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.ProfilesVM.Error.ProfileNotExist else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.ProfilesVM.Error.ProfileNotExist, *args)
        }
    }
    public object Providers {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Providers.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Title, *args)
        public object Action {
            public val UpdateAll: String
                get() = YumeLocaleManager.activeStrings.Providers.Action.UpdateAll
            @androidx.compose.runtime.Composable
            public fun UpdateAll(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Action.UpdateAll else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Action.UpdateAll, *args)
            public val Update: String
                get() = YumeLocaleManager.activeStrings.Providers.Action.Update
            @androidx.compose.runtime.Composable
            public fun Update(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Action.Update else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Action.Update, *args)
            public val Upload: String
                get() = YumeLocaleManager.activeStrings.Providers.Action.Upload
            @androidx.compose.runtime.Composable
            public fun Upload(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Action.Upload else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Action.Upload, *args)
            public val Operation: String
                get() = YumeLocaleManager.activeStrings.Providers.Action.Operation
            @androidx.compose.runtime.Composable
            public fun Operation(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Action.Operation else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Action.Operation, *args)
        }
        public object Empty {
            public val NotRunning: String
                get() = YumeLocaleManager.activeStrings.Providers.Empty.NotRunning
            @androidx.compose.runtime.Composable
            public fun NotRunning(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Empty.NotRunning else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Empty.NotRunning, *args)
            public val NotRunningHint: String
                get() = YumeLocaleManager.activeStrings.Providers.Empty.NotRunningHint
            @androidx.compose.runtime.Composable
            public fun NotRunningHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Empty.NotRunningHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Empty.NotRunningHint, *args)
            public val NoProviders: String
                get() = YumeLocaleManager.activeStrings.Providers.Empty.NoProviders
            @androidx.compose.runtime.Composable
            public fun NoProviders(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Empty.NoProviders else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Empty.NoProviders, *args)
            public val NoProvidersHint: String
                get() = YumeLocaleManager.activeStrings.Providers.Empty.NoProvidersHint
            @androidx.compose.runtime.Composable
            public fun NoProvidersHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Empty.NoProvidersHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Empty.NoProvidersHint, *args)
        }
        public object Type {
            public val ProxyProviders: String
                get() = YumeLocaleManager.activeStrings.Providers.Type.ProxyProviders
            @androidx.compose.runtime.Composable
            public fun ProxyProviders(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Type.ProxyProviders else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Type.ProxyProviders, *args)
            public val RuleProviders: String
                get() = YumeLocaleManager.activeStrings.Providers.Type.RuleProviders
            @androidx.compose.runtime.Composable
            public fun RuleProviders(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Type.RuleProviders else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Type.RuleProviders, *args)
        }
        public object VehicleType {
            public val Http: String
                get() = YumeLocaleManager.activeStrings.Providers.VehicleType.Http
            @androidx.compose.runtime.Composable
            public fun Http(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.VehicleType.Http else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.VehicleType.Http, *args)
            public val File: String
                get() = YumeLocaleManager.activeStrings.Providers.VehicleType.File
            @androidx.compose.runtime.Composable
            public fun File(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.VehicleType.File else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.VehicleType.File, *args)
            public val Inline: String
                get() = YumeLocaleManager.activeStrings.Providers.VehicleType.Inline
            @androidx.compose.runtime.Composable
            public fun Inline(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.VehicleType.Inline else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.VehicleType.Inline, *args)
            public val Compatible: String
                get() = YumeLocaleManager.activeStrings.Providers.VehicleType.Compatible
            @androidx.compose.runtime.Composable
            public fun Compatible(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.VehicleType.Compatible else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.VehicleType.Compatible, *args)
        }
        public object Message {
            public val FetchFailed: String
                get() = YumeLocaleManager.activeStrings.Providers.Message.FetchFailed
            @androidx.compose.runtime.Composable
            public fun FetchFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Message.FetchFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Message.FetchFailed, *args)
            public val UpdateSuccess: String
                get() = YumeLocaleManager.activeStrings.Providers.Message.UpdateSuccess
            @androidx.compose.runtime.Composable
            public fun UpdateSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Message.UpdateSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Message.UpdateSuccess, *args)
            public val UpdateFailed: String
                get() = YumeLocaleManager.activeStrings.Providers.Message.UpdateFailed
            @androidx.compose.runtime.Composable
            public fun UpdateFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Message.UpdateFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Message.UpdateFailed, *args)
            public val AllUpdated: String
                get() = YumeLocaleManager.activeStrings.Providers.Message.AllUpdated
            @androidx.compose.runtime.Composable
            public fun AllUpdated(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Message.AllUpdated else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Message.AllUpdated, *args)
            public val UploadSuccess: String
                get() = YumeLocaleManager.activeStrings.Providers.Message.UploadSuccess
            @androidx.compose.runtime.Composable
            public fun UploadSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Message.UploadSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Message.UploadSuccess, *args)
            public val UploadFailed: String
                get() = YumeLocaleManager.activeStrings.Providers.Message.UploadFailed
            @androidx.compose.runtime.Composable
            public fun UploadFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Providers.Message.UploadFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Providers.Message.UploadFailed, *args)
        }
    }
    public object Proxy {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Proxy.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Title, *args)
        public object Mode {
            public val Direct: String
                get() = YumeLocaleManager.activeStrings.Proxy.Mode.Direct
            @androidx.compose.runtime.Composable
            public fun Direct(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Mode.Direct else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Mode.Direct, *args)
        }
        public object Action {
            public val Panel: String
                get() = YumeLocaleManager.activeStrings.Proxy.Action.Panel
            @androidx.compose.runtime.Composable
            public fun Panel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Action.Panel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Action.Panel, *args)
            public val Test: String
                get() = YumeLocaleManager.activeStrings.Proxy.Action.Test
            @androidx.compose.runtime.Composable
            public fun Test(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Action.Test else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Action.Test, *args)
            public val Sort: String
                get() = YumeLocaleManager.activeStrings.Proxy.Action.Sort
            @androidx.compose.runtime.Composable
            public fun Sort(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Action.Sort else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Action.Sort, *args)
            public val LocateCurrent: String
                get() = YumeLocaleManager.activeStrings.Proxy.Action.LocateCurrent
            @androidx.compose.runtime.Composable
            public fun LocateCurrent(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Action.LocateCurrent else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Action.LocateCurrent, *args)
            public val More: String
                get() = YumeLocaleManager.activeStrings.Proxy.Action.More
            @androidx.compose.runtime.Composable
            public fun More(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Action.More else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Action.More, *args)
        }
        public object Node {
            public val Timeout: String
                get() = YumeLocaleManager.activeStrings.Proxy.Node.Timeout
            @androidx.compose.runtime.Composable
            public fun Timeout(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Node.Timeout else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Node.Timeout, *args)
            public val Count: String
                get() = YumeLocaleManager.activeStrings.Proxy.Node.Count
            @androidx.compose.runtime.Composable
            public fun Count(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Node.Count else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Node.Count, *args)
        }
        public object Empty {
            public val NoNodes: String
                get() = YumeLocaleManager.activeStrings.Proxy.Empty.NoNodes
            @androidx.compose.runtime.Composable
            public fun NoNodes(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Empty.NoNodes else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Empty.NoNodes, *args)
            public val Hint: String
                get() = YumeLocaleManager.activeStrings.Proxy.Empty.Hint
            @androidx.compose.runtime.Composable
            public fun Hint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Empty.Hint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Empty.Hint, *args)
        }
        public object Testing {
            public val Group: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.Group
            @androidx.compose.runtime.Composable
            public fun Group(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.Group else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.Group, *args)
            public val All: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.All
            @androidx.compose.runtime.Composable
            public fun All(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.All else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.All, *args)
            public val RequestSent: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.RequestSent
            @androidx.compose.runtime.Composable
            public fun RequestSent(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.RequestSent else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.RequestSent, *args)
            public val Progress: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.Progress
            @androidx.compose.runtime.Composable
            public fun Progress(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.Progress else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.Progress, *args)
            public val Completed: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.Completed
            @androidx.compose.runtime.Composable
            public fun Completed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.Completed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.Completed, *args)
            public val Failed: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.Failed
            @androidx.compose.runtime.Composable
            public fun Failed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.Failed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.Failed, *args)
            public val InProgress: String
                get() = YumeLocaleManager.activeStrings.Proxy.Testing.InProgress
            @androidx.compose.runtime.Composable
            public fun InProgress(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Testing.InProgress else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Testing.InProgress, *args)
        }
        public object Selection {
            public val Switched: String
                get() = YumeLocaleManager.activeStrings.Proxy.Selection.Switched
            @androidx.compose.runtime.Composable
            public fun Switched(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Selection.Switched else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Selection.Switched, *args)
            public val Failed: String
                get() = YumeLocaleManager.activeStrings.Proxy.Selection.Failed
            @androidx.compose.runtime.Composable
            public fun Failed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Selection.Failed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Selection.Failed, *args)
            public val Error: String
                get() = YumeLocaleManager.activeStrings.Proxy.Selection.Error
            @androidx.compose.runtime.Composable
            public fun Error(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.Selection.Error else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.Selection.Error, *args)
        }
        public object SortMode {
            public val Default: String
                get() = YumeLocaleManager.activeStrings.Proxy.SortMode.Default
            @androidx.compose.runtime.Composable
            public fun Default(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.SortMode.Default else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.SortMode.Default, *args)
            public val ByName: String
                get() = YumeLocaleManager.activeStrings.Proxy.SortMode.ByName
            @androidx.compose.runtime.Composable
            public fun ByName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.SortMode.ByName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.SortMode.ByName, *args)
            public val ByLatency: String
                get() = YumeLocaleManager.activeStrings.Proxy.SortMode.ByLatency
            @androidx.compose.runtime.Composable
            public fun ByLatency(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.SortMode.ByLatency else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.SortMode.ByLatency, *args)
        }
        public object DisplayMode {
            public val SingleDetailed: String
                get() = YumeLocaleManager.activeStrings.Proxy.DisplayMode.SingleDetailed
            @androidx.compose.runtime.Composable
            public fun SingleDetailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.DisplayMode.SingleDetailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.DisplayMode.SingleDetailed, *args)
            public val SingleSimple: String
                get() = YumeLocaleManager.activeStrings.Proxy.DisplayMode.SingleSimple
            @androidx.compose.runtime.Composable
            public fun SingleSimple(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.DisplayMode.SingleSimple else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.DisplayMode.SingleSimple, *args)
            public val DoubleDetailed: String
                get() = YumeLocaleManager.activeStrings.Proxy.DisplayMode.DoubleDetailed
            @androidx.compose.runtime.Composable
            public fun DoubleDetailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.DisplayMode.DoubleDetailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.DisplayMode.DoubleDetailed, *args)
            public val DoubleSimple: String
                get() = YumeLocaleManager.activeStrings.Proxy.DisplayMode.DoubleSimple
            @androidx.compose.runtime.Composable
            public fun DoubleSimple(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Proxy.DisplayMode.DoubleSimple else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Proxy.DisplayMode.DoubleSimple, *args)
        }
    }
    public object Rules {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Rules.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Title, *args)
        public val Summary: String
            get() = YumeLocaleManager.activeStrings.Rules.Summary
        @androidx.compose.runtime.Composable
        public fun Summary(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Summary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Summary, *args)
        public val SearchHint: String
            get() = YumeLocaleManager.activeStrings.Rules.SearchHint
        @androidx.compose.runtime.Composable
        public fun SearchHint(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.SearchHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.SearchHint, *args)
        public object Label {
            public val Proxy: String
                get() = YumeLocaleManager.activeStrings.Rules.Label.Proxy
            @androidx.compose.runtime.Composable
            public fun Proxy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Label.Proxy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Label.Proxy, *args)
            public val HitRate: String
                get() = YumeLocaleManager.activeStrings.Rules.Label.HitRate
            @androidx.compose.runtime.Composable
            public fun HitRate(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Label.HitRate else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Label.HitRate, *args)
        }
        public object Message {
            public val ToggleFailed: String
                get() = YumeLocaleManager.activeStrings.Rules.Message.ToggleFailed
            @androidx.compose.runtime.Composable
            public fun ToggleFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Message.ToggleFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Message.ToggleFailed, *args)
        }
        public object Empty {
            public val Loading: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.Loading
            @androidx.compose.runtime.Composable
            public fun Loading(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.Loading else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.Loading, *args)
            public val LoadingHint: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.LoadingHint
            @androidx.compose.runtime.Composable
            public fun LoadingHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.LoadingHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.LoadingHint, *args)
            public val NotRunning: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.NotRunning
            @androidx.compose.runtime.Composable
            public fun NotRunning(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.NotRunning else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.NotRunning, *args)
            public val NotRunningHint: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.NotRunningHint
            @androidx.compose.runtime.Composable
            public fun NotRunningHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.NotRunningHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.NotRunningHint, *args)
            public val NoRules: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.NoRules
            @androidx.compose.runtime.Composable
            public fun NoRules(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.NoRules else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.NoRules, *args)
            public val NoRulesHint: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.NoRulesHint
            @androidx.compose.runtime.Composable
            public fun NoRulesHint(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.NoRulesHint else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.NoRulesHint, *args)
            public val NoResults: String
                get() = YumeLocaleManager.activeStrings.Rules.Empty.NoResults
            @androidx.compose.runtime.Composable
            public fun NoResults(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Rules.Empty.NoResults else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Rules.Empty.NoResults, *args)
        }
    }
    public object Service {
        public object Notification {
            public val Running: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.Running
            @androidx.compose.runtime.Composable
            public fun Running(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.Running else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.Running, *args)
            public val UnknownProfile: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.UnknownProfile
            @androidx.compose.runtime.Composable
            public fun UnknownProfile(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.UnknownProfile else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.UnknownProfile, *args)
            public val NoNode: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.NoNode
            @androidx.compose.runtime.Composable
            public fun NoNode(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.NoNode else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.NoNode, *args)
            public val CurrentNode: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.CurrentNode
            @androidx.compose.runtime.Composable
            public fun CurrentNode(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.CurrentNode else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.CurrentNode, *args)
            public val CurrentNodeLabel: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.CurrentNodeLabel
            @androidx.compose.runtime.Composable
            public fun CurrentNodeLabel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.CurrentNodeLabel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.CurrentNodeLabel, *args)
            public val RealtimeTraffic: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.RealtimeTraffic
            @androidx.compose.runtime.Composable
            public fun RealtimeTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.RealtimeTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.RealtimeTraffic, *args)
            public val SpeedLine: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.SpeedLine
            @androidx.compose.runtime.Composable
            public fun SpeedLine(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.SpeedLine else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.SpeedLine, *args)
            public val TotalTraffic: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.TotalTraffic
            @androidx.compose.runtime.Composable
            public fun TotalTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.TotalTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.TotalTraffic, *args)
            public val LogChannel: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.LogChannel
            @androidx.compose.runtime.Composable
            public fun LogChannel(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.LogChannel else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.LogChannel, *args)
            public val LogRecording: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.LogRecording
            @androidx.compose.runtime.Composable
            public fun LogRecording(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.LogRecording else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.LogRecording, *args)
            public val LogRecordingPending: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.LogRecordingPending
            @androidx.compose.runtime.Composable
            public fun LogRecordingPending(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.LogRecordingPending else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.LogRecordingPending, *args)
            public val Stop: String
                get() = YumeLocaleManager.activeStrings.Service.Notification.Stop
            @androidx.compose.runtime.Composable
            public fun Stop(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Notification.Stop else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Notification.Stop, *args)
        }
        public object Tile {
            public val ClickToOpen: String
                get() = YumeLocaleManager.activeStrings.Service.Tile.ClickToOpen
            @androidx.compose.runtime.Composable
            public fun ClickToOpen(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Tile.ClickToOpen else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Tile.ClickToOpen, *args)
            public val ClickToStartProxy: String
                get() = YumeLocaleManager.activeStrings.Service.Tile.ClickToStartProxy
            @androidx.compose.runtime.Composable
            public fun ClickToStartProxy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Tile.ClickToStartProxy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Tile.ClickToStartProxy, *args)
            public val ClickToStopProxy: String
                get() = YumeLocaleManager.activeStrings.Service.Tile.ClickToStopProxy
            @androidx.compose.runtime.Composable
            public fun ClickToStopProxy(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Tile.ClickToStopProxy else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Tile.ClickToStopProxy, *args)
            public val Connecting: String
                get() = YumeLocaleManager.activeStrings.Service.Tile.Connecting
            @androidx.compose.runtime.Composable
            public fun Connecting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Tile.Connecting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Tile.Connecting, *args)
            public val Disconnecting: String
                get() = YumeLocaleManager.activeStrings.Service.Tile.Disconnecting
            @androidx.compose.runtime.Composable
            public fun Disconnecting(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Service.Tile.Disconnecting else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Service.Tile.Disconnecting, *args)
        }
    }
    public object Settings {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.Settings.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.Title, *args)
        public object Section {
            public val UiSettings: String
                get() = YumeLocaleManager.activeStrings.Settings.Section.UiSettings
            @androidx.compose.runtime.Composable
            public fun UiSettings(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.Section.UiSettings else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.Section.UiSettings, *args)
            public val More: String
                get() = YumeLocaleManager.activeStrings.Settings.Section.More
            @androidx.compose.runtime.Composable
            public fun More(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.Section.More else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.Section.More, *args)
        }
        public object UiSettings {
            public val App: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.App
            @androidx.compose.runtime.Composable
            public fun App(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.App else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.App, *args)
            public val AppSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.AppSummary
            @androidx.compose.runtime.Composable
            public fun AppSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.AppSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.AppSummary, *args)
            public val Network: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.Network
            @androidx.compose.runtime.Composable
            public fun Network(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.Network else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.Network, *args)
            public val NetworkSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.NetworkSummary
            @androidx.compose.runtime.Composable
            public fun NetworkSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.NetworkSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.NetworkSummary, *args)
            public val Override: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.Override
            @androidx.compose.runtime.Composable
            public fun Override(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.Override else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.Override, *args)
            public val OverrideSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.OverrideSummary
            @androidx.compose.runtime.Composable
            public fun OverrideSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.OverrideSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.OverrideSummary, *args)
            public val MetaFeatures: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.MetaFeatures
            @androidx.compose.runtime.Composable
            public fun MetaFeatures(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.MetaFeatures else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.MetaFeatures, *args)
            public val MetaFeaturesSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.UiSettings.MetaFeaturesSummary
            @androidx.compose.runtime.Composable
            public fun MetaFeaturesSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.UiSettings.MetaFeaturesSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.UiSettings.MetaFeaturesSummary, *args)
        }
        public object More {
            public val Lab: String
                get() = YumeLocaleManager.activeStrings.Settings.More.Lab
            @androidx.compose.runtime.Composable
            public fun Lab(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.Lab else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.Lab, *args)
            public val LabSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.More.LabSummary
            @androidx.compose.runtime.Composable
            public fun LabSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.LabSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.LabSummary, *args)
            public val Logs: String
                get() = YumeLocaleManager.activeStrings.Settings.More.Logs
            @androidx.compose.runtime.Composable
            public fun Logs(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.Logs else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.Logs, *args)
            public val LogsSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.More.LogsSummary
            @androidx.compose.runtime.Composable
            public fun LogsSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.LogsSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.LogsSummary, *args)
            public val Onboarding: String
                get() = YumeLocaleManager.activeStrings.Settings.More.Onboarding
            @androidx.compose.runtime.Composable
            public fun Onboarding(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.Onboarding else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.Onboarding, *args)
            public val OnboardingSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.More.OnboardingSummary
            @androidx.compose.runtime.Composable
            public fun OnboardingSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.OnboardingSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.OnboardingSummary, *args)
            public val About: String
                get() = YumeLocaleManager.activeStrings.Settings.More.About
            @androidx.compose.runtime.Composable
            public fun About(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.About else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.About, *args)
            public val AboutSummary: String
                get() = YumeLocaleManager.activeStrings.Settings.More.AboutSummary
            @androidx.compose.runtime.Composable
            public fun AboutSummary(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.More.AboutSummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.More.AboutSummary, *args)
        }
        public object Error {
            public val WebviewFailed: String
                get() = YumeLocaleManager.activeStrings.Settings.Error.WebviewFailed
            @androidx.compose.runtime.Composable
            public fun WebviewFailed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Settings.Error.WebviewFailed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Settings.Error.WebviewFailed, *args)
        }
    }
    public object TrafficStatistics {
        public val Title: String
            get() = YumeLocaleManager.activeStrings.TrafficStatistics.Title
        @androidx.compose.runtime.Composable
        public fun Title(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Title else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Title, *args)
        public val EntrySummary: String
            get() = YumeLocaleManager.activeStrings.TrafficStatistics.EntrySummary
        @androidx.compose.runtime.Composable
        public fun EntrySummary(vararg args: Any?): String =
            if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.EntrySummary else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.EntrySummary, *args)
        public object TimeRange {
            public val Today: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.TimeRange.Today
            @androidx.compose.runtime.Composable
            public fun Today(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.TimeRange.Today else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.TimeRange.Today, *args)
            public val Week: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.TimeRange.Week
            @androidx.compose.runtime.Composable
            public fun Week(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.TimeRange.Week else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.TimeRange.Week, *args)
        }
        public object Summary {
            public val TodayTraffic: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Summary.TodayTraffic
            @androidx.compose.runtime.Composable
            public fun TodayTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Summary.TodayTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Summary.TodayTraffic, *args)
            public val WeekTraffic: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Summary.WeekTraffic
            @androidx.compose.runtime.Composable
            public fun WeekTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Summary.WeekTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Summary.WeekTraffic, *args)
            public val Average: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Summary.Average
            @androidx.compose.runtime.Composable
            public fun Average(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Summary.Average else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Summary.Average, *args)
            public val DayTrafficTip: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Summary.DayTrafficTip
            @androidx.compose.runtime.Composable
            public fun DayTrafficTip(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Summary.DayTrafficTip else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Summary.DayTrafficTip, *args)
        }
        public object Donut {
            public val Other: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Donut.Other
            @androidx.compose.runtime.Composable
            public fun Other(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Donut.Other else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Donut.Other, *args)
        }
        public object Action {
            public val Clear: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Action.Clear
            @androidx.compose.runtime.Composable
            public fun Clear(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Action.Clear else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Action.Clear, *args)
            public val ClearSuccess: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Action.ClearSuccess
            @androidx.compose.runtime.Composable
            public fun ClearSuccess(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Action.ClearSuccess else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Action.ClearSuccess, *args)
        }
        public object Section {
            public val Traffic: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Section.Traffic
            @androidx.compose.runtime.Composable
            public fun Traffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Section.Traffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Section.Traffic, *args)
            public val TopApps: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Section.TopApps
            @androidx.compose.runtime.Composable
            public fun TopApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Section.TopApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Section.TopApps, *args)
            public val TodayApps: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Section.TodayApps
            @androidx.compose.runtime.Composable
            public fun TodayApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Section.TodayApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Section.TodayApps, *args)
            public val WeekApps: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Section.WeekApps
            @androidx.compose.runtime.Composable
            public fun WeekApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Section.WeekApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Section.WeekApps, *args)
            public val SystemTraffic: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Section.SystemTraffic
            @androidx.compose.runtime.Composable
            public fun SystemTraffic(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Section.SystemTraffic else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Section.SystemTraffic, *args)
            public val EmptyApps: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Section.EmptyApps
            @androidx.compose.runtime.Composable
            public fun EmptyApps(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Section.EmptyApps else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Section.EmptyApps, *args)
        }
        public object Metric {
            public val Download: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Metric.Download
            @androidx.compose.runtime.Composable
            public fun Download(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Metric.Download else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Metric.Download, *args)
            public val Upload: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Metric.Upload
            @androidx.compose.runtime.Composable
            public fun Upload(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Metric.Upload else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Metric.Upload, *args)
            public val UsageLine: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Metric.UsageLine
            @androidx.compose.runtime.Composable
            public fun UsageLine(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Metric.UsageLine else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Metric.UsageLine, *args)
            public val SortByName: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Metric.SortByName
            @androidx.compose.runtime.Composable
            public fun SortByName(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Metric.SortByName else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Metric.SortByName, *args)
            public val SortByUsage: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Metric.SortByUsage
            @androidx.compose.runtime.Composable
            public fun SortByUsage(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Metric.SortByUsage else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Metric.SortByUsage, *args)
        }
        public object Weekday {
            public val Mon: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Mon
            @androidx.compose.runtime.Composable
            public fun Mon(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Mon else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Mon, *args)
            public val Tue: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Tue
            @androidx.compose.runtime.Composable
            public fun Tue(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Tue else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Tue, *args)
            public val Wed: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Wed
            @androidx.compose.runtime.Composable
            public fun Wed(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Wed else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Wed, *args)
            public val Thu: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Thu
            @androidx.compose.runtime.Composable
            public fun Thu(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Thu else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Thu, *args)
            public val Fri: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Fri
            @androidx.compose.runtime.Composable
            public fun Fri(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Fri else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Fri, *args)
            public val Sat: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Sat
            @androidx.compose.runtime.Composable
            public fun Sat(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Sat else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Sat, *args)
            public val Sun: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Sun
            @androidx.compose.runtime.Composable
            public fun Sun(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Sun else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Sun, *args)
            public val Today: String
                get() = YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Today
            @androidx.compose.runtime.Composable
            public fun Today(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Today else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.TrafficStatistics.Weekday.Today, *args)
        }
    }
    public object Util {
        public object Error {
            public val UnknownError: String
                get() = YumeLocaleManager.activeStrings.Util.Error.UnknownError
            @androidx.compose.runtime.Composable
            public fun UnknownError(vararg args: Any?): String =
                if (args.isEmpty()) YumeLocaleManager.activeStrings.Util.Error.UnknownError else String.format(java.util.Locale.getDefault(), YumeLocaleManager.activeStrings.Util.Error.UnknownError, *args)
        }
    }
}
