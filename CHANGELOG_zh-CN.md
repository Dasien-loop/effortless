## 更新日志
----------

### 3.4.0 - Dasien 维护移植版 - 2026-08-17

* 使用标准 Gradle 工具移植 Forge 1.20.1、NeoForge 1.21.1 和 NeoForge 26.1.2。
* 按版本增加 AE2、超越维度、精致存储、精妙背包和 ProjectE 的可选材料兼容。
* 修复网络材料读取，包括放在 Curios 饰品槽中的 AE2 和精致存储无线终端。
* 修复所有支持版本的放置、破坏、撤销/重做、预览和轮盘菜单功能。
* 稳定并优化网络预览，加入服务端材料检查和刷新限频。
* 保留上游署名与 LGPLv3 许可证。本维护移植版作者：Dasien。

----------

### 3.4.0

* 添加对 OpenPAC 的支持（[#202](https://github.com/huskcasaca/effortless/pull/202)）。
* 添加对 FTB Chunks 的支持（[#204](https://github.com/huskcasaca/effortless/pull/204)）。
* 优化建造行为（[#203](https://github.com/huskcasaca/effortless/pull/203)）。
* 修复装有液体的桶未掉落的问题（[#205](https://github.com/huskcasaca/effortless/pull/205)）。
* 修复无法替换水的问题（[#206](https://github.com/huskcasaca/effortless/pull/206)）。
* 恢复保留工具耐久度的配置选项（[#174](https://github.com/huskcasaca/effortless/pull/174)）。
* 修复 GlException 导致的无效按键问题（[#207](https://github.com/huskcasaca/effortless/pull/207)）。
* 重构设置页面（[#208](https://github.com/huskcasaca/effortless/pull/208)）。

----------

### 3.3.0

* 修复 Fabric 在 1.21 及更高版本中无法使用网络的问题（[#196](https://github.com/huskcasaca/effortless/pull/196)）。
* 修复 Forge 1.21 及更高版本无法启动的问题（[#197](https://github.com/huskcasaca/effortless/pull/197)）。
* 修复墙上火把放置问题（[#198](https://github.com/huskcasaca/effortless/pull/198)）。
* 移植至 1.21.3（[#199](https://github.com/huskcasaca/effortless/pull/200)）。
* 修复随机物品创建问题（[#201](https://github.com/huskcasaca/effortless/pull/201)）。

----------

### 3.2.0

* 修复方块实体标签设置问题（[#169](https://github.com/huskcasaca/effortless/pull/169)）。
* 修复模组描述过长导致的崩溃（[#170](https://github.com/huskcasaca/effortless/pull/170)）。
* 修复轴向图案和中心圆计算的旋转角度（[#171](https://github.com/huskcasaca/effortless/pull/171)，感谢 @almarzn）。
* 修复 Minecraft 1.21 服务端无法启动的问题（[#190](https://github.com/huskcasaca/effortless/pull/190)）。
* 修复工具使用不当导致的崩溃（[#191](https://github.com/huskcasaca/effortless/pull/191)）。
* 修复 NeoForge 兼容性问题（[#192](https://github.com/huskcasaca/effortless/pull/192)）。
* 移植至 1.21.1（[#193](https://github.com/huskcasaca/effortless/pull/193)）。

----------

### 3.1.0

* 移植至 1.20.6（[#157](https://github.com/huskcasaca/effortless/pull/157)）。
* 移植至 1.21（[#158](https://github.com/huskcasaca/effortless/pull/158)）。
* 移植至 NeoForge 和 Minecraft 1.21（[#159](https://github.com/huskcasaca/effortless/pull/159)）。

----------

### 3.0.0

* 添加被动模式（[#118](https://github.com/huskcasaca/effortless/pull/118)）。
* 修复提示框物品数量问题（[#120](https://github.com/huskcasaca/effortless/pull/120)）。
* 添加方块堆叠数量限制（[#121](https://github.com/huskcasaca/effortless/pull/121)）。
* 修复方块追踪问题（[#123](https://github.com/huskcasaca/effortless/pull/123)）。
* 添加“仅使用正确工具”选项（[#128](https://github.com/huskcasaca/effortless/pull/128)）。
* 添加多种替换模式（[#134](https://github.com/huskcasaca/effortless/pull/134)）。
* 添加剪贴板复制和粘贴功能（[#135](https://github.com/huskcasaca/effortless/pull/135)）。
* 添加剪贴板镜像、旋转和移动快捷键（[#138](https://github.com/huskcasaca/effortless/pull/138)）。
* 修复服务端找不到模组的问题（[#139](https://github.com/huskcasaca/effortless/pull/139)）。
* 添加方块实体渲染器（[#144](https://github.com/huskcasaca/effortless/pull/144)）。
* 支持复制和粘贴容器内容（[#144](https://github.com/huskcasaca/effortless/pull/144)）。
* 修复台阶物品消耗问题（[#145](https://github.com/huskcasaca/effortless/pull/145)）。
* 修复更新建造模式时图案被禁用的问题（[#147](https://github.com/huskcasaca/effortless/pull/147)）。
* 修复在轮盘菜单中选取方块时崩溃的问题（[#151](https://github.com/huskcasaca/effortless/pull/151)）。
* 修复 Forge 桶无法放置非原版流体的问题（[#153](https://github.com/huskcasaca/effortless/pull/153)）。
* 添加剪贴板集合（[#154](https://github.com/huskcasaca/effortless/pull/154)）。
* 添加不可替换物品提示（[#155](https://github.com/huskcasaca/effortless/pull/155)）。

----------

### 3.0.0-beta.2

* 新增剪贴板界面（[#150](https://github.com/huskcasaca/effortless/pull/150)）。
* 修复在轮盘菜单中选取方块时崩溃的问题（[#151](https://github.com/huskcasaca/effortless/pull/151)）。

### 3.0.0-beta.1

* 修复客户端建造提示显示不正确的问题（[#146](https://github.com/huskcasaca/effortless/pull/146)）。
* 修复更新建造模式时图案被禁用的问题（[#147](https://github.com/huskcasaca/effortless/pull/147)）。

### 3.0.0-alpha.4

* 添加方块实体渲染器（[#144](https://github.com/huskcasaca/effortless/pull/144)）。
* 支持复制和粘贴容器内容（[#144](https://github.com/huskcasaca/effortless/pull/144)）。
* 修复台阶物品消耗问题（[#145](https://github.com/huskcasaca/effortless/pull/145)）。

### 3.0.0-alpha.3

* 恢复快速替换模式（[#141](https://github.com/huskcasaca/effortless/pull/141)）。

### 3.0.0-alpha.2

* 添加剪贴板镜像、旋转和移动快捷键（[#138](https://github.com/huskcasaca/effortless/pull/138)）。
* 修复服务端找不到模组的问题（[#139](https://github.com/huskcasaca/effortless/pull/139)）。

### 3.0.0-alpha.1

* 添加被动模式（[#118](https://github.com/huskcasaca/effortless/pull/118)）。
* 调整会话数据包顺序（[#119](https://github.com/huskcasaca/effortless/pull/119)）。
* 修复提示框物品数量问题（[#120](https://github.com/huskcasaca/effortless/pull/120)）。
* 添加方块堆叠数量限制（[#121](https://github.com/huskcasaca/effortless/pull/121)）。
* 修复方块追踪问题（[#123](https://github.com/huskcasaca/effortless/pull/123)）。
* 重做图案界面（[#125](https://github.com/huskcasaca/effortless/pull/125)）。
* 重做建造模式（[#126](https://github.com/huskcasaca/effortless/pull/126)）。
* 重做设置界面（[#127](https://github.com/huskcasaca/effortless/pull/127)）。
* 添加“仅使用正确工具”选项（[#128](https://github.com/huskcasaca/effortless/pull/128)）。
* 重做音效系统（[#129](https://github.com/huskcasaca/effortless/pull/129)）。
* 添加建造时的玩家状态（[#130](https://github.com/huskcasaca/effortless/pull/130)）。
* 重做撤销/重做功能（[#131](https://github.com/huskcasaca/effortless/pull/131)）。
* 重做变换器（[#132](https://github.com/huskcasaca/effortless/pull/132)）。
* 添加多种替换模式（[#134](https://github.com/huskcasaca/effortless/pull/134)）。
* 添加剪贴板复制和粘贴功能（[#135](https://github.com/huskcasaca/effortless/pull/135)）。

----------

### 2.4.1

* 修复统计奖励问题（[#117](https://github.com/huskcasaca/effortless/pull/117)）。

### 2.4.0

* 添加边长统一功能（[#109](https://github.com/huskcasaca/effortless/pull/109)）。
* 添加方块交互功能（[#111](https://github.com/huskcasaca/effortless/pull/111)）。
* 修复 RadialTransformer 崩溃（[#112](https://github.com/huskcasaca/effortless/pull/112)）。
* 添加更多客户端配置选项（[#113](https://github.com/huskcasaca/effortless/pull/113)）。
* 添加更多建造提示（[#114](https://github.com/huskcasaca/effortless/pull/114)）。
* 更名为 Effortless Structure（[#115](https://github.com/huskcasaca/effortless/pull/115)）。
* 添加西班牙语和法语翻译（[#116](https://github.com/huskcasaca/effortless/pull/116)）。

### 2.3.3

* 修复灯笼导致的崩溃（[#102](https://github.com/huskcasaca/effortless/pull/102)）。
* 修复 Modrinth 推送顺序问题（[#103](https://github.com/huskcasaca/effortless/pull/103)）。
* 修复 Forge 崩溃（[#106](https://github.com/huskcasaca/effortless/pull/106)）。
* 修复 Fabric 配置问题（[#107](https://github.com/huskcasaca/effortless/pull/107)）。

### 2.3.2

* 修复服务端超时（[#99](https://github.com/huskcasaca/effortless/pull/99)）。
* 修复 propertyGetter 空指针异常（[#100](https://github.com/huskcasaca/effortless/pull/100)）。

### 2.3.1

* 修复 Quilt 崩溃（[#96](https://github.com/huskcasaca/effortless/pull/96)）。

### 2.3.0

* 添加服务端配置（[#83](https://github.com/huskcasaca/effortless/pull/83)）。
* 修复 Quilt 游戏崩溃（[#85](https://github.com/huskcasaca/effortless/pull/85)）。
* 修复持续导致服务端崩溃的问题（[#86](https://github.com/huskcasaca/effortless/pull/86)）。
* 修复尝试打开建造轮盘时崩溃的问题（[#87](https://github.com/huskcasaca/effortless/pull/87)）。
* 修复圆形和圆柱建造模式中中心起点无效的问题（[#88](https://github.com/huskcasaca/effortless/pull/88)）。
* 修复启动时左 Ctrl 无法使用的问题（[#89](https://github.com/huskcasaca/effortless/pull/89)）。
* 添加图案预设（[#90](https://github.com/huskcasaca/effortless/pull/90)）。
* 修复部分问题（[#93](https://github.com/huskcasaca/effortless/pull/93)）。
* 添加世界边界限制（[#94](https://github.com/huskcasaca/effortless/pull/94)）。
* 添加德语翻译（[#91](https://github.com/huskcasaca/effortless/pull/91)，感谢 @AndiLeni）。
* 修复翻译条目（[#95](https://github.com/huskcasaca/effortless/pull/95)）。

### 2.2.0

* 按下 ESC 时重置建造（[#70](https://github.com/huskcasaca/effortless/pull/70)）。
* 添加仅客户端放置/破坏方块功能（[#71](https://github.com/huskcasaca/effortless/pull/71)）。
* 使用 ServiceLoader（[#73](https://github.com/huskcasaca/effortless/pull/73)）。
* 修复玩家/世界空指针异常（[#74](https://github.com/huskcasaca/effortless/pull/74)）。

### 2.1.0

* 优化 Minecraft 移植层（[#64](https://github.com/huskcasaca/effortless/pull/64)）。
* 修复圆形建造模式追踪（[#65](https://github.com/huskcasaca/effortless/pull/65)）。
* 添加金字塔和圆锥建造模式（[#66](https://github.com/huskcasaca/effortless/pull/66)）。
* 添加 SoundManager（[#67](https://github.com/huskcasaca/effortless/pull/67)）。
* 添加 ServiceLoader（[#68](https://github.com/huskcasaca/effortless/pull/68)）。
* 修复方块声音（[#69](https://github.com/huskcasaca/effortless/pull/69)）。

### 2.0.2

* 移植至其他版本（[#61](https://github.com/huskcasaca/effortless/pull/61)）。
* 修复与 Sodium 的崩溃（[#60](https://github.com/huskcasaca/effortless/pull/60)）。
* 修复启动循环问题（[#62](https://github.com/huskcasaca/effortless/pull/62)）。

### 2.0.1

* 修复按下 Alt 时崩溃的问题（[#54](https://github.com/huskcasaca/effortless/pull/54)）。

### 2.0.0

* 添加 Forge 支持。
* 添加物品随机化器。
* 重写全部内容（[#50](https://github.com/huskcasaca/effortless/pull/50)）。
* 创建 ru.json（[#45](https://github.com/huskcasaca/effortless/pull/45)，感谢 @Telezhka-the-First）。

### 1.6.3

* 修复操作栏文字遮挡问题。
* 修复图像资源路径转换问题。
