package com.github.yumeyucca.yumebox.core.locale

internal val ZhHansYumeStrings_Override: YumeStrings_Override = YumeStrings_Override(
    Title = "覆写配置",
    Action = YumeStrings_Override_Action(
        Create = "创建配置",
        New = "新建配置",
        NetworkImport = "链接导入",
    ),
    BuiltIn = YumeStrings_Override_BuiltIn(
        PreventDnsLeak = "防止 DNS 泄露",
        AddDirectRules = "添加直连规则",
        PuddingDog = "布丁狗的订阅转换",
        Acl4ssrOnlineFull = "ACL4SSR Online Full",
        CopyName = "%s (副本)",
    ),
    Empty = YumeStrings_Override_Empty(
        Title = "暂无覆写配置",
        Hint = "点击下方按钮创建新配置，或导入配置文件",
        UserTitle = "暂无导入覆写",
        UserHint = "可新建或导入 YAML / JS 覆写",
    ),
    Section = YumeStrings_Override_Section(
        BuiltIn = "内置覆写",
        User = "导入覆写",
    ),
    Status = YumeStrings_Override_Status(
        InUse = "使用中",
        NotInUse = "未使用",
        BuiltIn = "内置",
    ),
    Card = YumeStrings_Override_Card(
        Copy = "复制配置",
        Export = "导出配置",
        Edit = "编辑配置",
        Delete = "删除配置",
        EditButton = "编辑",
        DeleteButton = "删除",
        Apply = "应用配置",
        ApplyButton = "应用",
    ),
    ApplySheet = YumeStrings_Override_ApplySheet(
        Title = "应用到订阅",
        Empty = "暂无订阅",
        Success = "已更新应用范围",
        Failed = "更新应用范围失败：%s",
        Button = YumeStrings_Override_ApplySheet_Button(
            Cancel = "取消",
            Confirm = "确定",
        ),
    ),
    Import = YumeStrings_Override_Import(
        ReadError = "无法读取导入文件",
        Failed = "导入失败: %s",
        FileError = "读取文件失败: %s",
        NetworkError = "链接导入失败: %s",
        InvalidUrl = "请输入 HTTP 或 HTTPS 链接",
        HttpError = "HTTP 请求失败：%d",
        UnsupportedType = "仅支持 YAML 或 JS",
        EmptyJavaScript = "JS 文件为空",
    ),
    Export = YumeStrings_Override_Export(
        Failed = "导出失败：%s",
        Success = "已导出配置：%s",
    ),
    Dialog = YumeStrings_Override_Dialog(
        Create = YumeStrings_Override_Dialog_Create(
            Title = "添加配置",
            Name = "配置名称",
            Url = "配置链接",
            Type = "创建类型",
        ),
        Delete = YumeStrings_Override_Dialog_Delete(
            Title = "删除配置",
            InUseMessage = "配置 %s 正在被订阅使用，删除后将解除绑定关系.确定要删除吗？此操作不可恢复.",
            Message = "确定要删除配置 %s 吗？此操作不可恢复.",
        ),
        Button = YumeStrings_Override_Dialog_Button(
            Cancel = "取消",
            Delete = "删除",
        ),
    ),
    Draft = YumeStrings_Override_Draft(
        BasicRouting = "基础分流",
        ServiceRouting = "服务分流",
    ),
    Save = YumeStrings_Override_Save(
        ImportDefaultName = "导入的覆写配置",
        Failed = "保存覆写失败",
    ),
    Label = YumeStrings_Override_Label(
        RulesReplace = "覆盖规则",
    ),
)

