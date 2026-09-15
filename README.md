# mibluetoothunlocker
mibluetoothunlocker 非小米手环蓝牙解锁小米手机的xposed插件

代码写的比较烂，各位大佬请轻喷

本软件是xposed插件，可以使非小米/红米手环 在小米手机上使用蓝牙解锁功能

本软件hook了系统设置和系统界面，可能会降低系统安全性，请您酌情使用

本软件为本人为了方便手环解锁开发，如有侵权，请联系删帖处理。

1.0版本有bug，只要启动了蓝牙就可以解锁，正在解决此问题

2.0版本加入解锁阈值设置，支持自定义蓝牙rssi信号强度

3.0版本修复了可能会卡死系统界面的问题

3.1版本修复了一些已知问题，提升稳定性，减少了日志的输出

3.3版本，修复偶尔无法解锁的问题，优化了解锁速度

3.4版本，更新隐私协议

3.5版本添加了选择设备和手动输入mac地址的功能，lsp模块中加入推荐勾选

3.6版本搜索设备时可以选择已绑定过的设备，优化蓝牙获取rssi的稳定性

3.7版本 配置文件保存读取不再需要root权限，添加关闭蓝牙解锁提示的功能

4.0版本 加入hyperos兼容模式（hyperos兼容模式下不可关闭解锁提示），修复了一些已知问题

4.1版本 兼容hyperos2.0，兼容hyperos关闭解锁提示，添加重启提示

4.2版本 修复了一个启用解锁提示可能会卡在解锁界面的bug

5.1.1版本 修复选择设备页面有名称为Unknown的设备时发生空指针闪退的问题

5.1.2版本 已配对设备信号强度和距离展示修复未生效，相关代码已还原

5.1.3版本 设备选择页长按设备可直接在系统中取消蓝牙配对

## 5.3：设备管理与界面优化

- 主页设备卡片进入「管理解锁设备」，仅显示已配置设备，刷新和设备状态变化后也保持该范围；未配置设备请从右上角「添加配对设备」进入。
- 添加页面默认显示全部已知/搜索到的设备，可组合「隐藏无名称」「仅常用」筛选。已配对和已配置设备默认为常用，也可逐个设为常用或不常用；筛选不改变配对或解锁配置。
- 名称使用本机备注、系统别名、广播名称、系统名称及有效历史名称；监听名称更新，避免空名称覆盖已有名称。设备不提供名称时显示「未命名设备」，可自行备注，不会根据地址猜测型号。
- 设备操作和解锁方式使用带说明的卡片选项，标出当前解锁方式；移除与取消配对单独确认。帮助、手动添加等弹窗统一为圆角 Material 样式。
- 主页改为靠上排列的设备和设置卡片，输入项简化为「解锁阈值」，保存按钮与提示开关统一对齐，小屏幕可滚动。
- 版本名 `5.3`，版本号 `58`。Release 使用现有 keystore 的 `zx` key 签名，与已发布 v5.0 证书一致；密码不写入构建配置或 Git。

## 5.2.1：被动信号观察与快速复核

距离模式自动使用“机会式被动观察 → 优先候选 → 两次连接 RSSI 复核”。没有新增开关，也不会把广播强度直接作为解锁依据。连接模式仍使用用户明确选择的“已配对且已连接”条件，不判断距离。

