WOK步战核心 0.5.0-beta.4 · 掏平板动画音效（12 个）

自制合成音效，可自由使用：用代码离线合成，不读取、不混入任何游戏或素材库的音频。
生成脚本与种子见 ui-preview/tablet-anim/tools/make-sounds.mjs（WOK步战工作区，不在本仓库；
随机种子 20261010，每个音效再异或名字的哈希，重复运行逐字节相同）。
WAV 为 44.1kHz、单声道、16bit，峰值 -3dBFS；转 OGG：ffmpeg -c:a libvorbis -q:a 4 -ac 1。

响度均衡写在 assets/wok_infantry/sounds.json 每个事件的 volume 里，游戏里再乘 0.25
（与原版界面点击声的播放音量相同），受“玩家”音量滑杆控制，只在本地客户端播放。
事件 wok_infantry:tablet.<名字> 不注册 SoundEvent（见 CHANGELOG 0.5.0-beta.4“兼容性”）。

holster 收枪 / rustle 布料 / draw 抽板 / grip 握稳 / power 电源键 / boot 开机 /
ready 联网就绪 / search 搜索中 / zoom 贴近 / sleep 息屏 / stow 收回 / raise 举枪
