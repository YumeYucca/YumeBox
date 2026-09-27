package com.github.yumeyucca.yumebox.core.locale

public data class YumeStrings(
    public val About: YumeStrings_About,
    public val AccessControl: YumeStrings_AccessControl,
    public val AppSettings: YumeStrings_AppSettings,
    public val Component: YumeStrings_Component,
    public val Connection: YumeStrings_Connection,
    public val Editor: YumeStrings_Editor,
    public val Feature: YumeStrings_Feature,
    public val Home: YumeStrings_Home,
    public val Log: YumeStrings_Log,
    public val MetaFeature: YumeStrings_MetaFeature,
    public val NetworkSettings: YumeStrings_NetworkSettings,
    public val Onboarding: YumeStrings_Onboarding,
    public val OpenSourceLicenses: YumeStrings_OpenSourceLicenses,
    public val Override: YumeStrings_Override,
    public val ProfilesPage: YumeStrings_ProfilesPage,
    public val ProfilesVM: YumeStrings_ProfilesVM,
    public val Providers: YumeStrings_Providers,
    public val Proxy: YumeStrings_Proxy,
    public val Rules: YumeStrings_Rules,
    public val Service: YumeStrings_Service,
    public val Settings: YumeStrings_Settings,
    public val TrafficStatistics: YumeStrings_TrafficStatistics,
    public val Util: YumeStrings_Util,
)

// ==================== About ====================

public data class YumeStrings_About_App(
    public val Description: String,
    public val VersionLoading: String,
    public val VersionFailed: String,
)

public data class YumeStrings_About_Debug(
    public val Title: String,
    public val TestInitialization: String,
    public val InitializationReset: String,
)

public data class YumeStrings_About_Section(
    public val ProjectLinks: String,
    public val More: String,
    public val Support: String,
    public val License: String,
)

public data class YumeStrings_About_Link(
    public val TelegramGroup: String,
    public val TelegramChannel: String,
)

public data class YumeStrings_About_Support(
    public val ExportLogs: String,
    public val ReportIssue: String,
    public val ExportSuccess: String,
    public val ExportFailed: String,
)

public data class YumeStrings_About_License(
    public val Libraries: String,
    public val LibrariesSummary: String,
    public val AgplName: String,
    public val AgplDescription: String,
)

public data class YumeStrings_About(
    public val Title: String,
    public val App: YumeStrings_About_App,
    public val Debug: YumeStrings_About_Debug,
    public val Section: YumeStrings_About_Section,
    public val Link: YumeStrings_About_Link,
    public val Support: YumeStrings_About_Support,
    public val License: YumeStrings_About_License,
    public val Copyright: String,
)

// ==================== AccessControl ====================

public data class YumeStrings_AccessControl_Search(
    public val Placeholder: String,
    public val Empty: String,
)

public data class YumeStrings_AccessControl_AppList(
    public val Title: String,
    public val Loading: String,
)

public data class YumeStrings_AccessControl_Settings(
    public val Title: String,
    public val ShowSystemApps: String,
    public val SelectedFirst: String,
    public val SortMode: String,
    public val SortModeCurrent: String,
    public val BatchOperation: String,
    public val SelectAll: String,
    public val DeselectAll: String,
    public val Invert: String,
    public val ImportExport: String,
    public val Import: String,
    public val Export: String,
    public val ImportSuccess: String,
    public val ExportSuccess: String,
    public val ImportFailed: String,
    public val RegionQuickSelect: String,
    public val ChinaApps: String,
    public val OverseasApps: String,
    public val RegionSelectResult: String,
)

public data class YumeStrings_AccessControl_SortMode(
    public val PackageName: String,
    public val Label: String,
    public val InstallTime: String,
    public val UpdateTime: String,
)

public data class YumeStrings_AccessControl_Button(
    public val Cancel: String,
    public val Confirm: String,
)

public data class YumeStrings_AccessControl(
    public val Title: String,
    public val Search: YumeStrings_AccessControl_Search,
    public val AppList: YumeStrings_AccessControl_AppList,
    public val Settings: YumeStrings_AccessControl_Settings,
    public val SortMode: YumeStrings_AccessControl_SortMode,
    public val Button: YumeStrings_AccessControl_Button,
)

// ==================== AppSettings ====================

public data class YumeStrings_AppSettings_Section(
    public val Behavior: String,
    public val Interface: String,
    public val Privacy: String,
    public val Service: String,
    public val Network: String,
    public val Navigation: String,
    public val Home: String,
)

public data class YumeStrings_AppSettings_Behavior(
    public val AutoStartTitle: String,
    public val AutoUpdateOnStartTitle: String,
)

public data class YumeStrings_AppSettings_Interface(
    public val ThemeModeTitle: String,
    public val ThemeModeSummary: String,
    public val ThemeModeSystem: String,
    public val ThemeModeLight: String,
    public val ThemeModeDark: String,
    public val LanguageTitle: String,
    public val LanguageSystem: String,
    public val LanguageChinese: String,
    public val LanguageChineseTraditional: String,
    public val LanguageEnglish: String,
    public val LanguageJapanese: String,
    public val LanguageRussian: String,
    public val ColorThemeTitle: String,
    public val ColorThemePickerTitle: String,
    public val ColorThemeCodeLabel: String,
    public val ColorThemeCustomSummary: String,
    public val ThemeColorPolarityInvertTitle: String,
    public val TopBarBlurTitle: String,
    public val AutoHideNavbarTitle: String,
    public val PageScaleTitle: String,
    public val PageScaleDialogSummary: String,
    public val PredictiveBackTitle: String,
    public val PredictiveBackRestartSummary: String,
    public val PredictiveBackProgressTitle: String,
    public val ClassicHomeTitle: String,
    public val SystemWallpaperTitle: String,
    public val CustomIconTitle: String,
    public val HomeQuoteTitle: String,
    public val HomeQuoteDefault: String,
    public val EditHomeQuoteTitle: String,
    public val HomeWallpaperImportFailed: String,
)

