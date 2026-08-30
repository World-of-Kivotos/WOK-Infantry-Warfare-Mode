# WOK步战附属-载具部位血量

`wok_vehicle_health` 是 WOK步战产品线的独立附属 MOD，面向卓越前线（Superb Warfare）及继承其载具基类的附属载具。

## 当前功能

- 履带：直接复用卓越前线模型 OBB 的左右履带命中与血量；任意一侧被判定为损坏后，车辆动力和转向输入会被锁止。
- 引擎：直接复用主/副引擎 OBB 与血量；引擎被判定为损坏后，车辆无法自行移动。
- 观瞄：以炮手座位摄像机的模型变换位置为命中中心，维护独立且随载具保存的观瞄血量；命中时向车内乘员显示短暂雪花干扰。
- 炮塔座圈：非观瞄区域的炮塔 OBB 命中继续消耗卓越前线炮塔血量；受损后炮塔转动速度显著下降，毁坏后进一步下降。
- 炮塔视角同步：炮塔受损后，炮手第一人称/开镜视角跟随服务端确认的炮塔实际朝向，不再先于炮管转向。
- TACZ 爆炸兼容：识别 TACZ 爆炸弹药的自定义爆炸，将实际产生的载具伤害按爆心最近部位计入履带、引擎、炮塔座圈或观瞄。
- 兼容兜底：卓越前线附属若继承 `VehicleEntity` 但遗漏部件最大血量，会在其载具数据加载时补齐合理默认值；若遗漏部件 OBB 标记，则按模型空间位置进行有限兜底判定。

## 依赖与独立性

- 必需：Minecraft 1.20.1、Forge 47、Superb Warfare 0.8.9.1（兼容范围锁定在 0.8.9.1 至 0.9 之前）。
- TACZ 为可选依赖；未安装 TACZ 时不加载其兼容路径，载具部位系统仍可独立运行。
- 不依赖 `wok_infantry`、`wok_body_health`、`wok_trauma` 或 `wok_infantry_armor`，可以作为单独的 WOK步战附属安装。
- 构建时通过 `superbwarfare_dev_jar_path` 指向本地卓越前线 JAR；该第三方 JAR 不会进入本仓库或构建产物。

## 构建

从仓库根目录执行：

```powershell
Push-Location wok_vehicle_health
..\gradlew.bat clean test build
Pop-Location
```

构建产物位于 `wok_vehicle_health/build/libs/`。
