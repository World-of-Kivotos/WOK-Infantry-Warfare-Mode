# WOK步战开发数据迁移工具

这套工具用于在两台 Windows 电脑之间迁移当前 WOK步战开发环境。它保留完整 `.git`、未提交改动、美术、文档、表格及工作区内的本地兼容依赖。`Full` 保留所有已跟踪和未跟踪文件；`Source` 只排除可重建的未跟踪构建目录、缓存、日志和临时文件。

## WOK 本体 + WOK步战全产品线导出

下面的入口会同时打包 `D:\Wok`、`D:\WOK测试`、当前 WOK步战工作区与 `D:\WOK步战测试\1.20.1-Forge_47.4.22`：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\migration\Export-AllWokProducts.ps1
```

恢复联合包：

```powershell
powershell -ExecutionPolicy Bypass -File .\Import-WokMigration.ps1 `
  -DestinationRoot "D:\Work\Wok步战附属-创伤治疗" `
  -RestoreTestInstancePath "D:\WOK步战测试\1.20.1-Forge_47.4.22" `
  -RestoreWokMainWorkspacePath "D:\Wok" `
  -RestoreWokMainTestEnvironmentPath "D:\WOK测试"
```

联合包内使用 `products/wok-main/` 与 WOK步战的 `workspace/`/`extras/wok-infantry-test-instance/` 分离存储，导入时也拒绝两条产品线指向同一目录。

## 旧电脑：导出

推荐数据包（排除 Gradle 缓存、构建产物、运行日志，但保留 `outputs/`）：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\migration\Export-WokMigration.ps1
```

指定 U 盘或移动硬盘：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\migration\Export-WokMigration.ps1 -Destination "G:\WOK迁移"
```

连同 WOK步战附属专用 Minecraft 测试实例一起带走：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\migration\Export-WokMigration.ps1 -Destination "G:\WOK迁移" -IncludeTestInstance
```

要将构建产物、本地运行目录也原样保留，使用 `-Profile Full`。用户提供的 `F:\QQhuancun` 外部归档可用 `-IncludeExternalSources` 加入。迁移前的独立护甲副本只供考古，必须显式加 `-IncludeLegacyArmor`才打包。

导出后可在旧电脑或拷贝到移动硬盘后执行：

```powershell
.\Verify-WokMigration.ps1
```

## 新电脑：导入

在迁移包根目录中运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\Import-WokMigration.ps1 -DestinationRoot "D:\Work\Wok步战附属-创伤治疗"
```

如果包内带有测试实例：

```powershell
powershell -ExecutionPolicy Bypass -File .\Import-WokMigration.ps1 `
  -DestinationRoot "D:\Work\Wok步战附属-创伤治疗" `
  -RestoreTestInstancePath "D:\WOK步战测试\1.20.1-Forge_47.4.22"
```

导入程序先验证 SHA-256，再恢复工作区，最后运行 `git status` 和 `git fsck`。它默认拒绝导入到非空目录，避免意外覆盖。

## 迁移边界

- `WOK` 与 `WOK步战` 是两条不同产品线。脚本会拒绝将步战附属测试数据导入 `D:\WOK测试`。
- `wok_infantry_armor/` 是 `WOK步战附属-独立护甲` 的 Git 权威源码；`D:\Wok\standalone\wok-infantry-armor` 不得继续独立开发。
- 账号密码、GitHub 令牌、SSH/GPG 私钥、浏览器会话和 Codex 登录态不进入数据包。请在新电脑单独登录并重新配置凭据。
- 用户自有的第三方 MOD/JAR 可供两台个人设备之间迁移，不要将该数据包公开发布。
- Codex 任务列表通常跟账号/主机绑定，本工具保留工作区上下文和 `AGENTS.md`，不复制应用的私有登录数据库。

## 新电脑建议环境

- Windows 10/11、PowerShell 5.1+
- Git for Windows，并登录原 GitHub 账号
- JDK 17
- Codex 桌面端，使用原账号登录
- 第一次构建时允许 Gradle 重新下载可重建缓存

如果新电脑的盘符不同，请检查 `wok_vehicle_health/gradle.properties` 内的 `superbwarfare_dev_jar_path`，以及文档中记录的测试路径。