internal val ZhYumeStrings_Override: YumeStrings_Override = YumeStrings_Override(
    Title = "覆寫設定",
    Action = YumeStrings_Override_Action(
        Create = "建立設定",
        New = "新增設定",
        NetworkImport = "連結匯入",
    ),
    BuiltIn = YumeStrings_Override_BuiltIn(
        PreventDnsLeak = "防止 DNS 洩漏",
        AddDirectRules = "新增直連規則",
        PuddingDog = "布丁狗的訂閱轉換",
        Acl4ssrOnlineFull = "ACL4SSR Online Full",
        CopyName = "%s (副本)",
    ),
    Empty = YumeStrings_Override_Empty(
        Title = "暫無覆寫設定",
        Hint = "點下方按鈕建立新設定，或匯入設定檔",
        UserTitle = "暫無匯入覆寫",
        UserHint = "可新增或匯入 YAML / JS 覆寫",
    ),
    Section = YumeStrings_Override_Section(
        BuiltIn = "內建",
        User = "已匯入",
    ),
    Status = YumeStrings_Override_Status(
        InUse = "使用中",
        NotInUse = "未使用",
        BuiltIn = "內建",
    ),
    Card = YumeStrings_Override_Card(
        Copy = "複製設定",
        Export = "匯出設定",
        Edit = "編輯設定",
        Delete = "刪除設定",
        EditButton = "編輯",
        DeleteButton = "刪除",
        Apply = "套用設定",
        ApplyButton = "套用",
    ),
    ApplySheet = YumeStrings_Override_ApplySheet(
        Title = "套用到訂閱",
        Empty = "暫無訂閱",
        Success = "已更新套用範圍",
        Failed = "更新套用範圍失敗：%s",
        Button = YumeStrings_Override_ApplySheet_Button(
            Cancel = "取消",
            Confirm = "確定",
        ),
    ),
    Import = YumeStrings_Override_Import(
        ReadError = "無法讀取匯入檔案",
        Failed = "匯入失敗: %s",
        FileError = "讀取檔案失敗: %s",
        NetworkError = "連結匯入失敗: %s",
        InvalidUrl = "請輸入 HTTP 或 HTTPS 連結",
        HttpError = "HTTP 請求失敗：%d",
        UnsupportedType = "僅支援 YAML 或 JS",
        EmptyJavaScript = "JS 檔案為空",
    ),
    Export = YumeStrings_Override_Export(
        Failed = "匯出失敗：%s",
        Success = "已匯出設定：%s",
    ),
    Dialog = YumeStrings_Override_Dialog(
        Create = YumeStrings_Override_Dialog_Create(
            Title = "新增設定",
            Name = "設定名稱",
            Url = "設定連結",
            Type = "建立類型",
        ),
        Delete = YumeStrings_Override_Dialog_Delete(
            Title = "刪除設定",
            InUseMessage = "設定 %s 正在被訂閱使用，刪除後將解除綁定關係.確定要刪除嗎？此操作無法復原.",
            Message = "確定要刪除設定 %s 嗎？此操作無法復原.",
        ),
        Button = YumeStrings_Override_Dialog_Button(
            Cancel = "取消",
            Delete = "刪除",
        ),
    ),
    Draft = YumeStrings_Override_Draft(
        BasicRouting = "基礎分流",
        ServiceRouting = "服務分流",
    ),
    Save = YumeStrings_Override_Save(
        ImportDefaultName = "匯入的覆寫設定",
        Failed = "儲存覆寫失敗",
    ),
    Label = YumeStrings_Override_Label(
        RulesReplace = "覆蓋規則",
    ),
)

internal val EnYumeStrings_Override: YumeStrings_Override = YumeStrings_Override(
    Title = "Override Configs",
    Action = YumeStrings_Override_Action(
        Create = "Create Config",
        New = "New Config",
        NetworkImport = "Import from Link",
    ),
    BuiltIn = YumeStrings_Override_BuiltIn(
        PreventDnsLeak = "Prevent DNS Leaks",
        AddDirectRules = "Add Direct Rules",
        PuddingDog = "Pudding Dog Subscription Conversion",
        Acl4ssrOnlineFull = "ACL4SSR Online Full",
        CopyName = "%s (Copy)",
    ),
    Empty = YumeStrings_Override_Empty(
        Title = "No override configs",
        Hint = "Click the button below to create a new config, or import a config file",
        UserTitle = "No imported overrides",
        UserHint = "Create or import a YAML / JS override",
    ),
    Section = YumeStrings_Override_Section(
        BuiltIn = "Built-in",
        User = "Imported",
    ),
    Status = YumeStrings_Override_Status(
        InUse = "In Use",
        NotInUse = "Not in Use",
        BuiltIn = "Built-in",
    ),
    Card = YumeStrings_Override_Card(
        Copy = "Copy Config",
        Export = "Export Config",
        Edit = "Edit Config",
        Delete = "Delete Config",
        EditButton = "Edit",
        DeleteButton = "Delete",
        Apply = "Apply Config",
        ApplyButton = "Apply",
    ),
    ApplySheet = YumeStrings_Override_ApplySheet(
        Title = "Apply to Subscriptions",
        Empty = "No subscriptions",
        Success = "Application scope updated",
        Failed = "Failed to update application scope: %s",
        Button = YumeStrings_Override_ApplySheet_Button(
            Cancel = "Cancel",
            Confirm = "Confirm",
        ),
    ),
    Import = YumeStrings_Override_Import(
        ReadError = "Cannot read import file",
        Failed = "Import failed: %s",
        FileError = "Failed to read file: %s",
        NetworkError = "Link import failed: %s",
        InvalidUrl = "Enter an HTTP or HTTPS URL",
        HttpError = "HTTP request failed: %d",
        UnsupportedType = "Only YAML or JS is supported",
        EmptyJavaScript = "JS file is empty",
    ),
    Export = YumeStrings_Override_Export(
        Failed = "Export failed: %s",
        Success = "Exported config: %s",
    ),
    Dialog = YumeStrings_Override_Dialog(
        Create = YumeStrings_Override_Dialog_Create(
            Title = "Add Config",
            Name = "Config Name",
            Url = "Config URL",
            Type = "Creation Type",
        ),
        Delete = YumeStrings_Override_Dialog_Delete(
            Title = "Delete Config",
            InUseMessage = "Config %s is being used by subscriptions. Deleting will unbind the relationship. Are you sure? This action cannot be undone.",
            Message = "Are you sure you want to delete config %s? This action cannot be undone.",
        ),
        Button = YumeStrings_Override_Dialog_Button(
            Cancel = "Cancel",
            Delete = "Delete",
        ),
    ),
    Draft = YumeStrings_Override_Draft(
        BasicRouting = "Basic Routing",
        ServiceRouting = "Service Routing",
    ),
    Save = YumeStrings_Override_Save(
        ImportDefaultName = "Imported Override Config",
        Failed = "Failed to save override",
    ),
    Label = YumeStrings_Override_Label(
        RulesReplace = "Override Rules",
    ),
)

