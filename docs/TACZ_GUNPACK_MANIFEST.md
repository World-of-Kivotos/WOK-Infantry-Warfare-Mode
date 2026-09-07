# WOK步战 TaCZ 枪械包清单

本清单记录 `WOK步战` 组包基线中的 TaCZ 枪械包。它不属于 `WOK` 本体产品线。

## 安装记录

- 安装日期：2026-08-29
- 用户提供归档：`F:\QQhuancun\tacz.zip`
- 归档大小：372,094,591 字节
- 归档 SHA-256：`097AD9EEF18FB7BF1712BBE8DD89D11699B3F6CE0DCC8BBE86159852F57ABA1D`
- 活动目录：`D:\WOK步战测试\1.20.1-Forge_47.4.22\tacz`
- 安装前备份：`D:\WOK步战测试\1.20.1-Forge_47.4.22\tacz.pre-gunpacks-20260829-002244`
- 安装方式：保留原有 `tacz_default_gun`、`.export-state.json` 与 `tacz-pre.toml`，只新增 11 个枪械包 ZIP；原有默认包及配置与归档内副本完全一致，未覆盖。

## 组包组成项

| 文件 | 大小（字节） | SHA-256 |
| --- | ---: | --- |
| `[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip` | 57,346,569 | `BEF5386B4253D7052323760FBB87BAA73B23E065C55EF3DE2A422833F2BB36F6` |
| `[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip` | 98,409,245 | `4A3CCA9DA2E680737AC66BB0869F54FC73C1963317EEEAF06FCEDBEA26C9EB22` |
| `ARIPS_ver.1.3.0.zip` | 3,180,090 | `39C878146B6A31CC3136C37CE7E936D96B20EC1B84AEFF31AF8A7306191C1E60` |
| `carl_gustaf_m4_converted_fixed.zip` | 462,925 | `00D7777B11C59871E71D312679FA167504D7694537DAE20BEFCD87D9CBC25117` |
| `Complete Silence 0.0.2b hotfix.zip` | 15,302,307 | `50A6E8E4AC26785BF9C3CD8BC283C0E6ABFEAD4DA50180D901464F7E50B16956` |
| `m72_law_converted.zip` | 76,680 | `A6AC748E6257C7E6881681487962D9E81EB23CC339C048A5AF5C971E9B94C67A` |
| `Ra1k_gunpack_v2.0.4_hotfix.zip` | 8,352,182 | `7011EB345D2ECE2B5BC6ED54B42708CF143B5EC121933BAB69F6383C69D85166` |
| `RFP v1.1.0 alpha 5.zip` | 28,559,003 | `27416ADEB51A614E91661FF0570E22DD0D249DC97B7916F20F8A55566A1BC71A` |
| `Suffuse-GunSmoke-Pack1.0.8-hotfix.zip` | 47,634,152 | `16D5AC1F5FEAA1EE266463C3834E3B2CB598C3A674250B77C9E41E9D42A317C3` |
| `TaCZ_-Expanded-Arsenal-v3.0 (1).zip` | 57,593,400 | `E4725721FDFE5D57C14AF0A93458F702BACD87CCFC361C46623319E1976C02EF` |
| `Tacz1.1.8-Only] Tacz_Plus_v1.6.zip` | 4,059,127 | `DC639E63BB8A09EACC8C49751DDB7D51AC047685C64DAB3F9E9842FF549D3A3D` |

## 已完成的静态检查

- 11 个目标 ZIP 均可读取，共包含 16,615 个归档条目。
- 各包均未发现绝对路径、`..` 路径穿越条目，或 `exe`、`dll`、`jar`、`bat`、`cmd`、`ps1`、`vbs`、`com`、`msi`、`scr` 文件。
- 外层与嵌套枪包中的 Lua 已检查常见进程执行、动态库加载、文件打开和网络模块调用，未发现匹配项。
- 安装后逐包哈希与用户提供归档内的来源文件一致。

静态检查不等于内容完全可信，也不能证明 TaCZ、Arcana、枪械 ID、动画、音效、配件或多人联机兼容。正式纳入可分发整合包前，还必须逐包核验作者许可、转载范围、署名要求与上游下载页面。

## 游戏内验收要求

1. 启动专用客户端并检查 `logs/latest.log`，确认 TaCZ 与 Arcana 没有枪包解析错误、重复 ID 或资源覆盖异常。
2. 使用 TaCZ 重载功能后逐包抽查枪械、弹药、瞄具、动画、音效、投射物和第三人称显示。
3. 分别验证单人和专用服务器；尤其检查 RPG、无后坐力炮等爆炸物与 Superb Warfare 0.8.9 的伤害联动。
4. 再以实际枪械伤害为输入，平衡 `WOK步战附属-部位血量`、`WOK步战附属-独立护甲`、创伤治疗与载具 T1/T2/T3 数值。

## 回滚

完全退出 Minecraft 与启动器后，先保留当前 `tacz` 目录，再将 `tacz.pre-gunpacks-20260829-002244` 恢复为 `tacz`。不要在客户端运行时替换枪包。
