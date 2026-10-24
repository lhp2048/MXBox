# MXBox 定制功能

MXBox 基于 [FongMi/TV](https://github.com/FongMi/TV)。安装包名是 `com.mx.mxbox`，应用名是 MXBox。图标、横幅和壁纸也换成了 MXBox 自己的。上游说明仍在 `README.md`。本文只记相对上游已经落地的定制。

合并上游新版本时，先读文末「移植核对」，按里面的文件把定制接回去，再对照前面各节看行为有没有丢。Java 包名仍是 `com.fongmi.android.tv`，不要改成 `com.mx.mxbox`。电视版 flavor 名叫 `leanback`，不是旧的 `tv`。

更细的设计稿：

- 个人源和直播：`docs/superpowers/specs/2026-10-24-feed-source-live.md`
- 电视端 B 站：`docs/superpowers/specs/2026-10-07-bilibili-tv-design.md`
- 射手网字幕：`docs/superpowers/specs/2026-10-20-assrt-subtitle-design.md`

## 功能支持

「手机浏览器」是扫电视二维码后打开的网页，控制的是电视。网页只由电视安装包提供。手机安装包不提供设置页、远程控制和设备发现，局域网里搜不到这台手机。手机自己播放时仍使用本机代理，这个地址不对外。

| 功能 | 电视安装包 | 手机安装包 | 手机浏览器 |
| --- | --- | --- | --- |
| 网页设置 | 有 | 没有 | 只能打开电视上的网页 |
| 网页底栏 | 有 | 没有 | 默认三个入口，中间三角展开其余页面 |
| 网页标题和图标 | 有 | 没有 | 标题和图标都是 MXBox |
| 个人源和直播 | 有 | 有，在点播页里切换 | 有，可填源地址 |
| 首页标题「站点 · 源名称」 | 有 | 有 | 没有 |
| 首页按钮隐藏和排序 | 有 | 没有 | 有 |
| B 站页面 | 有 | 没有 | 没有页面，可过滤标题、推送搜索 |
| 幕布 | 有 | 没有 | 有 |
| 全局弹幕 | 有，只盖在本机页面上 | 有，只盖在本机页面上 | 有，发给正在连的那台设备 |
| 射手网字幕 | 有，挂到正在播的点播 | 有，挂到正在播的点播 | 有，搜索并选择字幕 |
| 投屏接收 | 有 | 没有 | 没有 |
| 检查更新 | 有 | 有 | 没有 |
| 更新图标和下载百分比 | 有 | 没有 | 没有 |

## 网页

连着电视的网页左上角是 MXBox 图标，标题是 MXBox。浏览器标签是「MXBox · 当前页」。遥控器、日志、爬虫测试这几个页面同样使用这个图标，标签也带 MXBox。

底栏固定三个入口。左边是输入，右边是设置。中间一开始是播放，上方有一个三角。点三角向上展开：播放、字幕、本地、Bilibili、幕布。选中某一项后，中间按钮换成这一项，再点中间就进入这一页。点页面空白处会收起菜单。

原来的数字地址仍然有效：`1` 搜索、`2` 推送、`3` 弹幕、`4` 设置、`5` 本地、`6` Bilibili、`7` 幕布、`8` 在线字幕。扫码打开这些页面时，如果它属于中间那一档，中间按钮会跟着换成这一页。字幕页顶部是本地字幕、在线字幕、弹幕。`8` 直接打开在线字幕。

输入页的搜索按电视当前打开的页面分发。停在 B 站页面时，关键词送到这一页并开始搜索。停在其他页面时，打开电视自己的搜索。

## 检查更新

首页启动会自动检查，设置里也可以手动检查。地址是 `https://api.github.com/repos/lhp2048/MXBox/releases`。只认正式版，标签必须是 `vX.Y.Z`，而且比当前版本新。电视安装包的文件名是 `leanback-arm64_v8a.apk`。没有新版本时，手动检查会提示「已是最新版本」。

查到更新后：

- 弹出是否更新，并在首页左上角 Logo 的左侧显示更新图标。
- 点取消只关掉提示，图标还在。再点图标会重新弹出，可以选择更新或取消。
- 确定更新后开始下载。图标旁显示百分比。对话框里有进度条、已下载大小、速度和剩余时间。
- 正在下载时再点图标，只提示「正在下载」。
- 下载完会校验安装包。这时再点图标仍先打开对话框，点「安装」才进入系统安装界面，不会重新下载。

发布时把更新说明写进该版本的正文。普通条目会整段显示。电视和手机要分开写时，用 `# 电视版` 和 `# 手机版` 做标题，电视上只显示电视那一段。正文为空时，提示「这次发布没有写更新说明。」

```
# 更新说明

- 检查更新改为 MXBox 自己的仓库
- 查到新版本时弹出是否更新，并在首页 Logo 左侧显示更新图标
- 下载时图标旁显示百分比，对话框显示进度、速度和剩余时间
- 网页设置里可以隐藏、排序电视首页按钮
- 个人源和直播、电视端 B 站、全局弹幕、幕布、射手网字幕
```

## 首页按钮

电视首页默认从左到右是：Bilibili、幕布、点播、直播、搜索、收藏、推送、设置。没有直播源时不显示直播。启动后光标落在最左侧第一个仍显示的按钮上。

连着电视的网页里，打开设置页的「首页按钮」。勾选后显示在电视首页，并用上移、下移改顺序。没有直播源时，直播即使排在列表里也不会出现在电视上。

## 个人源和直播

连着电视的网页可以填写源服务器。打开设置页时，已经保存的地址会显示在输入框里。只填 `IP:端口` 时，会补上 `/api/v1/tvbox/feed.json`；填完整 JSON 网址则原样使用。保存后输入框改成实际使用的地址。

接口返回两类数据：

- `urls`：点播源列表。同一时间使用其中一个。
- `lives`：自己的直播频道，合并进当前源解析出的直播列表。

电视首页和手机点播页点 Logo，都会打开「切换配置源」。点播和直播分开选。可以添加源、查看用过的源，并对列表做测速。首页标题显示「当前站点 · 源名称」。

一份点播配置里如果还嵌了多份源，加载时会逐份合并，并显示进度。手机点播页会先用刚才看过的首页，再在后台刷新。

电视设置里可以开关「投屏接收」，修改手机上看到的设备名，并查看当前状态和电视 IP。默认开启。开机后如果仍开着，会自己开始接收。手机和电视要在同一个 Wi-Fi，在别的 App 里选择这个设备名即可投过来。

安装包里带一份内置点播源。还没有自己选过配置时，如果安装包写了远程配置地址，并且远程的 `version` 比本机高，启动时会换成那份配置。自己选过之后，不再自动替换。

第一次打开应用时，播放器默认用 Exo。

应用停在前台时屏幕保持常亮，不进入系统休眠，也不进入系统屏保。切到别的应用或退出后，再交给系统。暂停播放也不会单独关掉常亮。电视端五分钟没有按键，会自己打开幕布当屏保，见下面「幕布」。

## 电视端 B 站

首页「Bilibili」打开单独一页，不进入点播配置，也不使用原来的收藏页。未登录时显示二维码，用手机 B 站 App 扫码。登录态保存在这台电视上。

登录后默认播推荐，并记住上一次推荐里的第一条，下次仍排在最前。频道有四个：推荐、我的关注、收藏视频、收藏博主。播放中屏幕保持常亮。关注、本地收藏、搜索、UP 投稿、清晰度和播放条的其余行为见上面的 B 站设计稿。收藏视频不读 B 站收藏夹。收藏视频和收藏博主都留在这台电视上，退出登录后还在。

搜索框里有搜索历史和搜索联想。也可以调出二维码，手机扫码后把搜索词发到电视。手机推送里的 B 站搜索同样会把关键词送到这一页并开始搜索。

打开搜索时，正在播的视频会暂停。结果是铺满屏幕的卡片。停在某一条视频上大约两秒，右下角会小窗预览；确认后回到全屏并离开搜索。关掉搜索会清空输入，并从暂停的位置继续原来的视频。进度条两侧显示当前时间和总时长。

连着电视的网页里，「Bilibili 过滤」有一个开关和一个输入框。词用空格分开。开关打开时，推荐和搜索结果里，标题包含任一词语的视频不显示。关注、收藏和 UP 名字不过滤。

## 全局弹幕

连着电视的网页发弹幕，仍用原来的格式：`[时间,模式,字号,颜色]文字`。模式有滚动、顶部、底部、反向。

这条弹幕显示在当前 MXBox 页面上，不限于点播和直播。换页时还在飞的弹幕会继续。它不写入视频自己的弹幕文件，也不会出现在别的应用上。

## 幕布

首页「幕布」打开全屏页面。屏幕保持常亮。如果点播或直播正在播，进入幕布时暂停。

电视端把幕布当作应用自己的屏保。任意页面五分钟没有按键，幕布盖在当前页面上。正在播放的点播、直播、B 站不盖；这三处已经暂停时可以盖。按任意键或点屏幕只关掉幕布，回到刚才的页面，停在原来的暂停位置，不会自动继续播。已经停在幕布上时不再叠一层。从首页自己打开的幕布仍是菜单键出二维码、返回键退出。屏保打开的那一层，任意键都是退出，不弹二维码。手机版没有幕布，也没有这个屏保。

没有背景图、背景视频和常驻文字时，画面中间是默认方案：时钟显示到秒，下面是公历年月日和星期，再下面是农历年月日（含生肖、闰月）和当前节气。设了背景图、背景视频或常驻文字后，这组时钟让开。定时文字结束后，如果仍是默认背景，时钟回来。

遥控器菜单键在画面中间显示二维码，再按菜单或返回只关掉二维码。手机扫码后打开幕布控制页。只有幕布停在前台时，手机才能改它。

手机打开后先是推送：

- 可以推文字或图片。
- 每次在屏幕上随机位置。文字再随机字号和颜色，图片随机大小，然后一直从右往左滚动。
- 这些推送不记住。退出幕布后消失。
- 「已推送」里列出正在滚动的文字和图片，可以勾选后删除，也可以全部清除。推送页上的「全部清除」作用相同。

「高级设置」里的内容会保存，下次打开幕布沿用：

- 背景色、背景图、背景视频，以及完整显示或铺满。
- 声音用视频原声或背景音乐。音乐可以上传、删除、列表循环、单曲循环、随机播放。音量只影响幕布，不改系统音量。有背景视频时，默认听视频原声。
- 常驻文字留在画面中间。定时显示的那一句结束后，恢复已保存的常驻文字。
- 「恢复默认背景」去掉背景图、背景视频和常驻文字，背景色回到默认深色，画面回到时钟日历。已经上传的音乐还在，但会停下来。
- 「全部清空」同样回到时钟日历，并删掉背景图、背景视频和音乐。这两项都会清掉正在滚动的推送。

## 射手网字幕

连着电视的网页里，字幕页顶部有三个标签：本地字幕、在线字幕、弹幕。本地字幕上传本机字幕文件。在线字幕按片名搜索，并把选中的文本字幕挂到当前正在播放的点播上。Token 在搜索按钮旁边的设置图标里保存，写在电视上，不放进安装包。还没在这一页保存密钥时，搜索提示「请先保存密钥」。打开在线字幕时，如果电视正在播点播，会把片名和当前集名称填进搜索框。详细规则见上面的字幕设计稿。

## 移植核对

下面每条都是已经落地的改动。合上游时如果对应文件被覆盖，按这里接回去。个人源和直播的需求在 `docs/superpowers/specs/2026-10-24-feed-source-live.md`。`REQ.txt` 只留最初那份记录，不要拿它覆盖这份说明或那份需求。

### 安装包和构建

- `app/build.gradle`：`applicationId` 是 `com.mx.mxbox`，`namespace` 保持 `com.fongmi.android.tv`。`versionName` 目前是 `1.0.0`，`versionCode` 是 `100`。设备维度只有 `leanback` 和 `mobile`。ABI 只打 `arm64-v8a`，`universalApk` 为 false。发布包文件名是 `leanback-arm64_v8a.apk` 和 `mobile-arm64_v8a.apk`。`local.properties` 里有 `storeFile` 就用它签名，否则 release 用 debug 签名。不要自己造一套 keystore。
- `minSdk` 是 24。跑在电视上的代码不要用 `java.nio.file.Files`。
- 依赖仓库用国内镜像，写在 `settings.gradle`、`buildSrc/build.gradle`、`runtime/settings.gradle`、`runtime/buildSrc/build.gradle`、`chaquo/runtime/settings.gradle`、`chaquo/runtime/buildSrc/build.gradle`。不要改回 `google()`、`mavenCentral()`、`gradlePluginPortal()`。JitPack 只留给 `com.github.*`。
- `app/src/main/java/androidx/media3/**` 是编译垫片。当前引用的媒体库还没有这些类型时必须留着，否则电视版编不过。上游媒体库补齐同名类之后，再删掉垫片并对齐真正的方法，不要两套并存。`PlayerManager` 里光盘菜单暂时固定返回不可用，也是为了能编过这一版。

### 名称、图标、首次启动

- 名称和图标：`MxBoxConstants`、`app/src/main/res/mipmap-*`、`drawable-*/ic_mx_launcher_foreground.png`、`drawable-nodpi/ic_logo.png`、`drawable-nodpi/artwork.png`、`drawable-nodpi/wallpaper_1.webp`。电视横幅在 `app/src/leanback/res/drawable*/ic_banner*`。生成脚本是 `scripts/generate_launcher_icons.py`、`scripts/generate_banner_icons.py`、`scripts/generate_wallpaper.py`。
- `MxBoxBootstrap` 在 `App` 启动时调用。第一次打开把播放器设成 Exo，并记下 `mx_initialized`。内置源在 `assets/built_in_sources.json` 和 `assets/default_config.json`。用户还没自己选过配置时，`MxBoxConfigUpdater` 才允许用安装包里的远程配置地址按 `version` 替换。选过之后写 `mx_user_configured`，不再自动换。
- 前台常亮写在 `App.onActivityResumed`，用 `FLAG_KEEP_SCREEN_ON`。`onActivityPaused` 清掉。点播页和 B 站页不要在暂停播放时再清这个标记，否则首页、暂停中的播放页仍会进休眠和屏保。
- 应用内屏保只在电视端。`curtain/CurtainSaver.java` 的 `IDLE_MS` 是五分钟。leanback `BaseActivity` 在 `onResume` 开始计时，`onPause` 只停掉自己这次计时，避免上一页的 `onPause` 清掉下一页刚开始的计时。窗口回调在按键和触摸时重新计时。到时调用 `CurtainActivity.startSaver`，带 `saver`。点播页 `blocksCurtainSaver`、B 站页同名方法，以及还在播的 `PlaybackService`，都会挡住。暂停、播完、没有在播，可以盖。屏保这一层任意键、触摸、返回都是 `finish()`，不要走菜单键二维码。返回后不要调用播放。已经在 `CurtainActivity` 上时 `arm` 直接停表，避免再叠一层。手机版不要加。

### 个人源、直播、首页标题

- 新类都在 `api/config/`：`MxBoxFeedEndpoint`、`MxBoxFeedStore`、`MxBoxDepotFeed`、`MxBoxFeedLives`、`MxBoxSourceCatalog`、`MxBoxSourceProbe`、`MxBoxUserSourceStore`、`MxBoxConfigUpdater`、`ConfigCache`、`HomeResultCache`。`VodConfig` 负责把 feed 的 `urls` 当成点播源，并把 `lives` 并进直播。一份配置里再嵌多份源时，进度走 `ConfigLoadEvent`、`DepotLoadEvent` 和 `LoadProgressHelper`。手机点播页先用 `HomeResultCache` 里刚才看过的首页，再在后台刷新。
- 切换源的界面是 `MxBoxSourceDialog`、`MxBoxSourceAdapter`，以及 `res/layout/dialog_mx_source.xml`、`adapter_mx_source.xml`。电视首页和手机点播页点 Logo 都进这里。首页标题格式在 `MxBoxHomeTitle`。
- 网页源地址：`Action` 的 `do=feed`。打开设置页要先 GET 已保存地址，填进 `#feed_server`；POST 保存。只写 `IP:端口` 时补上 `/api/v1/tvbox/feed.json`。保存成功后输入框改成实际地址。

### 首页按钮

- 顺序和隐藏在 `home/HomeButtons.java`。默认顺序是 Bilibili、幕布、点播、直播、搜索、收藏、推送、设置。偏好键是 `mx_home_order`、`mx_home_hidden`。没有直播源时直播不显示，但列表里仍可保留这一项。
- 电视首页绘制在 leanback `HomeActivity.setFunc()`。焦点落在第一个仍显示的按钮上。手机版没有这组按钮。
- 网页在设置页里做子页，入口是「首页按钮」，不要做回设置页的一行独立大标题。勾选表示显示，上移、下移改顺序。接口是 `Action` 的 `do=homeButtons`，电视收到 `ServerEvent.HOME_BUTTONS` 后刷新首页。

### 电视端 B 站

- 只在电视版。入口是 leanback `BiliActivity` 和 `bili/` 包。手机版不要加这一页。
- 网页「Bilibili 过滤」仍是单独一页（底栏三角里的 Bilibili，数字地址 `6`）。这一页和幕布主页、在线字幕页都不要再放页面级「返回」。幕布内部从「已推送」「高级设置」回到推送的返回要留着。
- 输入页搜索不要做「应用 / Bilibili」手动开关。网页一律 POST `do=search`，只带关键词。`Action.onSearch` 看 `App.activity()`：当前页类名是 `BiliActivity` 就发 `ServerEvent.bili`，否则发原来的搜索。`App.activity()` 在 `onResume` 写入、`onPause` 清掉。
- B 站还没打开时，leanback `HomeActivity` 收到 `BILI` 用 `BiliActivity.start(context, word)` 打开并搜索。已经打开时由 `BiliActivity` 自己处理，不要再 start 一次。未登录提示扫码；页面还没进完时先记下词，进入后再搜。
- 最近搜索只存关键词，存在浏览器里。再点一条时，按当时电视正在看的页面重发，不记录上次发给了谁。

### 幕布、全局弹幕、投屏

- 幕布：leanback `CurtainActivity`，网页处理在 `server/process/Curtain.java`，状态在 `curtain/CurtainStore.java`。默认时钟日历在 `curtain/CurtainClock.java`，没有背景图、背景视频和常驻文字时显示。`restore` 和 `clearCache` 都回到这个方案；`clearCache` 还会删掉音乐。只有幕布在前台时手机才能改；不在前台时接口返回「请先在电视上打开幕布」。从首页打开时，菜单键只开关中间的二维码。屏保打开的是同一页，但带 `saver`，任意键退出，见上面的应用内屏保。有背景视频时默认听视频原声。网页音量只改幕布播放器。推送不保存，背景、音乐、常驻文字要保存。
- 全局弹幕：`ui/custom/GlobalDanmaku.java`。`App` 在页面 `onResume` 挂上、`onPause` 卸下。网页「发送弹幕」走原来的 `do=danmaku`。这和点播页「套用」一整份弹幕文件不是一回事，也不要写进视频自己的弹幕。
- 投屏：`cast/CastReceiver.java`、`setting/CastSetting.java`、leanback `CastDeviceDialog`、`DLNARendererService`。默认开启，开机仍开着就自己注册。状态看 `DLNARendererService.isRegistered()`。手机版没有投屏接收。

### 检查更新

- 仓库写死在 `utils/Github.java` 的 `lhp2048/MXBox`，不要指回 `FongMi/Release`。`Updater` 用 `版本名` 和 `leanback-arm64_v8a.apk` 或 `mobile-arm64_v8a.apk` 挑选正式版。标签必须是三位数字的 `vX.Y.Z`，跳过 draft 和 prerelease。
- 电视首页 Logo 左侧的更新图标和百分比在 leanback `activity_home.xml`、`ic_home_update.xml`、`UpdateDialog`。取消只关对话框，图标留着。下载中再点图标只提示正在下载。校验完成后如果对话框还在，走原来的安装；人已经离开的话，再点图标要先看到对话框，点「安装」才装。

### 网页

网页只打进电视包。`Nano.serve` 在手机上除了 `/proxy`、`/parse`、`/cache`、`/image/`、`/file`、`/tvbus` 都返回 404。手机设置里的「连接」是去打开另一台设备的网页。

资源在 `app/src/main/assets/`：`index.html`、`css/ui.css`、`js/script.js`、`js/navigation.js`、`js/i18n.js`、`js/input.js`，以及 `remote.html`、`logs.html`、`spider.html`、`mxbox_logo.png`。`parse.html` 没有改标题和图标。

底栏和地址：

- 左边固定「输入」（面板 1，里面是搜索、推送两个标签），右边固定「设置」（面板 4）。中间默认「播放」（面板 2）。三角向上展开：播放、字幕、本地、Bilibili、幕布。选中后替换中间按钮。不要把「字幕搜索」再加回这个菜单。
- 数字地址不要改号：`1` 搜索、`2` 推送、`3` 弹幕、`4` 设置、`5` 本地、`6` Bilibili、`7` 幕布、`8` 在线字幕。对应关系在 `navigation.js` 的 `WEB_TAB_NUMBERS`。`8` 的名字仍是 `assrt`，但面板是 3，并打开「在线字幕」标签。没有单独的面板 8。
- 用数字打开后，地址栏改成名字，例如 `?tab=8` 变成 `?tab=assrt`。标签文字是「MXBox · 当前页」。页眉标题是 MXBox，图标用 `mxbox_logo.png`。

设置页从上到下三块：名称 / 配置 / 发送，源服务器，然后「首页按钮」「应用日志」「爬虫测试」。字幕密钥不在设置页。首页按钮和密钥子页的样式跟应用日志一样：标题加线框返回，内容放在卡片里。

字幕页在面板 3，顶部三个标签：本地字幕、在线字幕、弹幕。本地只留上传文件。在线是射手网搜索，设置图标和搜索在同一行，点开才是密钥。逻辑在 `showMedia`、`showAssrtPage`。打开在线字幕才去拉当前片名。接口仍是 `/assrt`，实现在 `assrt/` 包和 `server/process/Assrt.java`。密钥键名 `mx_assrt_token`。这个网页接口不会去读点播配置里的 `assrt` 字段。

名称 / 配置 / 发送是填电视上已经打开的输入框，不是源服务器。没有对话框打开时发送不做任何事。配置类对话框只填入，不自动确认；UA、字幕接口、弹幕接口、mpv 配置会填入并自动确认。