internal val JaYumeStrings_Override: YumeStrings_Override = YumeStrings_Override(
    Title = "上書き設定",
    Action = YumeStrings_Override_Action(
        Create = "設定を作成",
        New = "新規設定",
        NetworkImport = "リンクからインポート",
    ),
    BuiltIn = YumeStrings_Override_BuiltIn(
        PreventDnsLeak = "DNS リークを防止",
        AddDirectRules = "直通ルールを追加",
        PuddingDog = "Pudding Dog 購読変換",
        Acl4ssrOnlineFull = "ACL4SSR Online Full",
        CopyName = "%s (コピー)",
    ),
    Empty = YumeStrings_Override_Empty(
        Title = "上書き設定がありません",
        Hint = "下のボタンで新規作成するか、設定ファイルをインポート",
        UserTitle = "インポート済みの上書きがありません",
        UserHint = "YAML / JS の上書きを作成またはインポートできます",
    ),
    Section = YumeStrings_Override_Section(
        BuiltIn = "組み込み",
        User = "インポート済み",
    ),
    Status = YumeStrings_Override_Status(
        InUse = "使用中",
        NotInUse = "未使用",
        BuiltIn = "組み込み",
    ),
    Card = YumeStrings_Override_Card(
        Copy = "設定をコピー",
        Export = "設定を書き出し",
        Edit = "設定を編集",
        Delete = "設定を削除",
        EditButton = "編集",
        DeleteButton = "削除",
        Apply = "設定を適用",
        ApplyButton = "適用",
    ),
    ApplySheet = YumeStrings_Override_ApplySheet(
        Title = "購読へ適用",
        Empty = "購読がありません",
        Success = "適用範囲を更新しました",
        Failed = "適用範囲の更新に失敗しました: %s",
        Button = YumeStrings_Override_ApplySheet_Button(
            Cancel = "キャンセル",
            Confirm = "確定",
        ),
    ),
    Import = YumeStrings_Override_Import(
        ReadError = "インポートファイルを読み取れません",
        Failed = "インポートに失敗しました: %s",
        FileError = "ファイルの読み取りに失敗しました: %s",
        NetworkError = "リンクのインポートに失敗しました: %s",
        InvalidUrl = "HTTP または HTTPS の URL を入力してください",
        HttpError = "HTTP 要求に失敗しました: %d",
        UnsupportedType = "YAML または JS のみ対応",
        EmptyJavaScript = "JS ファイルが空です",
    ),
    Export = YumeStrings_Override_Export(
        Failed = "書き出しに失敗しました: %s",
        Success = "設定を書き出しました: %s",
    ),
    Dialog = YumeStrings_Override_Dialog(
        Create = YumeStrings_Override_Dialog_Create(
            Title = "設定を追加",
            Name = "設定名",
            Url = "設定 URL",
            Type = "作成種別",
        ),
        Delete = YumeStrings_Override_Dialog_Delete(
            Title = "設定を削除",
            InUseMessage = "設定 %s は購読で使用中です.削除すると関連付けが解除されます.本当に削除しますか？この操作は元に戻せません.",
            Message = "設定 %s を削除しますか？この操作は元に戻せません.",
        ),
        Button = YumeStrings_Override_Dialog_Button(
            Cancel = "キャンセル",
            Delete = "削除",
        ),
    ),
    Draft = YumeStrings_Override_Draft(
        BasicRouting = "基本ルーティング",
        ServiceRouting = "サービスルーティング",
    ),
    Save = YumeStrings_Override_Save(
        ImportDefaultName = "インポートした上書き設定",
        Failed = "上書き設定の保存に失敗しました",
    ),
    Label = YumeStrings_Override_Label(
        RulesReplace = "ルールを上書き",
    ),
)