public data class YumeStrings_AppSettings_Privacy(
    public val HideFromRecentsTitle: String,
)

public data class YumeStrings_AppSettings_ServiceSection(
    public val TrafficNotificationTitle: String,
    public val ExitUiWhenBackgroundTitle: String,
    public val SuperIslandTitle: String,
    public val ShizukuTitle: String,
    public val ShizukuNotRunning: String,
    public val ShizukuPermissionRequired: String,
    public val ShizukuReady: String,
    public val ShizukuStatusUnavailable: String,
    public val ShizukuStatusPending: String,
    public val ShizukuStatusGranted: String,
    public val BatteryOptimizationTitle: String,
    public val BatteryOptimizationAlreadyDisabled: String,
)

public data class YumeStrings_AppSettings_Network(
    public val CustomUserAgentTitle: String,
    public val CustomUserAgentSummaryDefault: String,
)

public data class YumeStrings_AppSettings_WarningDialog(
    public val Title: String,
)

public data class YumeStrings_AppSettings_EditDialog(
    public val UserAgentTitle: String,
)

public data class YumeStrings_AppSettings_Button(
    public val Apply: String,
)

public data class YumeStrings_AppSettings(
    public val Title: String,
    public val Section: YumeStrings_AppSettings_Section,
    public val Behavior: YumeStrings_AppSettings_Behavior,
    public val Interface: YumeStrings_AppSettings_Interface,
    public val Privacy: YumeStrings_AppSettings_Privacy,
    public val ServiceSection: YumeStrings_AppSettings_ServiceSection,
    public val Network: YumeStrings_AppSettings_Network,
    public val WarningDialog: YumeStrings_AppSettings_WarningDialog,
    public val EditDialog: YumeStrings_AppSettings_EditDialog,
    public val Button: YumeStrings_AppSettings_Button,
)

// ==================== Component ====================

public data class YumeStrings_Component_ProfileCard(
    public val Update: String,
    public val Export: String,
    public val Edit: String,
    public val Delete: String,
    public val RemoteSubscription: String,
    public val LocalFile: String,
    public val LocalConfig: String,
    public val ClickToUpdate: String,
    public val Traffic: String,
    public val UsedTraffic: String,
    public val ExpireAt: String,
    public val ExpireToday: String,
    public val Expired: String,
    public val JustNow: String,
    public val MinutesAgo: String,
    public val HoursAgo: String,
    public val DaysAgo: String,
)

public data class YumeStrings_Component_WebView(
    public val InvalidUrl: String,
)

public data class YumeStrings_Component_Selector(
    public val NotModify: String,
    public val Enable: String,
    public val Disable: String,
    public val Replace: String,
    public val Prepend: String,
    public val Append: String,
    public val Merge: String,
)

public data class YumeStrings_Component_Navigation(
    public val Back: String,
)

public data class YumeStrings_Component_Message(
    public val Confirm: String,
    public val Hint: String,
    public val Error: String,
    public val Success: String,
)

public data class YumeStrings_Component_Button(
    public val Cancel: String,
    public val Confirm: String,
    public val Clear: String,
    public val Copy: String,
)

public data class YumeStrings_Component_Flag(
    public val ContentDescription: String,
)

public data class YumeStrings_Component_Loading(
    public val Starting: String,
)

public data class YumeStrings_Component_ConfigInput(
    public val PortLabel: String,
    public val CountItems: String,
)

public data class YumeStrings_Component_BottomBar(
    public val Home: String,
    public val Proxy: String,
    public val Config: String,
    public val Setting: String,
)

public data class YumeStrings_Component_Editor_Action(
    public val Search: String,
)

public data class YumeStrings_Component_Editor_Dialog(
    public val AddTitle: String,
    public val EditTitle: String,
    public val ResetTitle: String,
    public val ResetMessage: String,
)

public data class YumeStrings_Component_Editor_Empty(
    public val Title: String,
    public val Hint: String,
)

public data class YumeStrings_Component_Editor_Error(
    public val KeyEmpty: String,
    public val KeyExists: String,
)

public data class YumeStrings_Component_Editor_Rule(
    public val Type: String,
    public val Target: String,
    public val Content: String,
    public val Src: String,
    public val NoResolve: String,
    public val TargetReject: String,
    public val TargetDirect: String,
    public val TargetMatch: String,
    public val ErrorContentRequired: String,
)

public data class YumeStrings_Component_Editor(
    public val CountItems: String,
    public val Action: YumeStrings_Component_Editor_Action,
    public val Dialog: YumeStrings_Component_Editor_Dialog,
    public val Empty: YumeStrings_Component_Editor_Empty,
    public val Error: YumeStrings_Component_Editor_Error,
    public val Rule: YumeStrings_Component_Editor_Rule,
)

public data class YumeStrings_Component(
    public val ProfileCard: YumeStrings_Component_ProfileCard,
    public val WebView: YumeStrings_Component_WebView,
    public val Selector: YumeStrings_Component_Selector,
    public val Navigation: YumeStrings_Component_Navigation,
    public val Message: YumeStrings_Component_Message,
    public val Button: YumeStrings_Component_Button,
    public val Flag: YumeStrings_Component_Flag,
    public val Loading: YumeStrings_Component_Loading,
    public val ConfigInput: YumeStrings_Component_ConfigInput,
    public val BottomBar: YumeStrings_Component_BottomBar,
    public val Editor: YumeStrings_Component_Editor,
)

// ==================== Connection ====================

public data class YumeStrings_Connection_Tab(
    public val Active: String,
    public val Closed: String,
)

public data class YumeStrings_Connection_Sort(
    public val Time: String,
    public val Upload: String,
    public val Download: String,
    public val Host: String,
)

public data class YumeStrings_Connection_RelativeTime(
    public val JustNow: String,
    public val MinutesAgo: String,
    public val HoursAgo: String,
    public val DaysAgo: String,
    public val Date: String,
)