- **被动观察**：使用 Android 6+ 的 `SCAN_MODE_OPPORTUNISTIC`，仅复用系统/其他应用产生的 BLE 扫描结果，本模块不主动启动无线扫描。仅在亮屏且锁定期间订阅配置中的距离模式设备，熄屏、解锁或关闭蓝牙时停止；扫描注册在后台线程执行。
- **新鲜样本**：采用 `ScanResult.getTimestampNanos()` 的硬件观察时间。结果超过 500 ms、时间戳无效/重复/乱序或 RSSI 无效时不作为新样本。每台最多保留五条；至少三条、跨度至少 100 ms、窗口不超过 1.5 s 才计算中位数排序。配置变化和断连使旧数据失效。
- **省连接**：先连接排序后的第一台，每隔 350 ms 才增加一个并发名额，上限仍为三台。先完成确认时，排队中的设备无需建连；无广告样本的设备仍保留在候选列表，不会因不广播而被排除。
- **复核**：同一 GATT 连接两次间隔 150 ms 的有效 RSSI 都达到原有阈值才通过。每次读取从发起到处理结果不能超过 750 ms；迟到回调、连接失败或取消后的结果不能通过。单次尖峰不能直接通过，广告与连接 RSSI 不共用阈值。
- **回退**：没有其他应用扫描、目标不发广告、权限不足或 ROM 不允许机会式扫描时，立即使用连接检测，不额外等待广播。不会自动升级为持续主动扫描。机会式扫描注册有最小六秒间隔和失败后的三十秒冷却。

Android 29–34 没有公共 API 可直接借用其他应用的 GATT 客户端来订阅已连接设备 RSSI。API 36.1 的 `SCAN_TYPE_PASSIVE` 只规定不发扫描请求的空口扫描类型，与“不自行启动扫描”的机会式模式不是同一概念。本实现保持 Android 10+ 兼容。

**准确性边界**：纯被动监听无法保证持续有样本；RSSI 受发射功率、遮挡和多径影响，不能保证真实距离零误判，也不是抗地址伪造/中继的认证协议。本次优化保证广播只影响调度、过期数据不作解锁证据，并减少单次尖峰造成的误触发。实际续航收益与时延需要目标手机/手环测量，未用算法测试代替实机功耗数据。

