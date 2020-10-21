# AANotifier

Android Auto未対応のアプリの通知をAndroid Autoヘッドユニットに表示します。

## 必要アプリ・機器
- Android 8.0以上の端末
- [Android Autoアプリ](https://play.google.com/store/apps/details?id=com.google.android.projection.gearhead)
- Android Autoヘッドユニット (Android Auto対応ナビなど)

## インストール
1. 本アプリはAndroid Autoのポリシーに準拠していないため、Google Playストアから配布できません。したがって、以下の手順に従ってインストールしてください。
1. [リリースページ](https://github.com/sckzw/AANotifier/releases)からAPKファイルをダウンロードし、端末にアプリをインストールしてください(詳細は割愛します)。
1. [こちらのページの手順1](https://developer.android.com/training/cars/testing?hl=ja#step1)を参照してAndroid Autoの「提供元不明のアプリ」のチェックを有効にしてください。

## 設定
1. AANotifierのメイン画面の「Android Auto通知」のチェックが有効になっていることを確認してください。
1. AANotifierのメイン画面の「通知へのアクセス」をタップしてください。
1. 表示された画面で、AANotifierのチェックを有効にしてください。
1. AANotifierのメイン画面の「アプリ一覧」をタップしてください。
1. 表示された画面で、Android Autoに通知を表示したいアプリのチェックを有効にしてください。

## 使用方法
1. Android Autoヘッドユニットに端末を接続し、Android Autoを起動してください。
1. AANotifierは常に表示しておく必要はないため画面を閉じてください。
1. 有効にしたアプリが通知を発行すると、Android Autoヘッドユニットにも通知が表示されます。

## 注意事項
1. 車載時に見る必要がない通知は有効にしないでください。
1. 通知内容は停車中に確認してください。
1. AANotifierの「テスト向け設定」は特に指示がなければ変更しないでください。

![AA Notification](https://user-images.githubusercontent.com/4351207/96069583-7ba47680-0ed9-11eb-8360-063d5e9dc99f.png)

![AA Notification Area](https://user-images.githubusercontent.com/4351207/96069606-84954800-0ed9-11eb-9e66-a7051e8e67f0.png)

![MainActivity](https://user-images.githubusercontent.com/4351207/96069645-9a0a7200-0ed9-11eb-882f-b1a1af4570d6.jpg)

![AppListActivity](https://user-images.githubusercontent.com/4351207/96069699-b1495f80-0ed9-11eb-98e2-168d253c4121.jpg)

![Notification Access](https://user-images.githubusercontent.com/4351207/96069713-b9090400-0ed9-11eb-8e49-dfbcbdca5f4c.jpg)

# Main source code file
[MessagingService.java](https://github.com/sckzw/AANotifier/blob/master/mobile/src/main/java/io/github/sckzw/aanotifier/MessagingService.java "MessagingService.java")