public data class YumeStrings_Connection_Detail_Action(
    public val Interrupting: String,
    public val Interrupt: String,
)

public data class YumeStrings_Connection_Detail_Section(
    public val Info: String,
    public val Rule: String,
    public val Chain: String,
)

public data class YumeStrings_Connection_Detail_Label(
    public val Host: String,
    public val Protocol: String,
    public val Process: String,
    public val SourceAddress: String,
    public val DestinationAddress: String,
    public val Duration: String,
    public val Upload: String,
    public val Download: String,
    public val Type: String,
    public val Content: String,
    public val Chain: String,
)

public data class YumeStrings_Connection_Detail(
    public val Title: String,
    public val Action: YumeStrings_Connection_Detail_Action,
    public val Section: YumeStrings_Connection_Detail_Section,
    public val Label: YumeStrings_Connection_Detail_Label,
)

public data class YumeStrings_Connection(
    public val Title: String,
    public val Summary: String,
    public val Tab: YumeStrings_Connection_Tab,
    public val Search: String,
    public val SearchHint: String,
    public val SortBy: String,
    public val Sort: YumeStrings_Connection_Sort,
    public val ChainCount: String,
    public val RelativeTime: YumeStrings_Connection_RelativeTime,
    public val Detail: YumeStrings_Connection_Detail,
    public val Loading: String,
    public val Empty: String,
    public val NoResults: String,
)

// ==================== Editor ====================

public data class YumeStrings_Editor_Title(
    public val Config: String,
    public val Preview: String,
    public val Override: String,
    public val Profile: String,
)

public data class YumeStrings_Editor_Action(
    public val Format: String,
    public val Save: String,
)

public data class YumeStrings_Editor_Message(
    public val FormatSuccess: String,
    public val FormatSkipped: String,
    public val SaveFailed: String,
)

public data class YumeStrings_Editor_Discard(
    public val Title: String,
    public val Summary: String,
    public val Confirm: String,
)

public data class YumeStrings_Editor(
    public val Title: YumeStrings_Editor_Title,
    public val Action: YumeStrings_Editor_Action,
    public val Message: YumeStrings_Editor_Message,
    public val Discard: YumeStrings_Editor_Discard,
    public val JsonSubtitle: String,
)

// ==================== Feature ====================

public data class YumeStrings_Feature_ServiceStatus(
    public val Section: String,
    public val SwitchStartSubStore: String,
    public val OpenSubStorePanel: String,
    public val AllowLan: String,
    public val AutoCloseModeTitle: String,
    public val AutoCloseModeAlwaysOn: String,
    public val AutoCloseMode5Min: String,
    public val AutoCloseMode10Min: String,
    public val AutoClosed: String,
)

public data class YumeStrings_Feature_Panel(
    public val Section: String,
    public val SelectPanel: String,
    public val OpenMode: String,
    public val OpenPanel: String,
    public val Unknown: String,
    public val CreateShortcut: String,
    public val CreateShortcutSummary: String,
    public val ShortcutTitle: String,
    public val ShortcutName: String,
    public val ShortcutPickIcon: String,
    public val ShortcutResetIcon: String,
)

public data class YumeStrings_Feature_RemoteController(
    public val Section: String,
    public val ModeTitle: String,
    public val ControlBackend: String,
    public val AddBackend: String,
    public val EditBackend: String,
    public val Name: String,
    public val Host: String,
    public val Port: String,
    public val Secret: String,
    public val Protocol: String,
    public val PortRangeError: String,
    public val Connected: String,
    public val ConnectionFailed: String,
    public val Delete: String,
)

public data class YumeStrings_Feature_SubStore(
    public val Section: String,
    public val JavetLibraryReady: String,
    public val JavetLibraryDownload: String,
    public val JavetAvailable: String,
    public val DownloadHint: String,
    public val JavetDownloadSuccess: String,
    public val JavetDownloadFailed: String,
    public val DownloadResources: String,
    public val DownloadResourcesSummary: String,
    public val Not32Bit: String,
    public val DownloadSubStoreFirst: String,
    public val JavetNotReady: String,
    public val FrontendDownloadSuccess: String,
    public val FrontendDownloadFailed: String,
    public val BackendDownloadSuccess: String,
    public val BackendDownloadFailed: String,
    public val DownloadError: String,
)

public data class YumeStrings_Feature_BackupRestore_RestoreDialog(
    public val Title: String,
    public val Message: String,
)

public data class YumeStrings_Feature_BackupRestore_Message(
    public val ExportSuccess: String,
    public val RestoreSuccess: String,
)

public data class YumeStrings_Feature_BackupRestore_Error(
    public val OpenOutputFailed: String,
    public val OpenInputFailed: String,
    public val OperationFailed: String,
)

public data class YumeStrings_Feature_BackupRestore(
    public val Section: String,
    public val ExportTitle: String,
    public val ExportSummary: String,
    public val RestoreTitle: String,
    public val RestoreSummary: String,
    public val Cancel: String,
    public val RestoreDialog: YumeStrings_Feature_BackupRestore_RestoreDialog,
    public val Message: YumeStrings_Feature_BackupRestore_Message,
    public val Error: YumeStrings_Feature_BackupRestore_Error,
)

public data class YumeStrings_Feature(
    public val Title: String,
    public val ServiceStatus: YumeStrings_Feature_ServiceStatus,
    public val Panel: YumeStrings_Feature_Panel,
    public val RemoteController: YumeStrings_Feature_RemoteController,
    public val SubStore: YumeStrings_Feature_SubStore,
    public val BackupRestore: YumeStrings_Feature_BackupRestore,
)

// ==================== Home ====================

public data class YumeStrings_Home_Message(
    public val ConfigSwitched: String,
    public val ConfigSwitchFailed: String,
    public val Preparing: String,
    public val BuiltinGeoRequired: String,
    public val StartFailed: String,
    public val RuntimeUnavailable: String,
    public val StopFailed: String,
)

public data class YumeStrings_Home_Control(
    public val HintAddProfile: String,
    public val HintEnableProfile: String,
    public val Start: String,
    public val Stop: String,
)