官方依据：[机会式扫描](https://developer.android.com/reference/android/bluetooth/le/ScanSettings#SCAN_MODE_OPPORTUNISTIC)、[扫描结果时间戳](https://developer.android.com/reference/android/bluetooth/le/ScanResult#getTimestampNanos())、[连接 RSSI 读取](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#readRemoteRssi())、[API 36.1 被动扫描类型](https://developer.android.com/reference/android/bluetooth/le/ScanSettings#SCAN_TYPE_PASSIVE)。

## 5.2.0：多设备与连接稳定性

- 可以保存多台解锁设备，每台独立选择模式，任一设备满足条件即可解锁。旧版单 MAC 配置自动兼容。
- **距离模式**：适用于支持 BLE GATT 的手环/手表。建立连接后读取有效 RSSI，达到全局信号阈值才提交给系统解锁。
- **连接模式**：适用于眼镜、耳机、车载音响等。设备必须已完成系统配对并实际连接，不检查 RSSI 或距离。只有配对记录不满足解锁条件。
- 在“管理/配对设备”中点击设备，可配对、添加/修改模式、移除或取消配对。多台设备可逐个配对；配对确认由系统完成。音频设备的实际连接由系统蓝牙设置或厂商应用管理。
- “手动添加设备”会追加一台距离模式设备，不覆盖已有设备；随后可在管理页面修改模式。
- 移除最后一台设备会停止自定义解锁。“基础模式”是独立操作，使用系统设置中选择的设备。
- 搜索按钮启动一次有时限的经典蓝牙/BLE 搜索，离开页面即停止；主页仅在点击刷新时测量，不会因打开页面反复建连。
- RSSI 未测得时显示未知；不再使用配对记录的占位值计算距离。信号阈值范围为 -127 到 -1 dBm。

### 对应 issues

- [#2 小米互传异常](https://github.com/zixing131/mibluetoothunlocker/issues/2)：修复 GATT 未释放、断开后复用旧客户端以及主页重复建连；检测结束、失败、超时、熄屏或解锁后释放本模块客户端。移除旧模块的虚假原生设备注册，避免原生和模块同时对自定义设备建立连接。提供的日志显示互传连接成功后读取状态超时，尚不能证明唯一根因；需要两端设备复测连续互传。
- [#3 多设备](https://github.com/zixing131/mibluetoothunlocker/issues/3)：独立设备配置、最多三路并行检测，第一台离线不会阻塞其余设备；任一符合条件即可解锁。
- [#4 小米眼镜](https://github.com/zixing131/mibluetoothunlocker/issues/4)：提供明确可选的已配对且已连接模式，避免把不支持 GATT 的音频设备当作 BLE 手环。尚未拿到小米眼镜实测，型号/固件兼容性仍需确认。
- [#7 OPPO Watch X2 RSSI 失败](https://github.com/zixing131/mibluetoothunlocker/issues/7)：等待连接成功回调后读取信号；失败后延迟 500 ms 重试一次，每次尝试最长 3.5 秒，整轮检测最长 12 秒。检查 GATT 状态和 RSSI，有效期已结束的回调不会解锁。Watch X2 / HyperOS 3、4 尚需实测。

已关闭的 #1（设备类别）、#5（跨品牌闹钟同步）和 #6（LSPosed 更新）不作为此次新增需求；跨品牌闹钟同步未实现。本项目继续使用 libxposed API / service 102。

### 安装与验证

在 LSPosed 中启用模块，作用域选 `com.android.settings` 和 `com.android.systemui`，初次启用或更新后重启手机。需要解锁时唤出系统密码界面，模块会重新检测设备；系统的强认证、SIM 锁等检查继续生效。Android 12+ 搜索需要附近设备权限，距离搜索还需要位置权限；Android 10/11 使用位置权限。

本次核验当前 Android 14 MIUI 的 `MiuiBleUnlockHelper` smali：监听器无条件构造，`onUnlocked(2)` 经系统强认证检查进入 `tryUnlockByBle()`，该方法要求密码界面可见。不同 HyperOS 版本的内部类/字段仍需要设备验证。

```sh
cd src
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
# 在允许测试程序启动页面的 Android 设备/模拟器上运行：
./gradlew :app:connectedDebugAndroidTest
```

验证结果：5.3 Debug / Release 构建成功；40 项 JVM 测试通过；Lint 0 errors（仍有 warnings）。Android 14 实机上配置存储测试和应用上下文测试通过，界面测试被 MIUI 以 `MIUI Permission Denied Activity` 拒绝启动而超时，尚未通过。以上不等同于互传、眼镜和手表的端到端实测通过，也未完成被动观察优化的实机功耗和时延测量。5.3 实机安装被系统以 `INSTALL_FAILED_USER_RESTRICTED` 拒绝，尚未完成新版界面的实机视觉检查。

后续更新欢迎关注公众号：V安全突击队
![V安全突击队](https://github.com/zixing131/mibluetoothunlocker/assets/18580281/c6185042-b697-4467-aa7c-f800ae2cf273)

3.7版本下载地址：
https://wwjw.lanzoum.com/iXgcM0xpeyxa


3.5版本下载地址
https://wwjw.lanzoue.com/iay3k0j2lk5i
密码:52pj

3.4版本下载地址：
https://wwt.lanzouy.com/i3S4f0a4q8rg
密码:52pj

![image](https://user-images.githubusercontent.com/18580281/208625873-035c01b2-904a-4785-b11e-e52b54c03f54.png)
![image](https://user-images.githubusercontent.com/18580281/208625905-d25b9abb-9233-4d2f-854f-bd9d756089ce.png)
![1](https://user-images.githubusercontent.com/18580281/186316181-828c958c-0982-47ef-9853-2f838e0a061f.jpg)
![2](https://user-images.githubusercontent.com/18580281/186316209-997fafc5-6dc7-4533-acf3-44cad3c4fb04.jpg)
![3](https://user-images.githubusercontent.com/18580281/186316241-bff40689-f424-4abc-a386-e3c32dec7c09.jpg)
![4](https://user-images.githubusercontent.com/18580281/186317501-60a43b60-6a0f-4e70-958e-3e624157be17.jpg)
![5](https://user-images.githubusercontent.com/18580281/186316304-ba3b9873-5a93-4c8b-98d0-e9a6beca58db.jpg)
