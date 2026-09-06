# WOK步战附属-载具部位血量

当前版本 `0.1.0-beta.1`；产物为 `build/libs/wok_vehicle_health-0.1.0-beta.1.jar`。T 级与反坦克平衡仍需真实组合服射击矩阵验收。

`wok_vehicle_health` 是 WOK步战产品线的独立附属 MOD，面向卓越前线（Superb Warfare）及继承其载具基类的附属载具。当前整合包基线包括 SBW 0.8.9 final、龙腾 1.4.1.01 hotfix1、FCP 1.2.1 和 VVP 0.2.x。

## 当前功能

- 履带：直接复用卓越前线模型 OBB 的左右履带命中与血量；任意一侧被判定为损坏后，车辆动力和转向输入会被锁止。
- 引擎：直接复用主/副引擎 OBB 与血量；引擎被判定为损坏后，车辆无法自行移动。
- 观瞄：以炮手座位摄像机的模型变换位置为命中中心，维护独立且随载具保存的观瞄血量；命中时向车内乘员显示短暂雪花干扰。
- 炮塔座圈：非观瞄区域的炮塔 OBB 命中继续消耗卓越前线炮塔血量；受损后炮塔转动速度显著下降，毁坏后进一步下降。
- 炮塔视角同步：炮塔受损后，炮手第一人称/开镜视角跟随服务端确认的炮塔实际朝向，不再先于炮管转向。
- TACZ 爆炸兼容：识别 TACZ 爆炸弹药的自定义爆炸，将实际产生的载具伤害按爆心最近部位计入履带、引擎、炮塔座圈或观瞄。
- 分类 T 级平衡：按坦克、装甲战斗车、轻型战术车和直升机各自的 T1/T2/T3 模板覆盖首批编成载具的车体与部件上限；完整数值见 [`BALANCE_TIERS.md`](BALANCE_TIERS.md)。
- 方向装甲：已登记坦克炮按目标正面、侧面和后部结算；大口径坦克炮对装甲车侧后装甲使用单独压制系数。
- 步兵反坦克归一化：TaCZ Carl Gustaf HEAT、温压弹与 M72 在载具侧分开结算，避免第三方爆炸倍率导致温压弹替代 HEAT；不修改弹药携带和补给上限。
- 兼容兜底：未进入 WOK 平衡表的 SBW 载具仍保留第三方原始车体与统一 50 点部件上限；附属载具若遗漏部件 OBB 标记，则按模型空间位置进行有限兜底判定。

## 依赖与独立性

- 必需：Minecraft 1.20.1、Forge 47、Superb Warfare 0.8.9 final（兼容范围锁定在 0.8.9 至 0.9 之前）。
- 已对照当前整合包验证的 SBW 附属：龙腾 1.4.1.01 hotfix1、FCP 1.2.1、VVP 0.2.x。它们的载具继承 SBW `VehicleEntity`，无需强依赖即可使用同一套部位逻辑。
- 整合包版本、316 份载具数据的 OBB 覆盖统计与平衡基线见 [`COMPATIBILITY_MATRIX.md`](COMPATIBILITY_MATRIX.md)。
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