public data class YumeStrings_Home_NodeInfo(
    public val Node: String,
    public val Delay: String,
    public val Unknown: String,
    public val DelayValue: String,
)

public data class YumeStrings_Home_Traffic(
    public val UpShort: String,
    public val DownShort: String,
    public val Upload: String,
    public val Download: String,
    public val NoProfile: String,
)

public data class YumeStrings_Home_IpInfo(
    public val ExitIp: String,
)

public data class YumeStrings_Home_Status(
    public val Connecting: String,
    public val Disconnecting: String,
    public val Running: String,
    public val Lost: String,
    public val TapToStart: String,
)

public data class YumeStrings_Home_Settings(
    public val Title: String,
    public val Quote: String,
    public val ClassicHome: String,
    public val ExpandSidebar: String,
    public val ChangeWallpaper: String,
)

public data class YumeStrings_Home_ProxyMode(
    public val Vpn: String,
    public val Tun: String,
    public val Ebpf: String,
)

public data class YumeStrings_Home_PreviewGuide(
    public val Title: String,
    public val Description: String,
    public val Next: String,
    public val Start: String,
    public val WallpaperTitle: String,
    public val WallpaperDescription: String,
    public val SwipeTitle: String,
    public val SwipeDescription: String,
    public val AddTitle: String,
    public val AddDescription: String,
)

public data class YumeStrings_Home(
    public val Title: String,
    public val Message: YumeStrings_Home_Message,
    public val Control: YumeStrings_Home_Control,
    public val NodeInfo: YumeStrings_Home_NodeInfo,
    public val Traffic: YumeStrings_Home_Traffic,
    public val IpInfo: YumeStrings_Home_IpInfo,
    public val Status: YumeStrings_Home_Status,
    public val Settings: YumeStrings_Home_Settings,
    public val ProxyMode: YumeStrings_Home_ProxyMode,
    public val PreviewGuide: YumeStrings_Home_PreviewGuide,
)

// ==================== Log ====================

public data class YumeStrings_Log_Level(
    public val All: String,
    public val Debug: String,
    public val Info: String,
    public val Warning: String,
    public val Error: String,
    public val Filter: String,
)

public data class YumeStrings_Log_Action(
    public val Export: String,
    public val Copied: String,
)

public data class YumeStrings_Log_Empty(
    public val Connecting: String,
    public val ConnectingHint: String,
    public val Retrying: String,
    public val RetryingHint: String,
    public val NoLogs: String,
    public val LiveHint: String,
    public val NoMatch: String,
    public val NoResults: String,
)

public data class YumeStrings_Log_Detail(
    public val WaitingLog: String,
    public val WillShowWhenGenerated: String,
)

public data class YumeStrings_Log(
    public val Title: String,
    public val Summary: String,
    public val SearchHint: String,
    public val Level: YumeStrings_Log_Level,
    public val Action: YumeStrings_Log_Action,
    public val Empty: YumeStrings_Log_Empty,
    public val Detail: YumeStrings_Log_Detail,
)

// ==================== MetaFeature ====================

public data class YumeStrings_MetaFeature_Section(
    public val ConnectionAndTraffic: String,
    public val Routing: String,
)

public data class YumeStrings_MetaFeature_GeoX(
    public val OnlineUpdateTitle: String,
    public val OnlineUpdateSummary: String,
)

public data class YumeStrings_MetaFeature_CustomRouting_Region(
    public val HongKong: String,
    public val Taiwan: String,
    public val Japan: String,
    public val Singapore: String,
    public val UnitedStates: String,
    public val Other: String,
)

public data class YumeStrings_MetaFeature_CustomRouting_Item(
    public val Proxy: String,
    public val Ads: String,
    public val China: String,
    public val Global: String,
    public val Match: String,
)

public data class YumeStrings_MetaFeature_CustomRouting(
    public val Title: String,
    public val Summary: String,
    public val EditYaml: String,
    public val ManualYamlPresetDiscarded: String,
    public val GroupTypeTitle: String,
    public val GroupTypeUrlTest: String,
    public val GroupTypeFallback: String,
    public val UrlTestRegionGroupTitle: String,
    public val FallbackRegionGroupTitle: String,
    public val Region: YumeStrings_MetaFeature_CustomRouting_Region,
    public val Item: YumeStrings_MetaFeature_CustomRouting_Item,
)

public data class YumeStrings_MetaFeature_RuntimeRules(
    public val Title: String,
    public val Summary: String,
)

public data class YumeStrings_MetaFeature_Download(
    public val DialogTitle: String,
    public val DownloadComplete: String,
)

public data class YumeStrings_MetaFeature_AgeKey(
    public val Section: String,
    public val X25519Title: String,
    public val HybridTitle: String,
    public val SecretKey: String,
    public val PublicKey: String,
    public val Generate: String,
    public val DerivePublicKey: String,
)

public data class YumeStrings_MetaFeature(
    public val Title: String,
    public val Section: YumeStrings_MetaFeature_Section,
    public val GeoX: YumeStrings_MetaFeature_GeoX,
    public val CustomRouting: YumeStrings_MetaFeature_CustomRouting,
    public val RuntimeRules: YumeStrings_MetaFeature_RuntimeRules,
    public val Download: YumeStrings_MetaFeature_Download,
    public val AgeKey: YumeStrings_MetaFeature_AgeKey,
)

// ==================== NetworkSettings ====================

public data class YumeStrings_NetworkSettings_Section(
    public val VpnOptions: String,
    public val Advanced: String,
    public val ProxyOptions: String,
    public val Kernel: String,
)

public data class YumeStrings_NetworkSettings_Advanced(
    public val DisableOverrideTitle: String,
)

public data class YumeStrings_NetworkSettings_RunMode(
    public val SectionTitle: String,
    public val VpnServiceTitle: String,
    public val VpnServiceSummary: String,
    public val TunTitle: String,
    public val TunSummary: String,
    public val EbpfTitle: String,
    public val EbpfSummary: String,
)