internal val RuYumeStrings_Override: YumeStrings_Override = YumeStrings_Override(
    Title = "Переопределения",
    Action = YumeStrings_Override_Action(
        Create = "Создать конфигурацию",
        New = "Новая конфигурация",
        NetworkImport = "Импорт по ссылке",
    ),
    BuiltIn = YumeStrings_Override_BuiltIn(
        PreventDnsLeak = "Предотвратить утечки DNS",
        AddDirectRules = "Добавить правила прямого доступа",
        PuddingDog = "Конвертация подписки Pudding Dog",
        Acl4ssrOnlineFull = "ACL4SSR Online Full",
        CopyName = "%s (копия)",
    ),
    Empty = YumeStrings_Override_Empty(
        Title = "Нет переопределений",
        Hint = "Нажмите кнопку ниже, чтобы создать конфигурацию, или импортируйте файл",
        UserTitle = "Нет импортированных переопределений",
        UserHint = "Создайте или импортируйте YAML / JS переопределение",
    ),
    Section = YumeStrings_Override_Section(
        BuiltIn = "Встроенные",
        User = "Импортированные",
    ),
    Status = YumeStrings_Override_Status(
        InUse = "Используется",
        NotInUse = "Не используется",
        BuiltIn = "Встроенное",
    ),
    Card = YumeStrings_Override_Card(
        Copy = "Копировать конфигурацию",
        Export = "Экспорт конфигурации",
        Edit = "Изменить конфигурацию",
        Delete = "Удалить конфигурацию",
        EditButton = "Изменить",
        DeleteButton = "Удалить",
        Apply = "Применить конфигурацию",
        ApplyButton = "Применить",
    ),
    ApplySheet = YumeStrings_Override_ApplySheet(
        Title = "Применить к подпискам",
        Empty = "Нет подписок",
        Success = "Область применения обновлена",
        Failed = "Не удалось обновить область применения: %s",
        Button = YumeStrings_Override_ApplySheet_Button(
            Cancel = "Отмена",
            Confirm = "Подтвердить",
        ),
    ),
    Import = YumeStrings_Override_Import(
        ReadError = "Не удалось прочитать файл импорта",
        Failed = "Импорт не удался: %s",
        FileError = "Не удалось прочитать файл: %s",
        NetworkError = "Импорт по ссылке не удался: %s",
        InvalidUrl = "Введите URL HTTP или HTTPS",
        HttpError = "HTTP-запрос не удался: %d",
        UnsupportedType = "Поддерживаются только YAML или JS",
        EmptyJavaScript = "JS-файл пуст",
    ),
    Export = YumeStrings_Override_Export(
        Failed = "Экспорт не удался: %s",
        Success = "Конфигурация экспортирована: %s",
    ),
    Dialog = YumeStrings_Override_Dialog(
        Create = YumeStrings_Override_Dialog_Create(
            Title = "Добавить конфигурацию",
            Name = "Имя конфигурации",
            Url = "URL конфигурации",
            Type = "Тип создания",
        ),
        Delete = YumeStrings_Override_Dialog_Delete(
            Title = "Удалить конфигурацию",
            InUseMessage = "Конфигурация %s используется подписками. Удаление снимет привязку. Удалить? Это действие нельзя отменить.",
            Message = "Удалить конфигурацию %s? Это действие нельзя отменить.",
        ),
        Button = YumeStrings_Override_Dialog_Button(
            Cancel = "Отмена",
            Delete = "Удалить",
        ),
    ),
    Draft = YumeStrings_Override_Draft(
        BasicRouting = "Базовая маршрутизация",
        ServiceRouting = "Сервисная маршрутизация",
    ),
    Save = YumeStrings_Override_Save(
        ImportDefaultName = "Импортированное переопределение",
        Failed = "Не удалось сохранить переопределение",
    ),
    Label = YumeStrings_Override_Label(
        RulesReplace = "Переопределить правила",
    ),
)

