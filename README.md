# ComfortablePandaApp (パンダアプリ)

このプロジェクトは、Android向けのパンダアプリです。
友達への引き継ぎ・作業依頼用のドキュメントです。

## 1. 環境構築

### 必須要件

- **Android Studio**: 最新版（Ladybug以降推奨）
- **JDK**: 11以上 (Android Studio同梱のものでOK)

### プロジェクトのセットアップ

1. Android Studioを開き、`Open` からこのフォルダを選択してください。
2. Gradleの同期（Sync）が完了するのを待ちます。
3. エミュレータまたは実機を接続して、実行（Run）ボタンを押して動作確認してください。

## 2. 調整・開発について

主なソースコードは `app/src/main/java/com/example/pandaapp` 配下にあります。
UIは Jetpack Compose で記述されています。

- **MainActivity**: アプリのエントリーポイント (`app/src/main/java/com/example/pandaapp/ui/MainActivity.kt`)
- **ui/**: 画面ごとのComposeコンポーネント
- **util/**: ユーティリティクラス

## 3. Google Play ストアへのアップロード手順

Google Play Consoleへアップロードするためのリリースビルド（AABファイル）の作成手順です。

### 手順

1. Android Studioのメニューから **Build** > **Generate Signed Bundle / APK...** を選択します。
2. **Android App Bundle** を選択し、Nextをクリックします。
3. **Key store path** の設定:
    - すでにキーストアファイル（`.jks`）がある場合は、そのパスを指定してください。
    - **注意**: キーストアのパスワードとキーのパスワードは別途共有します（または新規作成してください）。
    - 新規作成する場合は `Create new...` から作成し、パスワードを忘れないようにメモしてください。
4. **Build Variants** で `release` を選択し、Finishをクリックします。
5. ビルドが完了すると、`app/release/app-release.aab` (または `app/build/outputs/bundle/release/`) にファイルが生成されます。

## 4. Google Play Console への登録

1. [Google Play Console](https://play.google.com/console/) にログインします。
2. アプリを選択（または作成）します。
3. 左メニューの **製品版 (Production)** または **内部テスト (Internal testing)** などを選択します。
4. **新しいリリースを作成** をクリックします。
5. 先ほど作成した `.aab` ファイルをアップロードします。
6. リリースノートなどを入力し、**リリースのレビュー** -> **ロールアウト** へと進んでください。

## 5. 内部仕様 (Internal Specifications)

### アーキテクチャ概要
- **UI**: Jetpack Compose を全面的に採用しています。
- **非同期処理**: Kotlin Coroutines / WorkManager
- **データ保存**:
    - **課題データ**: `SharedPreferences` に JSON 文字列として保存 (`AssignmentStore`)。シリアライズには `kotlinx.serialization` を使用。
    - **認証情報**: `EncryptedSharedPreferences` を使用して安全に保存 (`CredentialsStore`)。

### 主要コンポーネント

#### 1. データ層 (`data/`)
- **PandaRepository**: データの取得を抽象化します。現在は `PandaApiClient` を経由してネットワークから課題を取得します。
- **AssignmentStore**: 取得した課題リストをローカルにキャッシュします。

#### 2. バックグラウンド処理 (`worker/`)
- **AssignmentFetchWorker**: 定期的にバックグラウンドで課題を取得する `CoroutineWorker` です。
    - 認証情報がある場合のみ実行されます。
    - 新しい課題があるかチェックし、あれば通知 (`NewAssignmentNotifier`) を送ります。
    - 取得後、ホーム画面のウィジェット (`AssignmentWidgetProvider`) の更新もトリガーします。

#### 3. ウィジェット (`widget/`)
- **AssignmentWidgetProvider**: ホーム画面に課題リストを表示するウィジェットです。
- `AssignmentFetchWorker` の完了時や、アプリ内での更新時に再描画されます。

#### 4. ユーティリティ (`util/`)
- **AssignmentUpdateProcessor**: 新旧の課題リストを比較し、新規課題の検出ロジックを担当します。
- **CredentialsStore**: ユーザー名とパスワードの暗号化保存・読み出しを担当します。

## その他

- `local.properties` はgitignoreされているため、SDKのパスなどが合わない場合は自動生成されるか確認してください。
- 何か不明点があれば連絡ください！

## 6. TODOリスト

### TODO (細かめ)

- [ ] **テスト、クイズも取得する**
    - 現在は課題(Assignment)のみ取得しているため、テストやクイズも取得対象に含める。
    - 関連ファイル: `data/api/PandaApiClient.kt`, `data/model/Assignment.kt`
- [ ] **ウィジェットのUIがバグる**
    - レイアウト崩れや更新タイミングの問題を修正する。
    - 関連ファイル: `widget/AssignmentWidgetProvider.kt`, `res/layout/widget_assignment.xml`, `res/layout/widget_assignment_item.xml`
- [ ] **Pandaのメンテナンス中も取得しようとしている**
    - メンテナンス中やサーバーダウン時のエラーハンドリングを追加し、無駄なリトライやクラッシュを防ぐ。
    - 関連ファイル: `worker/AssignmentFetchWorker.kt`, `data/api/PandaApiClient.kt`
- [ ] **Pandaの課題取得失敗時に、課題0件として内部ストレージが上書きされる**
    - 取得失敗時（空リストやエラー時）に、既存のキャッシュをクリアしないように修正する。
    - 関連ファイル: `worker/AssignmentFetchWorker.kt`, `util/AssignmentStore.kt`
- [ ] **毎回履修科目を取得しているが、定期的で良い。その時に課題URLも取得したい**
    - 毎回全コースを取得するのではなく、キャッシュを活用するか頻度を下げる。また、課題の詳細URLも取得できるようにする。
    - 関連ファイル: `data/repository/PandaRepository.kt`, `data/api/PandaApiClient.kt`

### TODO (でかめ)

- [ ] **内部ブラウザ実装**
    - 課題をタップした際に、外部ブラウザではなくアプリ内のブラウザで開き、ログイン状態を維持したまま直ぐにアクセスできるようにする。
    - 関連ファイル: `ui/` (新規WebView画面など), `ui/MainActivity.kt`