public data class YumeStrings_NetworkSettings_VpnOptions(
    public val BypassPrivateTitle: String,
    public val DnsHijackTitle: String,
    public val AllowBypassTitle: String,
    public val EnableIpv6Title: String,
    public val SystemProxyTitle: String,
)

public data class YumeStrings_NetworkSettings_EbpfOptions(
    public val BypassCnTitle: String,
)

public data class YumeStrings_NetworkSettings_TunOptions(
    public val Title: String,
    public val IfNameTitle: String,
    public val MtuTitle: String,
    public val StackTitle: String,
    public val StackSystem: String,
    public val StackGVisor: String,
    public val StackMixed: String,
    public val StackMips: String,
    public val AutoRouteTitle: String,
    public val StrictRouteTitle: String,
    public val AutoRedirectTitle: String,
    public val DnsModeTitle: String,
    public val DnsRedirHost: String,
    public val DnsFakeIp: String,
    public val Ipv6Title: String,
)

public data class YumeStrings_NetworkSettings_Kernel(
    public val ActiveTitle: String,
    public val BundledAlpha: String,
    public val RefreshTitle: String,
    public val DownloadTitle: String,
    public val FetchButton: String,
    public val DownloadButton: String,
    public val CustomTitle: String,
    public val CustomMethodTitle: String,
    public val CustomUrlMethod: String,
    public val CustomFileMethod: String,
    public val CustomUrlLabel: String,
    public val CustomChooseFile: String,
    public val CustomInstallButton: String,
)

public data class YumeStrings_NetworkSettings_ProxyOptions(
    public val AccessControlModeTitle: String,
    public val AllowAll: String,
    public val AllowSelected: String,
    public val RejectSelected: String,
    public val ManageAccessControlTitle: String,
)

public data class YumeStrings_NetworkSettings_WifiAutomation(
    public val Title: String,
    public val EnabledTitle: String,
    public val WifiNameHeading: String,
    public val NetworkChangeSection: String,
    public val OtherWifiTitle: String,
    public val NoWifiTitle: String,
    public val AddCurrent: String,
    public val AddManual: String,
    public val Scan: String,
    public val Scanning: String,
    public val ScanEmpty: String,
    public val ScanUnavailable: String,
    public val ManualDialogTitle: String,
    public val EditDialogTitle: String,
    public val ManualDialogLabel: String,
    public val StartAction: String,
    public val StopAction: String,
    public val KeepAction: String,
    public val ActionTitle: String,
    public val ProfileAction: String,
    public val NoSwitchAction: String,
    public val EmptyRules: String,
    public val VpnOnly: String,
    public val PermissionTitle: String,
    public val PermissionMessage: String,
    public val ApproximateTitle: String,
    public val ApproximateMessage: String,
    public val DeniedTitle: String,
    public val DeniedMessage: String,
    public val SettingsTitle: String,
    public val SettingsMessage: String,
    public val LocationTitle: String,
    public val LocationMessage: String,
    public val Grant: String,
    public val OpenSettings: String,
    public val TurnOnLocation: String,
    public val Added: String,
    public val Duplicate: String,
    public val NoWifi: String,
    public val Unavailable: String,
)

public data class YumeStrings_NetworkSettings_Error(
    public val VpnDenied: String,
    public val RootRequired: String,
)

public data class YumeStrings_NetworkSettings(
    public val Title: String,
    public val Section: YumeStrings_NetworkSettings_Section,
    public val Advanced: YumeStrings_NetworkSettings_Advanced,
    public val RunMode: YumeStrings_NetworkSettings_RunMode,
    public val VpnOptions: YumeStrings_NetworkSettings_VpnOptions,
    public val EbpfOptions: YumeStrings_NetworkSettings_EbpfOptions,
    public val TunOptions: YumeStrings_NetworkSettings_TunOptions,
    public val Kernel: YumeStrings_NetworkSettings_Kernel,
    public val ProxyOptions: YumeStrings_NetworkSettings_ProxyOptions,
    public val WifiAutomation: YumeStrings_NetworkSettings_WifiAutomation,
    public val Error: YumeStrings_NetworkSettings_Error,
)

// ==================== Onboarding ====================

public data class YumeStrings_Onboarding_Navigation(
    public val Back: String,
    public val Next: String,
    public val Enter: String,
    public val Start: String,
)

public data class YumeStrings_Onboarding_Welcome(
    public val Tagline: String,
)

public data class YumeStrings_Onboarding_Permission_Common(
    public val Granted: String,
)

public data class YumeStrings_Onboarding_Permission_Notification(
    public val Title: String,
    public val SummaryNeed: String,
    public val SummaryNotRequired: String,
)

public data class YumeStrings_Onboarding_Permission_AppList(
    public val Title: String,
    public val SummaryNeed: String,
)

public data class YumeStrings_Onboarding_Permission(
    public val Title: String,
    public val Subtitle: String,
    public val Common: YumeStrings_Onboarding_Permission_Common,
    public val Notification: YumeStrings_Onboarding_Permission_Notification,
    public val AppList: YumeStrings_Onboarding_Permission_AppList,
)

public data class YumeStrings_Onboarding_Privacy_Accept(
    public val Title: String,
)

public data class YumeStrings_Onboarding_Privacy(
    public val Title: String,
    public val Subtitle: String,
    public val RichTextLead: String,
    public val RichTextPrefix: String,
    public val RichTextConnector: String,
    public val RichTextSuffix: String,
    public val TermsLink: String,
    public val PolicyLink: String,
    public val Accept: YumeStrings_Onboarding_Privacy_Accept,
)

public data class YumeStrings_Onboarding_Personalize(
    public val Title: String,
    public val Subtitle: String,
)

public data class YumeStrings_Onboarding_Finish(
    public val Title: String,
    public val Subtitle: String,
)

public data class YumeStrings_Onboarding_Project_Github(
    public val Title: String,
    public val Summary: String,
)

