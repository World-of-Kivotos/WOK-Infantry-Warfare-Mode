# Third-Party Notices

## DVIDS: F-15E and Bomb Range BROLL

- Source: <https://www.dvidshub.net/video/149528/f-15e-and-bomb-range-broll>
- Creator: Airman 1st Class Samantha Ducker, 4th Fighter Wing
- Published: 2012-07-17
- Source asset: DOD_100445426
- Source status: marked **PUBLIC DOMAIN** by DVIDS
- Used excerpts: 00:33.500–00:36.500 for `jdam_f15_approach.ogg` and
  01:45.800–01:49.300 for `jdam_bomb_tail.ogg`
- Adaptation: excerpts were trimmed, converted to mono Ogg Vorbis, level
  normalized, filtered and faded for positional game playback.
- Reuse: the F-16C Paveway sound events `paveway_f16_approach` and
  `paveway_bomb_tail` play the same two files (`jdam_f15_approach.ogg` and
  `jdam_bomb_tail.ogg`) at pitch 1.1. They are F-15E recordings, not F-16C
  recordings, and no additional audio is bundled for them.

The source recordings and their adaptation are included without any suggestion
of official endorsement.

> The appearance of U.S. Department of War (DoW) visual information does not
> imply or constitute DoW endorsement.

DVIDS copyright guidance: <https://www.dvidshub.net/about/copyright>

## Superb Warfare and Create Big Cannons sound events (referenced, not bundled)

Since 0.1.0-beta.3 the howitzer barrages and the recon drone use six sound
events of this mod that only wrap sound events of other mods by registry id
(`"type": "event"` in `assets/wok_commander_support/sounds.json`):

| Event of this mod | Wrapped event | Provided by |
| --- | --- | --- |
| `howitzer_155_report` | `superbwarfare:plz_05_veryfar` | Superb Warfare (卓越前线) |
| `howitzer_105_report` | `superbwarfare:mk_42_veryfar` | Superb Warfare (卓越前线) |
| `artillery_incoming_cbc` | `createbigcannons:shell_flying` | Create Big Cannons |
| `artillery_incoming_sbw` | `superbwarfare:shell_fly` | Superb Warfare (卓越前线) |
| `recon_drone_engine` | `superbwarfare:ju_87_engine` | Superb Warfare (卓越前线) |
| `recon_drone_destroyed` | `superbwarfare:explosion_air` | Superb Warfare (卓越前线) |

No audio file, texture, model or code of Superb Warfare or Create Big Cannons
is included in this JAR. The sounds are loaded from those mods' own resources at
run time, only when the player has installed them, and remain subject to those
mods' licenses. Without the providing mod the wrapped event is simply silent;
the module still loads. This mod only sets its own subtitles, volume and
broadcast radius around the referenced events.

中文说明：上述 6 个声音事件只按注册 ID 引用卓越前线与 Create Big Cannons 的
声音事件，本 JAR 未打包、未修改任何第三方声音、贴图、模型或代码；未安装对应
MOD 时这些事件静音，模块照常加载。侦察无人机的模型与贴图为本模块自制，
不来自上述 MOD。