public data class YumeStrings_Onboarding_Project_Community(
    public val Title: String,
    public val Summary: String,
)

public data class YumeStrings_Onboarding_Project(
    public val Github: YumeStrings_Onboarding_Project_Github,
    public val Community: YumeStrings_Onboarding_Project_Community,
)

public data class YumeStrings_Onboarding_Sheet(
    public val PrivacyPolicyTitle: String,
    public val LoadFailed: String,
)

public data class YumeStrings_Onboarding(
    public val Navigation: YumeStrings_Onboarding_Navigation,
    public val Welcome: YumeStrings_Onboarding_Welcome,
    public val Permission: YumeStrings_Onboarding_Permission,
    public val Privacy: YumeStrings_Onboarding_Privacy,
    public val Personalize: YumeStrings_Onboarding_Personalize,
    public val Finish: YumeStrings_Onboarding_Finish,
    public val Project: YumeStrings_Onboarding_Project,
    public val Sheet: YumeStrings_Onboarding_Sheet,
)

// ==================== OpenSourceLicenses ====================

public data class YumeStrings_OpenSourceLicenses_LicenseSheet(
    public val NoContent: String,
)

public data class YumeStrings_OpenSourceLicenses(
    public val Title: String,
    public val LicenseSheet: YumeStrings_OpenSourceLicenses_LicenseSheet,
)

// ==================== Override ====================

public data class YumeStrings_Override_Action(
    public val Create: String,
    public val New: String,
    public val NetworkImport: String,
)

public data class YumeStrings_Override_BuiltIn(
    public val PreventDnsLeak: String,
    public val AddDirectRules: String,
    public val PuddingDog: String,
    public val Acl4ssrOnlineFull: String,
    public val CopyName: String,
)

public data class YumeStrings_Override_Empty(
    public val Title: String,
    public val Hint: String,
    public val UserTitle: String,
    public val UserHint: String,
)

public data class YumeStrings_Override_Section(
    public val BuiltIn: String,
    public val User: String,
)

public data class YumeStrings_Override_Status(
    public val InUse: String,
    public val NotInUse: String,
    public val BuiltIn: String,
)

public data class YumeStrings_Override_Card(
    public val Copy: String,
    public val Export: String,
    public val Edit: String,
    public val Delete: String,
    public val EditButton: String,
    public val DeleteButton: String,
    public val Apply: String,
    public val ApplyButton: String,
)

public data class YumeStrings_Override_ApplySheet_Button(
    public val Cancel: String,
    public val Confirm: String,
)

public data class YumeStrings_Override_ApplySheet(
    public val Title: String,
    public val Empty: String,
    public val Success: String,
    public val Failed: String,
    public val Button: YumeStrings_Override_ApplySheet_Button,
)

public data class YumeStrings_Override_Import(
    public val ReadError: String,
    public val Failed: String,
    public val FileError: String,
    public val NetworkError: String,
    public val InvalidUrl: String,
    public val HttpError: String,
    public val UnsupportedType: String,
    public val EmptyJavaScript: String,
)

public data class YumeStrings_Override_Export(
    public val Failed: String,
    public val Success: String,
)

public data class YumeStrings_Override_Dialog_Create(
    public val Title: String,
    public val Name: String,
    public val Url: String,
    public val Type: String,
)

public data class YumeStrings_Override_Dialog_Delete(
    public val Title: String,
    public val InUseMessage: String,
    public val Message: String,
)

public data class YumeStrings_Override_Dialog_Button(
    public val Cancel: String,
    public val Delete: String,
)

public data class YumeStrings_Override_Dialog(
    public val Create: YumeStrings_Override_Dialog_Create,
    public val Delete: YumeStrings_Override_Dialog_Delete,
    public val Button: YumeStrings_Override_Dialog_Button,
)

public data class YumeStrings_Override_Draft(
    public val BasicRouting: String,
    public val ServiceRouting: String,
)

public data class YumeStrings_Override_Save(
    public val ImportDefaultName: String,
    public val Failed: String,
)

public data class YumeStrings_Override_Label(
    public val RulesReplace: String,
)

public data class YumeStrings_Override(
    public val Title: String,
    public val Action: YumeStrings_Override_Action,
    public val BuiltIn: YumeStrings_Override_BuiltIn,
    public val Empty: YumeStrings_Override_Empty,
    public val Section: YumeStrings_Override_Section,
    public val Status: YumeStrings_Override_Status,
    public val Card: YumeStrings_Override_Card,
    public val ApplySheet: YumeStrings_Override_ApplySheet,
    public val Import: YumeStrings_Override_Import,
    public val Export: YumeStrings_Override_Export,
    public val Dialog: YumeStrings_Override_Dialog,
    public val Draft: YumeStrings_Override_Draft,
    public val Save: YumeStrings_Override_Save,
    public val Label: YumeStrings_Override_Label,
)

// ==================== ProfilesPage ====================

public data class YumeStrings_ProfilesPage_Action(
    public val UpdateAll: String,
    public val AddProfile: String,
)

public data class YumeStrings_ProfilesPage_Empty(
    public val NoProfiles: String,
    public val Hint: String,
)

public data class YumeStrings_ProfilesPage_Sheet(
    public val AddTitle: String,
    public val EditTitle: String,
    public val Complete: String,
)

public data class YumeStrings_ProfilesPage_Type(
    public val Title: String,
    public val Subscription: String,
    public val LocalFile: String,
    public val QrScan: String,
)

public data class YumeStrings_ProfilesPage_Input(
    public val ProfileName: String,
    public val SubscriptionUrl: String,
    public val SelectFile: String,
    public val NewProfile: String,
    public val AgeSecretKey: String,
)

public data class YumeStrings_ProfilesPage_QrScanner(
    public val NeedPermission: String,
    public val NeedCamera: String,
    public val SelectFromAlbum: String,
    public val RecognizeSuccess: String,
    public val RecognizeFailed: String,
    public val RecognizeError: String,
)

public data class YumeStrings_ProfilesPage_Message(
    public val UnknownFile: String,
)

public data class YumeStrings_ProfilesPage_Validation(
    public val EnterUrl: String,
    public val SelectFile: String,
    public val YamlOnly: String,
)

public data class YumeStrings_ProfilesPage_Progress(
    public val Downloading: String,
)

public data class YumeStrings_ProfilesPage_Button(
    public val Cancel: String,
    public val Confirm: String,
)

public data class YumeStrings_ProfilesPage_DeleteDialog(
    public val Title: String,
    public val Message: String,
    public val Confirm: String,
)

public data class YumeStrings_ProfilesPage_EditDialog(
    public val Title: String,
)

public data class YumeStrings_ProfilesPage_LinkSettings_Validation(
    public val EnterName: String,
    public val EnterUrl: String,
    public val InvalidUrl: String,
)

public data class YumeStrings_ProfilesPage_LinkSettings(
    public val Title: String,
    public val OpenMode: String,
    public val OpenModeInApp: String,
    public val OpenModeExternal: String,
    public val DefaultLink: String,
    public val DefaultLinkSummary: String,
    public val AddLink: String,
    public val EditLink: String,
    public val Name: String,
    public val Url: String,
    public val Close: String,
    public val Validation: YumeStrings_ProfilesPage_LinkSettings_Validation,
)

public data class YumeStrings_ProfilesPage_ShareDialog(
    public val Title: String,
    public val ShareFile: String,
    public val ShareLink: String,
    public val NoLink: String,
    public val ImportedConfigMissing: String,
)

public data class YumeStrings_ProfilesPage_SettingsDialog(
    public val Title: String,
    public val ChangeLink: String,
    public val CustomRouting: String,
    public val CustomRoutingSummary: String,
    public val NoDescription: String,
    public val EditProfile: String,
    public val OpenConfig: String,
    public val EditSettings: String,
    public val SectionType: String,
    public val SectionSubscription: String,
    public val SectionOverride: String,
    public val SaveFailed: String,
    public val ConfigMissing: String,
    public val AgeSecretKey: String,
)

public data class YumeStrings_ProfilesPage(
    public val Title: String,
    public val Action: YumeStrings_ProfilesPage_Action,
    public val Empty: YumeStrings_ProfilesPage_Empty,
    public val Sheet: YumeStrings_ProfilesPage_Sheet,
    public val Type: YumeStrings_ProfilesPage_Type,
    public val Input: YumeStrings_ProfilesPage_Input,
    public val QrScanner: YumeStrings_ProfilesPage_QrScanner,
    public val Message: YumeStrings_ProfilesPage_Message,
    public val Validation: YumeStrings_ProfilesPage_Validation,
    public val Progress: YumeStrings_ProfilesPage_Progress,
    public val Button: YumeStrings_ProfilesPage_Button,
    public val DeleteDialog: YumeStrings_ProfilesPage_DeleteDialog,
    public val EditDialog: YumeStrings_ProfilesPage_EditDialog,
    public val LinkSettings: YumeStrings_ProfilesPage_LinkSettings,
    public val ShareDialog: YumeStrings_ProfilesPage_ShareDialog,
    public val SettingsDialog: YumeStrings_ProfilesPage_SettingsDialog,
)

// ==================== ProfilesVM ====================

public data class YumeStrings_ProfilesVM_Message(
    public val ProfileAdded: String,
    public val AddFailed: String,
    public val ProfileDeleted: String,
    public val DeleteFailed: String,
    public val ProfileUpdated: String,
    public val UpdateFailed: String,
    public val ToggleFailed: String,
    public val ProvidersPartial: String,
    public val ProvidersUndiscovered: String,
)

public data class YumeStrings_ProfilesVM_Progress(
    public val Preparing: String,
    public val Verifying: String,
    public val ImportComplete: String,
)

public data class YumeStrings_ProfilesVM_Error(
    public val ProfileNotExist: String,
)

public data class YumeStrings_ProfilesVM(
    public val Message: YumeStrings_ProfilesVM_Message,
    public val Progress: YumeStrings_ProfilesVM_Progress,
    public val Error: YumeStrings_ProfilesVM_Error,
)

// ==================== Providers ====================

public data class YumeStrings_Providers_Action(
    public val UpdateAll: String,
    public val Update: String,
    public val Upload: String,
    public val Operation: String,
)

public data class YumeStrings_Providers_Empty(
    public val NotRunning: String,
    public val NotRunningHint: String,
    public val NoProviders: String,
    public val NoProvidersHint: String,
)

public data class YumeStrings_Providers_Type(
    public val ProxyProviders: String,
    public val RuleProviders: String,
)

public data class YumeStrings_Providers_VehicleType(
    public val Http: String,
    public val File: String,
    public val Inline: String,
    public val Compatible: String,
)

public data class YumeStrings_Providers_Message(
    public val FetchFailed: String,
    public val UpdateSuccess: String,
    public val UpdateFailed: String,
    public val AllUpdated: String,
    public val UploadSuccess: String,
    public val UploadFailed: String,
)

public data class YumeStrings_Providers(
    public val Title: String,
    public val Action: YumeStrings_Providers_Action,
    public val Empty: YumeStrings_Providers_Empty,
    public val Type: YumeStrings_Providers_Type,
    public val VehicleType: YumeStrings_Providers_VehicleType,
    public val Message: YumeStrings_Providers_Message,
)

// ==================== Proxy ====================

public data class YumeStrings_Proxy_Mode(
    public val Direct: String,
)

public data class YumeStrings_Proxy_Action(
    public val Panel: String,
    public val Test: String,
    public val Sort: String,
    public val LocateCurrent: String,
    public val More: String,
)

public data class YumeStrings_Proxy_Node(
    public val Timeout: String,
    public val Count: String,
)

public data class YumeStrings_Proxy_Empty(
    public val NoNodes: String,
    public val Hint: String,
)

public data class YumeStrings_Proxy_Testing(
    public val Group: String,
    public val All: String,
    public val RequestSent: String,
    public val Progress: String,
    public val Completed: String,
    public val Failed: String,
    public val InProgress: String,
)

public data class YumeStrings_Proxy_Selection(
    public val Switched: String,
    public val Failed: String,
    public val Error: String,
)

public data class YumeStrings_Proxy_SortMode(
    public val Default: String,
    public val ByName: String,
    public val ByLatency: String,
)

public data class YumeStrings_Proxy_DisplayMode(
    public val SingleDetailed: String,
    public val SingleSimple: String,
    public val DoubleDetailed: String,
    public val DoubleSimple: String,
)

public data class YumeStrings_Proxy(
    public val Title: String,
    public val Mode: YumeStrings_Proxy_Mode,
    public val Action: YumeStrings_Proxy_Action,
    public val Node: YumeStrings_Proxy_Node,
    public val Empty: YumeStrings_Proxy_Empty,
    public val Testing: YumeStrings_Proxy_Testing,
    public val Selection: YumeStrings_Proxy_Selection,
    public val SortMode: YumeStrings_Proxy_SortMode,
    public val DisplayMode: YumeStrings_Proxy_DisplayMode,
)

// ==================== Rules ====================

public data class YumeStrings_Rules_Label(
    public val Proxy: String,
    public val HitRate: String,
)

public data class YumeStrings_Rules_Message(
    public val ToggleFailed: String,
)

public data class YumeStrings_Rules_Empty(
    public val Loading: String,
    public val LoadingHint: String,
    public val NotRunning: String,
    public val NotRunningHint: String,
    public val NoRules: String,
    public val NoRulesHint: String,
    public val NoResults: String,
)

public data class YumeStrings_Rules(
    public val Title: String,
    public val Summary: String,
    public val SearchHint: String,
    public val Label: YumeStrings_Rules_Label,
    public val Message: YumeStrings_Rules_Message,
    public val Empty: YumeStrings_Rules_Empty,
)

// ==================== Service ====================

public data class YumeStrings_Service_Notification(
    public val Running: String,
    public val UnknownProfile: String,
    public val NoNode: String,
    public val CurrentNode: String,
    public val CurrentNodeLabel: String,
    public val RealtimeTraffic: String,
    public val SpeedLine: String,
    public val TotalTraffic: String,
    public val LogChannel: String,
    public val LogRecording: String,
    public val LogRecordingPending: String,
    public val Stop: String,
)

public data class YumeStrings_Service_Tile(
    public val ClickToOpen: String,
    public val ClickToStartProxy: String,
    public val ClickToStopProxy: String,
    public val Connecting: String,
    public val Disconnecting: String,
)

public data class YumeStrings_Service(
    public val Notification: YumeStrings_Service_Notification,
    public val Tile: YumeStrings_Service_Tile,
)

// ==================== Settings ====================

public data class YumeStrings_Settings_Section(
    public val UiSettings: String,
    public val More: String,
)

public data class YumeStrings_Settings_UiSettings(
    public val App: String,
    public val AppSummary: String,
    public val Network: String,
    public val NetworkSummary: String,
    public val Override: String,
    public val OverrideSummary: String,
    public val MetaFeatures: String,
    public val MetaFeaturesSummary: String,
)

public data class YumeStrings_Settings_More(
    public val Lab: String,
    public val LabSummary: String,
    public val Logs: String,
    public val LogsSummary: String,
    public val Onboarding: String,
    public val OnboardingSummary: String,
    public val About: String,
    public val AboutSummary: String,
)

public data class YumeStrings_Settings_Error(
    public val WebviewFailed: String,
)

public data class YumeStrings_Settings(
    public val Title: String,
    public val Section: YumeStrings_Settings_Section,
    public val UiSettings: YumeStrings_Settings_UiSettings,
    public val More: YumeStrings_Settings_More,
    public val Error: YumeStrings_Settings_Error,
)

// ==================== TrafficStatistics ====================

public data class YumeStrings_TrafficStatistics_TimeRange(
    public val Today: String,
    public val Week: String,
)

public data class YumeStrings_TrafficStatistics_Summary(
    public val TodayTraffic: String,
    public val WeekTraffic: String,
    public val Average: String,
    public val DayTrafficTip: String,
)

public data class YumeStrings_TrafficStatistics_Donut(
    public val Other: String,
)

public data class YumeStrings_TrafficStatistics_Action(
    public val Clear: String,
    public val ClearSuccess: String,
)

public data class YumeStrings_TrafficStatistics_Section(
    public val Traffic: String,
    public val TopApps: String,
    public val TodayApps: String,
    public val WeekApps: String,
    public val SystemTraffic: String,
    public val EmptyApps: String,
)

public data class YumeStrings_TrafficStatistics_Metric(
    public val Download: String,
    public val Upload: String,
    public val UsageLine: String,
    public val SortByName: String,
    public val SortByUsage: String,
)

public data class YumeStrings_TrafficStatistics_Weekday(
    public val Mon: String,
    public val Tue: String,
    public val Wed: String,
    public val Thu: String,
    public val Fri: String,
    public val Sat: String,
    public val Sun: String,
    public val Today: String,
)

public data class YumeStrings_TrafficStatistics(
    public val Title: String,
    public val EntrySummary: String,
    public val TimeRange: YumeStrings_TrafficStatistics_TimeRange,
    public val Summary: YumeStrings_TrafficStatistics_Summary,
    public val Donut: YumeStrings_TrafficStatistics_Donut,
    public val Action: YumeStrings_TrafficStatistics_Action,
    public val Section: YumeStrings_TrafficStatistics_Section,
    public val Metric: YumeStrings_TrafficStatistics_Metric,
    public val Weekday: YumeStrings_TrafficStatistics_Weekday,
)

// ==================== Util ====================

public data class YumeStrings_Util_Error(
    public val UnknownError: String,
)

public data class YumeStrings_Util(
    public val Error: YumeStrings_Util_Error,
)

