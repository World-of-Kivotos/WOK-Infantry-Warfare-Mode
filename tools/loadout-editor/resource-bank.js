// Generated from the installed WOK infantry test resources. No game files are changed.
window.WOK_RESOURCE_BANK = {
  "source": "WOK步战专用测试环境 1.20.1-Forge_47.4.22 · 2026-09-06",
  "sourceFiles": {
    "loadouts": "config/wok_infantry/loadouts.json",
    "loadoutsSha256": "2544b07873e1a544405df1be3a564109464bab898889d0fd4c1661c4d2911f05",
    "formations": "config/wok_infantry/formations.json",
    "gunpacks": [
      "tacz/tacz_default_gun",
      "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "tacz/ARIPS_ver.1.3.0.zip",
      "tacz/carl_gustaf_m4_converted_fixed.zip",
      "tacz/Complete Silence 0.0.2b hotfix.zip",
      "tacz/m72_law_converted.zip",
      "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "tacz/RFP v1.1.0 alpha 5.zip",
      "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
    ],
    "lenientJsonRecovered": [
      "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip!data/ccrp/data/guns/sr_3m_data.json",
      "tacz/ARIPS_ver.1.3.0.zip!assets/apdf/lang/zh_cn.json",
      "tacz/ARIPS_ver.1.3.0.zip!assets/asos/lang/zh_cn.json",
      "tacz/ARIPS_ver.1.3.0.zip!assets/atea/lang/en_us.json",
      "tacz/ARIPS_ver.1.3.0.zip!assets/atea/lang/zh_cn.json",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip!data/tacz/data/guns/vss_data.json",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip!data/tacz/data/guns/9a91_data.json",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip!data/tacz/index/guns/akm.json",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip!data/tacz/data/guns/sr3m_data.json",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip!data/tacz/data/guns/asval_data.json",
      "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip!data/tacz/data/guns/vsk_data.json"
    ]
  },
  "notes": [
    "条目为当前配装原始字段，完全相同的条目合并并保留全部来源。",
    "枪械和配件来自已安装枪包索引；multipleSources 表示同一 ID 出现在多个包中，扫描结果不推断运行时覆盖顺序。",
    "载具来自已安装模组载具数据和当前编制，条目存在不代表任意场地都可生成。",
    "资源库是离线快照；使用者安装的 MOD 与枪包须对应，导入时应核对注册表。"
  ],
  "entries": [
    {
      "label": "伯莱塔 M9A4 手枪",
      "sourceClass": "突击兵",
      "sourceSlot": "副武器",
      "sourceClassId": "assault",
      "sourceSlotId": "secondary",
      "sources": [
        {
          "classId": "assault",
          "className": "突击兵",
          "slotId": "secondary",
          "slotName": "副武器"
        }
      ],
      "entry": {
        "id": "m9a4",
        "displayName": "伯莱塔 M9A4 手枪",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttributeModifiers:[{Amount:3.0d,AttributeName:\"gunsmithlib:horz_recoil\",Operation:0,Slot:\"mainhand\",UUID:[I;16706736,-1641789173,-1973740856,78912542]},{Amount:3.0d,AttributeName:\"gunsmithlib:vert_recoil\",Operation:0,Slot:\"mainhand\",UUID:[I;-1234703291,1044726507,-2118587306,-1729417262]}],Extras:{Gun_UUID:\"e9e32a57-6bf6-4e05-ba46-c240101cf7fa\"},GunCurrentAmmoCount:17,GunFireMode:\"SEMI\",GunId:\"tacz:m9a4\",HasBulletInBarrel:1b}",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试铁斧",
      "sourceClass": "突击兵",
      "sourceSlot": "近战武器",
      "sourceClassId": "assault",
      "sourceSlotId": "melee",
      "sources": [
        {
          "classId": "assault",
          "className": "突击兵",
          "slotId": "melee",
          "slotName": "近战武器"
        },
        {
          "classId": "support",
          "className": "支援兵",
          "slotId": "melee",
          "slotName": "近战武器"
        },
        {
          "classId": "engineer",
          "className": "工程兵",
          "slotId": "melee",
          "slotName": "近战武器"
        },
        {
          "classId": "recon",
          "className": "侦察兵",
          "slotId": "melee",
          "slotName": "近战武器"
        }
      ],
      "entry": {
        "id": "test_iron_axe",
        "displayName": "测试铁斧",
        "itemId": "minecraft:iron_axe",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "小型弹药箱",
      "sourceClass": "突击兵",
      "sourceSlot": "战术道具二",
      "sourceClassId": "assault",
      "sourceSlotId": "gadget_two",
      "sources": [
        {
          "classId": "assault",
          "className": "突击兵",
          "slotId": "gadget_two",
          "slotName": "战术道具二"
        }
      ],
      "entry": {
        "id": "portable_ammo_crate",
        "displayName": "小型弹药箱",
        "itemId": "wok_infantry:ammo_supply_crate",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试弓",
      "sourceClass": "支援兵",
      "sourceSlot": "主武器",
      "sourceClassId": "support",
      "sourceSlotId": "primary",
      "sources": [
        {
          "classId": "support",
          "className": "支援兵",
          "slotId": "primary",
          "slotName": "主武器"
        },
        {
          "classId": "recon",
          "className": "侦察兵",
          "slotId": "primary",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "test_bow",
        "displayName": "测试弓",
        "itemId": "minecraft:bow",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试石剑",
      "sourceClass": "支援兵",
      "sourceSlot": "副武器",
      "sourceClassId": "support",
      "sourceSlotId": "secondary",
      "sources": [
        {
          "classId": "support",
          "className": "支援兵",
          "slotId": "secondary",
          "slotName": "副武器"
        },
        {
          "classId": "recon",
          "className": "侦察兵",
          "slotId": "secondary",
          "slotName": "副武器"
        }
      ],
      "entry": {
        "id": "test_stone_sword",
        "displayName": "测试石剑",
        "itemId": "minecraft:stone_sword",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试盾牌",
      "sourceClass": "支援兵",
      "sourceSlot": "战术道具一",
      "sourceClassId": "support",
      "sourceSlotId": "gadget_one",
      "sources": [
        {
          "classId": "support",
          "className": "支援兵",
          "slotId": "gadget_one",
          "slotName": "战术道具一"
        }
      ],
      "entry": {
        "id": "test_shield",
        "displayName": "测试盾牌",
        "itemId": "minecraft:shield",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试箭矢",
      "sourceClass": "支援兵",
      "sourceSlot": "战术道具二",
      "sourceClassId": "support",
      "sourceSlotId": "gadget_two",
      "sources": [
        {
          "classId": "support",
          "className": "支援兵",
          "slotId": "gadget_two",
          "slotName": "战术道具二"
        },
        {
          "classId": "engineer",
          "className": "工程兵",
          "slotId": "gadget_two",
          "slotName": "战术道具二"
        },
        {
          "classId": "recon",
          "className": "侦察兵",
          "slotId": "gadget_two",
          "slotName": "战术道具二"
        }
      ],
      "entry": {
        "id": "test_arrows",
        "displayName": "测试箭矢",
        "itemId": "minecraft:arrow",
        "count": 64,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试投掷物",
      "sourceClass": "支援兵",
      "sourceSlot": "投掷物",
      "sourceClassId": "support",
      "sourceSlotId": "throwable",
      "sources": [
        {
          "classId": "support",
          "className": "支援兵",
          "slotId": "throwable",
          "slotName": "投掷物"
        },
        {
          "classId": "engineer",
          "className": "工程兵",
          "slotId": "throwable",
          "slotName": "投掷物"
        },
        {
          "classId": "recon",
          "className": "侦察兵",
          "slotId": "throwable",
          "slotName": "投掷物"
        }
      ],
      "entry": {
        "id": "test_snowballs",
        "displayName": "测试投掷物",
        "itemId": "minecraft:snowball",
        "count": 16,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试铁镐",
      "sourceClass": "工程兵",
      "sourceSlot": "主武器",
      "sourceClassId": "engineer",
      "sourceSlotId": "primary",
      "sources": [
        {
          "classId": "engineer",
          "className": "工程兵",
          "slotId": "primary",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "test_iron_pickaxe",
        "displayName": "测试铁镐",
        "itemId": "minecraft:iron_pickaxe",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试弩",
      "sourceClass": "工程兵",
      "sourceSlot": "副武器",
      "sourceClassId": "engineer",
      "sourceSlotId": "secondary",
      "sources": [
        {
          "classId": "engineer",
          "className": "工程兵",
          "slotId": "secondary",
          "slotName": "副武器"
        }
      ],
      "entry": {
        "id": "test_crossbow",
        "displayName": "测试弩",
        "itemId": "minecraft:crossbow",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试打火石",
      "sourceClass": "工程兵",
      "sourceSlot": "战术道具一",
      "sourceClassId": "engineer",
      "sourceSlotId": "gadget_one",
      "sources": [
        {
          "classId": "engineer",
          "className": "工程兵",
          "slotId": "gadget_one",
          "slotName": "战术道具一"
        }
      ],
      "entry": {
        "id": "test_flint",
        "displayName": "测试打火石",
        "itemId": "minecraft:flint_and_steel",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "测试望远镜",
      "sourceClass": "侦察兵",
      "sourceSlot": "战术道具一",
      "sourceClassId": "recon",
      "sourceSlotId": "gadget_one",
      "sources": [
        {
          "classId": "recon",
          "className": "侦察兵",
          "slotId": "gadget_one",
          "slotName": "战术道具一"
        }
      ],
      "entry": {
        "id": "test_spyglass",
        "displayName": "测试望远镜",
        "itemId": "minecraft:spyglass",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M4A1 卡宾枪 垂直握把+M4红点瞄具",
      "sourceClass": "队长",
      "sourceSlot": "主武器",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_f1b9f2e1f8f4",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "m4a1",
        "displayName": "M4A1 卡宾枪 垂直握把+M4红点瞄具",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentGRIP:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:grip_vertical_military\"}},AttachmentLASER:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:ls321\",LaserColor:-65536}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"suffuse:scope_compm4\"}},AttachmentSTOCK:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:stock_militech_b5\"}},Extras:{Gun_UUID:\"15e5cab9-5950-43e3-aaca-c999a58e05ab\"},GunCurrentAmmoCount:30,GunFireMode:\"AUTO\",GunId:\"tacz:m4a1\",HasBulletInBarrel:1b,wok_infantry_weapon_editor_id:[I;-313891080,1558987820,-1855586294,-1636629728],wok_infantry_ads_speed_scale:0.60f,wok_infantry_vertical_recoil_scale:3.75f,wok_infantry_horizontal_recoil_scale:3.00f}",
        "ammoReserveLimit": 150
      }
    },
    {
      "label": "M4A1 卡宾枪 ACOG",
      "sourceClass": "队长",
      "sourceSlot": "主武器",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_f1b9f2e1f8f4",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "m4a1_2",
        "displayName": "M4A1 卡宾枪 ACOG",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentGRIP:{Count:0b,id:\"minecraft:air\"},AttachmentLASER:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:ls321\",LaserColor:-65536}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"suffuse:scope_acogta01\"}},AttachmentSTOCK:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:stock_militech_b5\"}},Extras:{Gun_UUID:\"15e5cab9-5950-43e3-aaca-c999a58e05ab\"},GunCurrentAmmoCount:30,GunFireMode:\"AUTO\",GunId:\"tacz:m4a1\",HasBulletInBarrel:1b,wok_infantry_weapon_editor_id:[I;-313891080,1558987820,-1855586294,-1636629728],wok_infantry_ads_speed_scale:0.45f,wok_infantry_vertical_recoil_scale:4.25f,wok_infantry_horizontal_recoil_scale:3.50f}",
        "ammoReserveLimit": 150
      }
    },
    {
      "label": "小队队包无线电",
      "sourceClass": "队长",
      "sourceSlot": "道具1",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_2f4899e7585f",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_2f4899e7585f",
          "slotName": "道具1"
        }
      ],
      "entry": {
        "id": "rally_radio",
        "displayName": "小队队包无线电",
        "itemId": "wok_infantry:rally_radio",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M67手榴弹",
      "sourceClass": "队长",
      "sourceSlot": "道具2",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_b9371df3382c",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_b9371df3382c",
          "slotName": "道具2"
        },
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_2f4899e7585f",
          "slotName": "道具1"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_2f4899e7585f",
          "slotName": "道具1"
        }
      ],
      "entry": {
        "id": "hand_grenade",
        "displayName": "M67手榴弹",
        "itemId": "superbwarfare:hand_grenade",
        "count": 3,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "AFAK单兵战术急救包",
      "sourceClass": "队长",
      "sourceSlot": "医疗物品1",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_44b5e70b6661",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_44b5e70b6661",
          "slotName": "医疗物品1"
        },
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_44b5e70b6661",
          "slotName": "医疗物品1"
        },
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_864a8307ac23",
          "slotName": "医疗物品 1"
        },
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_864a8307ac23",
          "slotName": "医疗物品 1"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_44b5e70b6661",
          "slotName": "医疗物品1"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_44b5e70b6661",
          "slotName": "医疗物品1"
        }
      ],
      "entry": {
        "id": "afak",
        "displayName": "AFAK单兵战术急救包",
        "itemId": "wok_trauma:afak",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "CALOK-B止血剂",
      "sourceClass": "队长",
      "sourceSlot": "医疗物品2",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_39dc75729ade",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_39dc75729ade",
          "slotName": "医疗物品2"
        },
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_39dc75729ade",
          "slotName": "医疗物品2"
        },
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_0ffaded08c78",
          "slotName": "医疗物品 2"
        },
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_0ffaded08c78",
          "slotName": "医疗物品 2"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_39dc75729ade",
          "slotName": "医疗物品2"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_39dc75729ade",
          "slotName": "医疗物品2"
        }
      ],
      "entry": {
        "id": "calok_b",
        "displayName": "CALOK-B止血剂",
        "itemId": "wok_trauma:calok_b",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "FLUX 防弹头盔",
      "sourceClass": "队长",
      "sourceSlot": "头盔",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_85c68ee2594b",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_85c68ee2594b",
          "slotName": "头盔"
        },
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_85c68ee2594b",
          "slotName": "头盔"
        },
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_a788b752d369",
          "slotName": "头盔"
        },
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_a788b752d369",
          "slotName": "头盔"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_85c68ee2594b",
          "slotName": "头盔"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_85c68ee2594b",
          "slotName": "头盔"
        }
      ],
      "entry": {
        "id": "helmet_flux",
        "displayName": "FLUX 防弹头盔",
        "itemId": "wok_infantry_armor:helmet_flux",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "Eagle Industries MMAC 插板胸挂（丛林绿）",
      "sourceClass": "队长",
      "sourceSlot": "护甲",
      "sourceClassId": "custom_8602eece5ba1",
      "sourceSlotId": "custom_1d599f7cfa09",
      "sources": [
        {
          "classId": "custom_8602eece5ba1",
          "className": "队长",
          "slotId": "custom_1d599f7cfa09",
          "slotName": "护甲"
        },
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_1d599f7cfa09",
          "slotName": "护甲"
        },
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_bca8b92dc25c",
          "slotName": "护甲"
        },
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_bca8b92dc25c",
          "slotName": "护甲"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_1d599f7cfa09",
          "slotName": "护甲"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_1d599f7cfa09",
          "slotName": "护甲"
        }
      ],
      "entry": {
        "id": "plate_armor_mmac_ranger_green",
        "displayName": "Eagle Industries MMAC 插板胸挂（丛林绿）",
        "itemId": "wok_infantry_armor:plate_armor_mmac_ranger_green",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M4A1 卡宾枪 ACOG",
      "sourceClass": "步枪兵",
      "sourceSlot": "主武器",
      "sourceClassId": "copy_95873c56176f4c5f",
      "sourceSlotId": "custom_f1b9f2e1f8f4",
      "sources": [
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "m4a1_2",
        "displayName": "M4A1 卡宾枪 ACOG",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentGRIP:{Count:0b,id:\"minecraft:air\"},AttachmentLASER:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:ls321\",LaserColor:-65536}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"suffuse:scope_acogta01\"}},AttachmentSTOCK:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:stock_militech_b5\"}},Extras:{Gun_UUID:\"15e5cab9-5950-43e3-aaca-c999a58e05ab\"},GunCurrentAmmoCount:30,GunFireMode:\"AUTO\",GunId:\"tacz:m4a1\",HasBulletInBarrel:1b,wok_infantry_weapon_editor_id:[I;-313891080,1558987820,-1855586294,-1636629728],wok_infantry_ads_speed_scale:0.45f,wok_infantry_vertical_recoil_scale:4.25f,wok_infantry_horizontal_recoil_scale:3.50f}",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M4A1 卡宾枪 垂直握把+M4红点瞄具",
      "sourceClass": "步枪兵",
      "sourceSlot": "主武器",
      "sourceClassId": "copy_95873c56176f4c5f",
      "sourceSlotId": "custom_f1b9f2e1f8f4",
      "sources": [
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_f1b9f2e1f8f4",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "m4a1",
        "displayName": "M4A1 卡宾枪 垂直握把+M4红点瞄具",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentGRIP:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:grip_vertical_military\"}},AttachmentLASER:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:ls321\",LaserColor:-65536}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"suffuse:scope_compm4\"}},AttachmentSTOCK:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:stock_militech_b5\"}},Extras:{Gun_UUID:\"15e5cab9-5950-43e3-aaca-c999a58e05ab\"},GunCurrentAmmoCount:30,GunFireMode:\"AUTO\",GunId:\"tacz:m4a1\",HasBulletInBarrel:1b,wok_infantry_weapon_editor_id:[I;-313891080,1558987820,-1855586294,-1636629728],wok_infantry_ads_speed_scale:0.60f,wok_infantry_vertical_recoil_scale:3.75f,wok_infantry_horizontal_recoil_scale:3.00f}",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "伯莱塔 M9A4 手枪",
      "sourceClass": "步枪兵",
      "sourceSlot": "副武器",
      "sourceClassId": "copy_95873c56176f4c5f",
      "sourceSlotId": "custom_4986486840d0",
      "sources": [
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_4986486840d0",
          "slotName": "副武器"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_4986486840d0",
          "slotName": "副武器"
        }
      ],
      "entry": {
        "id": "m9a4",
        "displayName": "伯莱塔 M9A4 手枪",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{Extras:{Gun_UUID:\"e23e49ff-b5a9-428f-8b32-e5a5e5a0bad2\"},GunCurrentAmmoCount:17,GunFireMode:\"SEMI\",GunId:\"tacz:m9a4\",HasBulletInBarrel:1b,wok_infantry_weapon_editor_id:[I;379286789,-2042148959,-1886796757,1293427256],wok_infantry_ads_speed_scale:0.75f,wok_infantry_vertical_recoil_scale:2.25f,wok_infantry_horizontal_recoil_scale:1.75f}",
        "ammoReserveLimit": 54
      }
    },
    {
      "label": "M18烟雾弹",
      "sourceClass": "步枪兵",
      "sourceSlot": "道具2",
      "sourceClassId": "copy_95873c56176f4c5f",
      "sourceSlotId": "custom_b9371df3382c",
      "sources": [
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_b9371df3382c",
          "slotName": "道具2"
        },
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_b9371df3382c",
          "slotName": "道具1"
        },
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_b9371df3382c",
          "slotName": "道具2"
        }
      ],
      "entry": {
        "id": "m18_smoke_grenade",
        "displayName": "M18烟雾弹",
        "itemId": "superbwarfare:m18_smoke_grenade",
        "count": 4,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "小型弹药箱",
      "sourceClass": "步枪兵",
      "sourceSlot": "特殊物品",
      "sourceClassId": "copy_95873c56176f4c5f",
      "sourceSlotId": "custom_a7f079bd1766",
      "sources": [
        {
          "classId": "copy_95873c56176f4c5f",
          "className": "步枪兵",
          "slotId": "custom_a7f079bd1766",
          "slotName": "特殊物品"
        }
      ],
      "entry": {
        "id": "ammo_supply_crate",
        "displayName": "小型弹药箱",
        "itemId": "wok_infantry:ammo_supply_crate",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M24 狙击步枪",
      "sourceClass": "侦察兵",
      "sourceSlot": "主武器",
      "sourceClassId": "custom_ac0f9e6f4cfd",
      "sourceSlotId": "custom_ce15388d6e64",
      "sources": [
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_ce15388d6e64",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "m24_renewed",
        "displayName": "M24 狙击步枪",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:scope_standard_8x\",ZoomNumber:5}},Extras:{Gun_UUID:\"14cfebd9-dec4-4855-a957-8f5323188713\"},GunCurrentAmmoCount:5,GunFireMode:\"SEMI\",GunId:\"classicr:m24_renewed\",HasBulletInBarrel:1b,wok_infantry_ads_speed_scale:0.35f,wok_infantry_horizontal_recoil_scale:1.45f,wok_infantry_vertical_recoil_scale:2.5f,wok_infantry_weapon_editor_id:[I;-644395196,-177452292,-1913587766,642603991],wok_infantry_damage_scale:0.7272727f,wok_infantry_armor_ignore_scale:0.8333333f}",
        "ammoReserveLimit": 40
      }
    },
    {
      "label": "伯莱塔 M9A4 手枪",
      "sourceClass": "侦察兵",
      "sourceSlot": "副武器",
      "sourceClassId": "custom_ac0f9e6f4cfd",
      "sourceSlotId": "custom_5517d2a40be7",
      "sources": [
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_5517d2a40be7",
          "slotName": "副武器"
        },
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_5517d2a40be7",
          "slotName": "副武器"
        }
      ],
      "entry": {
        "id": "m9a4",
        "displayName": "伯莱塔 M9A4 手枪",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{Extras:{Gun_UUID:\"b136854f-c3ce-4acc-9783-bb418cc9fbb8\"},GunCurrentAmmoCount:17,GunFireMode:\"SEMI\",GunId:\"tacz:m9a4\",HasBulletInBarrel:1b,wok_infantry_weapon_editor_id:[I;-1855922725,1011501723,-1900975602,-1239196588],wok_infantry_ads_speed_scale:0.75f,wok_infantry_vertical_recoil_scale:2.25f,wok_infantry_horizontal_recoil_scale:1.75f}",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M18烟雾弹",
      "sourceClass": "侦察兵",
      "sourceSlot": "道具1",
      "sourceClassId": "custom_ac0f9e6f4cfd",
      "sourceSlotId": "custom_dcdd97cd92a0",
      "sources": [
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_dcdd97cd92a0",
          "slotName": "道具1"
        }
      ],
      "entry": {
        "id": "m18_smoke_grenade",
        "displayName": "M18烟雾弹",
        "itemId": "superbwarfare:m18_smoke_grenade",
        "count": 3,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "火炮指示器",
      "sourceClass": "侦察兵",
      "sourceSlot": "特殊道具",
      "sourceClassId": "custom_ac0f9e6f4cfd",
      "sourceSlotId": "custom_f2f05e36b10d",
      "sources": [
        {
          "classId": "custom_ac0f9e6f4cfd",
          "className": "侦察兵",
          "slotId": "custom_f2f05e36b10d",
          "slotName": "特殊道具"
        }
      ],
      "entry": {
        "id": "artillery_indicator",
        "displayName": "火炮指示器",
        "itemId": "superbwarfare:artillery_indicator",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "M249 机枪+ACOG 4倍镜+两脚架",
      "sourceClass": "班用机枪手",
      "sourceSlot": "主武器",
      "sourceClassId": "copy_7363b16aef164daa",
      "sourceSlotId": "custom_ce15388d6e64",
      "sources": [
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_ce15388d6e64",
          "slotName": "主武器"
        }
      ],
      "entry": {
        "id": "m249",
        "displayName": "M249 机枪+ACOG 4倍镜+两脚架",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentEXTENDED_MAG:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:extended_mag_1\"}},AttachmentGRIP:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"suffuse:grip_bt10vbatlas\"}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"suffuse:scope_acogta01\"}},Extras:{Gun_UUID:\"03108269-9b46-4ad8-8d09-1a5c2bbe1385\"},GunCurrentAmmoCount:100,GunFireMode:\"AUTO\",GunId:\"tacz:m249\",HasBulletInBarrel:0b,wok_infantry_weapon_editor_id:[I;-1756338582,593249709,-1448174376,1378530356],wok_infantry_ads_speed_scale:0.30f,wok_infantry_vertical_recoil_scale:8.0f,wok_infantry_horizontal_recoil_scale:6.0f,wok_infantry_armor_ignore_scale:1.1666667f,wok_infantry_rpm_scale:0.9375f}",
        "ammoReserveLimit": 300
      }
    },
    {
      "label": "IOTV Gen4 防弹衣（高机动型，复合迷彩）",
      "sourceClass": "班用机枪手",
      "sourceSlot": "护甲",
      "sourceClassId": "copy_7363b16aef164daa",
      "sourceSlotId": "custom_bca8b92dc25c",
      "sources": [
        {
          "classId": "copy_7363b16aef164daa",
          "className": "班用机枪手",
          "slotId": "custom_bca8b92dc25c",
          "slotName": "护甲"
        }
      ],
      "entry": {
        "id": "plate_armor_iotv_gen4_high_mobility",
        "displayName": "IOTV Gen4 防弹衣（高机动型，复合迷彩）",
        "itemId": "wok_infantry_armor:plate_armor_iotv_gen4_high_mobility",
        "count": 1,
        "snbt": "",
        "ammoReserveLimit": 180
      }
    },
    {
      "label": "卡尔-古斯塔夫M4 串联高爆反坦克弹",
      "sourceClass": "重型反坦克兵",
      "sourceSlot": "特殊物品",
      "sourceClassId": "copy_aeec546dce0a44f3",
      "sourceSlotId": "custom_a7f079bd1766",
      "sources": [
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_a7f079bd1766",
          "slotName": "特殊物品"
        }
      ],
      "entry": {
        "id": "carl_gustaf_m4",
        "displayName": "卡尔-古斯塔夫M4 串联高爆反坦克弹",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentEXTENDED_MAG:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"carl_gustaf_m4:ammo_mod_heat\"}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:scope_qmk152\"}},Extras:{Gun_UUID:\"7afc532b-f562-4bf2-925c-e837ca49893f\"},GunCurrentAmmoCount:1,GunFireMode:\"SEMI\",GunId:\"carl_gustaf_m4:carl_gustaf_m4\",HasBulletInBarrel:0b,wok_infantry_ads_speed_scale:0.45f,wok_infantry_spread_scale:0.85f,wok_infantry_vertical_recoil_scale:2.4f,wok_infantry_weapon_editor_id:[I;-523244275,1248152738,-1704330915,725752751]}",
        "ammoReserveLimit": 3
      }
    },
    {
      "label": "卡尔-古斯塔夫M4 温压弹",
      "sourceClass": "重型反坦克兵",
      "sourceSlot": "特殊物品",
      "sourceClassId": "copy_aeec546dce0a44f3",
      "sourceSlotId": "custom_a7f079bd1766",
      "sources": [
        {
          "classId": "copy_aeec546dce0a44f3",
          "className": "重型反坦克兵",
          "slotId": "custom_a7f079bd1766",
          "slotName": "特殊物品"
        }
      ],
      "entry": {
        "id": "carl_gustaf_m4_2",
        "displayName": "卡尔-古斯塔夫M4 温压弹",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{AttachmentEXTENDED_MAG:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"carl_gustaf_m4:ammo_mod_asm\"}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:scope_qmk152\"}},Extras:{Gun_UUID:\"7afc532b-f562-4bf2-925c-e837ca49893f\"},GunCurrentAmmoCount:1,GunFireMode:\"SEMI\",GunId:\"carl_gustaf_m4:carl_gustaf_m4\",HasBulletInBarrel:0b,wok_infantry_ads_speed_scale:0.45f,wok_infantry_spread_scale:0.85f,wok_infantry_vertical_recoil_scale:2.4f,wok_infantry_weapon_editor_id:[I;-523244275,1248152738,-1704330915,725752751]}",
        "ammoReserveLimit": 3
      }
    },
    {
      "label": "§3LMT | M203 榴弹发射器",
      "sourceClass": "榴弹兵",
      "sourceSlot": "副武器",
      "sourceClassId": "copy_f86e610ee9ce4c11",
      "sourceSlotId": "custom_4986486840d0",
      "sources": [
        {
          "classId": "copy_f86e610ee9ce4c11",
          "className": "榴弹兵",
          "slotId": "custom_4986486840d0",
          "slotName": "副武器"
        }
      ],
      "entry": {
        "id": "lmt_m203",
        "displayName": "§3LMT | M203 榴弹发射器",
        "itemId": "tacz:modern_kinetic_gun",
        "count": 1,
        "snbt": "{Extras:{Gun_UUID:\"c5e06ac7-8ecc-4b40-8553-b51c79e81a3c\"},GunCurrentAmmoCount:1,GunFireMode:\"SEMI\",GunId:\"ccrp:lmt_m203\",HasBulletInBarrel:1b,wok_infantry_ads_speed_scale:0.25f,wok_infantry_horizontal_recoil_scale:5.85f,wok_infantry_vertical_recoil_scale:7.5f,wok_infantry_weapon_editor_id:[I;535842110,-925545351,-1524294547,1392510362]}",
        "ammoReserveLimit": 180
      }
    }
  ],
  "guns": [
    {
      "id": "apdf:mdx2",
      "displayName": "马克西姆防务 | PDX-SD 短枪管步枪 ",
      "nameZh": "马克西姆防务 | PDX-SD 短枪管步枪 ",
      "nameEn": "Maxim Defense | PDX-SD SBR",
      "nameKey": "apdf.gun.mdx2.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "rifle",
      "indexPath": "data/apdf/index/guns/mdx2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "apdf:mdx2s",
      "displayName": "马克西姆防务 | PDX 民用限制级手枪",
      "nameZh": "马克西姆防务 | PDX 民用限制级手枪",
      "nameEn": "Maxim Defense | PDX Civ Pistol",
      "nameKey": "apdf.gun.mdx2s.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "rifle",
      "indexPath": "data/apdf/index/guns/mdx2s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "apdf:0950x38",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "apdf:mdx4",
      "displayName": "马克西姆防务 | SDX-508 短枪管步枪",
      "nameZh": "马克西姆防务 | SDX-508 短枪管步枪",
      "nameEn": "Maxim Defense | SDX-508 SBR",
      "nameKey": "apdf.gun.mdx4.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "rifle",
      "indexPath": "data/apdf/index/guns/mdx4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "apdf:mdx5",
      "displayName": "马克西姆防务 | MDX-510 短枪管步枪",
      "nameZh": "马克西姆防务 | MDX-510 短枪管步枪",
      "nameEn": "Maxim Defense | MDX-510 SBR",
      "nameKey": "apdf.gun.mdx5.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "rifle",
      "indexPath": "data/apdf/index/guns/mdx5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "apdf:mdxu",
      "displayName": "马克西姆防务 | MDX 定制款 全尺寸步枪",
      "nameZh": "马克西姆防务 | MDX 定制款 全尺寸步枪",
      "nameEn": "Maxim Defense | MDX Custom AR",
      "nameKey": "apdf.gun.mdxu.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "rifle",
      "indexPath": "data/apdf/index/guns/mdxu.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "apdf:1163x39",
      "ammoAmount": 10,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "apdf:skw8",
      "displayName": "塔诺夫机械 | SKW-338 后期型 半自动狙击步枪",
      "nameZh": "塔诺夫机械 | SKW-338 后期型 半自动狙击步枪",
      "nameEn": "Zakłady Mechaniczne Tarnów | SKW-338 Late LR",
      "nameKey": "apdf.gun.skw8.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "rifle",
      "indexPath": "data/apdf/index/guns/skw8.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "bhp:bhp",
      "displayName": "M1935 勃朗宁HP",
      "nameZh": "M1935 勃朗宁HP",
      "nameEn": "M1935 Browning HP",
      "nameKey": "bhp.gun.bhp.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "pistol",
      "indexPath": "data/bhp/index/guns/bhp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 13,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "carl_gustaf_m4:carl_gustaf_m4",
      "displayName": "卡尔-古斯塔夫M4",
      "nameZh": "卡尔-古斯塔夫M4",
      "nameEn": "Carl Gustaf M4 Recoilless Rifle",
      "nameKey": "carl_gustaf_m4.gun.carl_gustaf_m4.name",
      "source": "tacz/carl_gustaf_m4_converted_fixed.zip",
      "sources": [
        "tacz/carl_gustaf_m4_converted_fixed.zip"
      ],
      "type": "rpg",
      "indexPath": "data/carl_gustaf_m4/index/guns/carl_gustaf_m4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:rpg_rocket",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:a545",
      "displayName": "捷格加廖夫 | A-545 突击步枪",
      "nameZh": "捷格加廖夫 | A-545 突击步枪",
      "nameEn": "Dеgtyaryovа | A-545",
      "nameKey": "ccrp.gun.a545.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/a545.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aac_honeybadger",
      "displayName": "AAC丨蜜獾 突击步枪",
      "nameZh": "AAC丨蜜獾 突击步枪",
      "nameEn": "AAC | Honey Badger",
      "nameKey": "ccrp.gun.aac_honeybadger.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/aac_honeybadger.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:af2011",
      "displayName": "Arsenal | AF2011",
      "nameZh": "Arsenal | AF2011",
      "nameEn": "Arsenal | AF2011",
      "nameKey": "ccrp.gun.af2011.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/af2011.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aics_m700",
      "displayName": "精密国际&雷明顿 | AICS R700",
      "nameZh": "精密国际&雷明顿 | AICS R700",
      "nameEn": "AI&Remington | AICS R700",
      "nameKey": "ccrp.gun.aics_m700.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/aics_m700.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:65cm",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ak103",
      "displayName": "伊孜玛什 | AK103 突击步枪",
      "nameZh": "伊孜玛什 | AK103 突击步枪",
      "nameEn": "Izhmash | AK103",
      "nameKey": "ccrp.gun.ak103.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ak103.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ak47_spent_bullet",
      "displayName": "AK47丨英雄之殁",
      "nameZh": "AK47丨英雄之殁",
      "nameEn": "AK47-Hero's Miracle",
      "nameKey": "ccrp.gun.ak47_spent_bullet.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ak47_spent_bullet.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:ak74",
      "displayName": "伊孜玛什 | AK74 突击步枪",
      "nameZh": "伊孜玛什 | AK74 突击步枪",
      "nameEn": "Izhmash | AK74",
      "nameKey": "ccrp.gun.ak74.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ak74.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ak74m",
      "displayName": "伊孜玛什 | AK74M 突击步枪",
      "nameZh": "伊孜玛什 | AK74M 突击步枪",
      "nameEn": "Izhmash | AK74M",
      "nameKey": "ccrp.gun.ak74m.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ak74m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aks74u",
      "displayName": "卡拉什尼科夫 | AKS-74U",
      "nameZh": "卡拉什尼科夫 | AKS-74U",
      "nameEn": "Kalashnikov | AKS-74U",
      "nameKey": "ccrp.gun.aks74u.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/aks74u.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:am17",
      "displayName": "卡拉什尼科夫 | AM-17 短突击步枪",
      "nameZh": "卡拉什尼科夫 | AM-17 短突击步枪",
      "nameEn": "AM-17 SBR",
      "nameKey": "ccrp.gun.am17.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/am17.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:apc_9k_pro_g",
      "displayName": "B&T | APC9K Pro G",
      "nameZh": "B&T | APC9K Pro G",
      "nameEn": "APC9K Pro G",
      "nameKey": "ccrp.gun.apc_9k_pro_g.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/apc_9k_pro_g.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 25,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "stock",
        "laser",
        "grip",
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ar57",
      "displayName": "57Center丨AR57",
      "nameZh": "57Center丨AR57",
      "nameEn": "57Center | AR57",
      "nameKey": "ccrp.gun.ar57.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/ar57.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "stock",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_a3",
      "displayName": "斯泰尔 | AUG A3 突击步枪",
      "nameZh": "斯泰尔 | AUG A3 突击步枪",
      "nameEn": "Steyr | AUG A3",
      "nameKey": "ccrp.gun.aug_a3.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/aug_a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "laser",
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_a3_dmr",
      "displayName": "斯泰尔 | AUGA3 DMR",
      "nameZh": "斯泰尔 | AUGA3 DMR",
      "nameEn": "Steyr | AUGA3 DMR",
      "nameKey": "ccrp.gun.aug_a3_dmr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/aug_a3_dmr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "laser",
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_a3_m2kit",
      "displayName": "斯泰尔 | AUGA3 M2",
      "nameZh": "斯泰尔 | AUGA3 M2",
      "nameEn": "Steyr | AUGA3 M2",
      "nameKey": "ccrp.gun.aug_a3_m2kit.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/aug_a3_m2kit.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:556x45_m995",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "laser",
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_a3s",
      "displayName": "齐玛军械&斯泰尔 | AUG-SD",
      "nameZh": "齐玛军械&斯泰尔 | AUG-SD",
      "nameEn": "CAMG&Steyr | AUGA3-S",
      "nameKey": "ccrp.gun.aug_a3s.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/aug_a3s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "laser",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_camg_kit",
      "displayName": "齐玛军械 | AUGA3 狂猎套件",
      "nameZh": "齐玛军械 | AUGA3 狂猎套件",
      "nameEn": "CAMG | AUGA3 Wild Hunt Kit",
      "nameKey": "ccrp.gun.aug_camg_kit.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/aug_camg_kit.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "laser",
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_hbar",
      "displayName": "斯泰尔丨AUG HBAR",
      "nameZh": "斯泰尔丨AUG HBAR",
      "nameEn": "Steyr | AUG HBAR",
      "nameKey": "ccrp.gun.aug_hbar.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/aug_hbar.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 60,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "grip",
        "laser",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:aug_para",
      "displayName": "斯泰尔丨AUG Para",
      "nameZh": "斯泰尔丨AUG Para",
      "nameEn": "Steyr | AUG Para",
      "nameKey": "ccrp.gun.aug_para.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/aug_para.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 25,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "laser",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:block_17",
      "displayName": "Glock17丨波洛克",
      "nameZh": "Glock17丨波洛克",
      "nameEn": "Glock17 | BLOCK",
      "nameKey": "ccrp.gun.block_17.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/block_17.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:brn_180_bullpup",
      "displayName": "布劳内尔&世锐精密 | BRN-180 突击步枪【SARB-15无托套件】",
      "nameZh": "布劳内尔&世锐精密 | BRN-180 突击步枪【SARB-15无托套件】",
      "nameEn": "Brownells&SRU | BRN-180[SARB-15 Bullpup Chassis]",
      "nameKey": "ccrp.gun.brn_180_bullpup.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/brn_180_bullpup.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "laser",
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:camg_cheetah40",
      "displayName": "CAMG | Cheetah40",
      "nameZh": "",
      "nameEn": "CAMG | Cheetah40",
      "nameKey": "ccrp.gun.camg_cheetah40.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/camg_cheetah40.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:40sw",
      "ammoAmount": 17,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:camg_dexterous",
      "displayName": "齐玛军械&ARIP | Dexterous DTR",
      "nameZh": "齐玛军械&ARIP | Dexterous DTR",
      "nameEn": "CAMG&ARIP | Dexterous",
      "nameKey": "ccrp.gun.camg_dexterous.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/camg_dexterous.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:47x43caseless",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "ccrp:camg_krait",
      "displayName": "齐玛军械 | Krait 重型战斗步枪",
      "nameZh": "齐玛军械 | Krait 重型战斗步枪",
      "nameEn": "CAMG | Krait Battle Rifle",
      "nameKey": "ccrp.gun.camg_krait.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/camg_krait.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:68x51tvcm",
      "ammoAmount": 25,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:camg_m1014",
      "displayName": "伯奈利 | M4 Super90 & 齐玛军械 巨齿鲨套件",
      "nameZh": "伯奈利 | M4 Super90 & 齐玛军械 巨齿鲨套件",
      "nameEn": "Benelli | M4 Super90 & CAMG \"Megalodon\" Kit",
      "nameKey": "ccrp.gun.camg_m1014.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/ccrp/index/guns/camg_m1014.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "laser",
        "grip",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:camg_m4_sopmod2",
      "displayName": "齐玛军械 | M4 SOPMOD II 突击步枪",
      "nameZh": "齐玛军械 | M4 SOPMOD II 突击步枪",
      "nameEn": "CAMG | M4 SOPMOD II",
      "nameKey": "ccrp.gun.m4_sopmod2.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/camg_m4_sopmod2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:camg_mk18",
      "displayName": "齐玛军械 | MK18突击步枪",
      "nameZh": "齐玛军械 | MK18突击步枪",
      "nameEn": "CAMG | MK18",
      "nameKey": "ccrp.gun.camg_mk18.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/camg_mk18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:car_15",
      "displayName": "柯尔特 | CAR-15 突击步枪",
      "nameZh": "柯尔特 | CAR-15 突击步枪",
      "nameEn": "Colt | CAR-15",
      "nameKey": "ccrp.gun.car_15.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/car_15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:chisato_m1911",
      "displayName": "Detonics丨战斗大师1911",
      "nameZh": "Detonics丨战斗大师1911",
      "nameEn": "Detonics | Combat Master 1911",
      "nameKey": "ccrp.gun.chisato_m1911.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/chisato_m1911.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:cr300",
      "displayName": "HAENEL | CR300",
      "nameZh": "HAENEL | CR300",
      "nameEn": "HAENEL | CR300",
      "nameKey": "ccrp.gun.cr300.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/cr300.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:crow_and_egret",
      "displayName": "齐玛军械 | 黑鸦与白鹭",
      "nameZh": "齐玛军械 | 黑鸦与白鹭",
      "nameEn": "CAMG | Crow and Egret",
      "nameKey": "ccrp.gun.crow_and_egret.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/crow_and_egret.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50ae",
      "ammoAmount": 16,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:cslr_42a",
      "displayName": "CS/LR 42A 短突击步枪",
      "nameZh": "CS/LR 42A 短突击步枪",
      "nameEn": "CS/LR 42A SBR",
      "nameKey": "ccrp.gun.cslr_42a.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/cslr_42a.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:cslr_43a",
      "displayName": "CS/LR 43A 短突击步枪",
      "nameZh": "CS/LR 43A 短突击步枪",
      "nameEn": "CS/LR 43A SBR",
      "nameKey": "ccrp.gun.cslr_43a.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/cslr_43a.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:cslr_44",
      "displayName": "CS/LR 44 精确射手步枪",
      "nameZh": "CS/LR 44 精确射手步枪",
      "nameEn": "CS/LR 44 DMR",
      "nameKey": "ccrp.gun.cslr_44.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/cslr_44.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ddm4",
      "displayName": "丹尼尔防务 | DDM4 V7突击步枪",
      "nameZh": "丹尼尔防务 | DDM4 V7突击步枪",
      "nameEn": "Daniel Defense | DDM4 V7",
      "nameKey": "ccrp.gun.ddm4.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ddm4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:556x45_m995",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ddm4_pdw",
      "displayName": "丹尼尔防务 | DDM4 PDW个人防卫武器",
      "nameZh": "丹尼尔防务 | DDM4 PDW个人防卫武器",
      "nameEn": "Daniel Defense | DDM4 PDW",
      "nameKey": "ccrp.gun.ddm4_pdw.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ddm4_pdw.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ddm4_v7_pro",
      "displayName": "丹尼尔防务 | DDM4 V7 Pro突击步枪",
      "nameZh": "丹尼尔防务 | DDM4 V7 Pro突击步枪",
      "nameEn": "Daniel Defense | DDM4 V7 Pro",
      "nameKey": "ccrp.gun.ddm4_v7_pro.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ddm4_v7_pro.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:556x45_m855a2_f",
      "ammoAmount": 32,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ddm4a1",
      "displayName": "丹尼尔防务 | DD M4A1 突击步枪",
      "nameZh": "丹尼尔防务 | DD M4A1 突击步枪",
      "nameEn": "Daniel Defense | DD M4A1",
      "nameKey": "ccrp.gun.ddm4a1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/ddm4a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:deagle_nightingale",
      "displayName": "IWI&齐玛军械 | 沙漠之鹰 夜莺",
      "nameZh": "IWI&齐玛军械 | 沙漠之鹰 夜莺",
      "nameEn": "IMI&CAMG | Desert Eagle Nightingale",
      "nameKey": "ccrp.gun.deagle_nightingale.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/deagle_nightingale.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:44mag",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:deagle_xix",
      "displayName": "IMI | 沙漠之鹰XIX",
      "nameZh": "IMI | 沙漠之鹰XIX",
      "nameEn": "IMI | Desert Eagle XIX",
      "nameKey": "ccrp.gun.deagle_xix.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/deagle_xix.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50ae",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:dsa_sa58",
      "displayName": "DSA | SA58 战斗步枪",
      "nameZh": "DSA | SA58 战斗步枪",
      "nameEn": "DSA | SA58",
      "nameKey": "ccrp.gun.dsa_sa58.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/dsa_sa58.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 15,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "muzzle",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:f90",
      "displayName": "利特高 | F90卡宾枪",
      "nameZh": "利特高 | F90卡宾枪",
      "nameEn": "Lithgow Arms | F90 Carbine",
      "nameKey": "ccrp.gun.f90.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/f90.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "laser",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:f90_mbr",
      "displayName": "利特高 | F90 MBR 突击步枪",
      "nameZh": "利特高 | F90 MBR 突击步枪",
      "nameEn": "Lithgow Arms | F90 MBR",
      "nameKey": "ccrp.gun.f90_mbr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/f90_mbr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "laser",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:fightlite_scr_hg",
      "displayName": "Fightlite | SCR掠夺者 手枪",
      "nameZh": "Fightlite | SCR掠夺者 手枪",
      "nameEn": "Fightlite | SCR Raider",
      "nameKey": "ccrp.gun.fightlite_scr_hg.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/fightlite_scr_hg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:g95a1",
      "displayName": "HK | G95A1 突击步枪",
      "nameZh": "HK | G95A1 突击步枪",
      "nameEn": "HK | G95A1",
      "nameKey": "ccrp.gun.g95a1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/g95a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:glock20gen5mos",
      "displayName": "Glock | G20 gen5 mos",
      "nameZh": "Glock | G20 gen5 mos",
      "nameEn": "Glock | G20 gen5 mos",
      "nameKey": "ccrp.gun.glock20gen5mos.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/glock20gen5mos.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:10mm",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:glock40gen5mos",
      "displayName": "Glock | G40 gen5 mos",
      "nameZh": "Glock | G40 gen5 mos",
      "nameEn": "Glock | G40 gen5 mos",
      "nameKey": "ccrp.gun.glock40gen5mos.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/glock40gen5mos.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:10mm",
      "ammoAmount": 15,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "stock",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk21",
      "displayName": "HK | HK21",
      "nameZh": "HK | HK21",
      "nameEn": "HK | HK21",
      "nameKey": "ccrp.gun.hk21.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/hk21.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 200,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "muzzle",
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:hk416",
      "displayName": "HK | HK416",
      "nameZh": "HK | HK416",
      "nameEn": "HK | HK416",
      "nameKey": "ccrp.gun.hk416.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk416.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk416_sopmod",
      "displayName": "SOCOM&HK | HK416 CQB SOPMOD",
      "nameZh": "SOCOM&HK | HK416 CQB SOPMOD",
      "nameEn": "SOCOM&HK | HK416 CQB SOPMOD",
      "nameKey": "ccrp.gun.hk416_sopmod.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk416_sopmod.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk416a8",
      "displayName": "HK | HK416A8",
      "nameZh": "HK | HK416A8",
      "nameEn": "HK | HK416A8",
      "nameKey": "ccrp.gun.hk416a8.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk416a8.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk416c",
      "displayName": "HK416C 短突击步枪",
      "nameZh": "HK416C 短突击步枪",
      "nameEn": "HK416C",
      "nameKey": "ccrp.gun.hk416c.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk416c.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk417",
      "displayName": "HK | HK417",
      "nameZh": "HK | HK417",
      "nameEn": "HK | HK417",
      "nameKey": "ccrp.gun.hk417.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk417.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk433",
      "displayName": "HK | HK433 突击步枪",
      "nameZh": "HK | HK433 突击步枪",
      "nameEn": "HK | HK433",
      "nameKey": "ccrp.gun.hk433.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk433.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk_g28",
      "displayName": "HK | G28 精确射手步枪",
      "nameZh": "HK | G28 精确射手步枪",
      "nameEn": "",
      "nameKey": "ccrp.gun.hk_g28.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/hk_g28.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:hk_g28_patrol",
      "displayName": "HK | G28 巡逻型 精确射手步枪",
      "nameZh": "HK | G28 巡逻型 精确射手步枪",
      "nameEn": "",
      "nameKey": "ccrp.gun.hk_g28_patrol.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/hk_g28_patrol.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:honeybadger",
      "displayName": "Q LLC | 蜜獾 突击步枪",
      "nameZh": "Q LLC | 蜜獾 突击步枪",
      "nameEn": "Q LLC | Honey Badger",
      "nameKey": "ccrp.gun.honeybadger.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/honeybadger.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "classicr:muzzle_silencer_default": {
          "distance_addend": -24,
          "use_silence_sound": true
        }
      }
    },
    {
      "id": "ccrp:kac_ks_1",
      "displayName": "奈特军械 | KS-1 突击步枪",
      "nameZh": "奈特军械 | KS-1 突击步枪",
      "nameEn": "KAC | KS-1",
      "nameKey": "ccrp.gun.kac_ks_1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/kac_ks_1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:keltec_p50",
      "displayName": "KelTec丨P50",
      "nameZh": "KelTec丨P50",
      "nameEn": "KelTec | P50",
      "nameKey": "ccrp.gun.keltec_p50.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/keltec_p50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:km_ak74m",
      "displayName": "伊孜玛什 | KM-AK74M 突击步枪",
      "nameZh": "伊孜玛什 | KM-AK74M 突击步枪",
      "nameEn": "Izhmash | KM-AK74M",
      "nameKey": "ccrp.gun.km_ak74m.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/km_ak74m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:lastwar",
      "displayName": "【最后一战】",
      "nameZh": "【最后一战】",
      "nameEn": "[LAST WAR]",
      "nameKey": "ccrp.gun.lastwar.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/ccrp/index/guns/lastwar.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:lmt_m203",
      "displayName": "LMT | M203 榴弹发射器",
      "nameZh": "LMT | M203 榴弹发射器",
      "nameEn": "LMT | M203 Grenade Launcher",
      "nameKey": "ccrp.gun.lmt_m203.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rpg",
      "indexPath": "data/ccrp/index/guns/lmt_m203.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "grip",
        "scope",
        "stock",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m110",
      "displayName": "骑士军械 | M110 SASS狙击步枪",
      "nameZh": "骑士军械 | M110 SASS狙击步枪",
      "nameEn": "KAC | M110 SASS",
      "nameKey": "ccrp.gun.m110.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/m110.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m110a3",
      "displayName": "骑士军械 | M110A3 精确射手步枪",
      "nameZh": "骑士军械 | M110A3 精确射手步枪",
      "nameEn": "KAC | M110A3",
      "nameKey": "ccrp.gun.m110a3.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/m110a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:65cm",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m14_hbar",
      "displayName": "春田 | M14A1",
      "nameZh": "春田 | M14A1",
      "nameEn": "Springfield | M14A1",
      "nameKey": "ccrp.gun.m14_hbar.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/m14_hbar.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "module",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m16a3",
      "displayName": "北方工业 | M16A3 突击步枪",
      "nameZh": "北方工业 | M16A3 突击步枪",
      "nameEn": "Norinco | M16A3",
      "nameKey": "ccrp.gun.m16a3.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/m16a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m1887_long",
      "displayName": "温彻斯特 | M1887",
      "nameZh": "温彻斯特 | M1887",
      "nameEn": "Winchester | M1887",
      "nameKey": "ccrp.gun.m1887_long.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/ccrp/index/guns/m1887_long.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "grip",
        "laser",
        "scope",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m231",
      "displayName": "柯尔特 | M231 射孔枪",
      "nameZh": "柯尔特 | M231 射孔枪",
      "nameEn": "Colt | M231 FPW",
      "nameKey": "ccrp.gun.m231.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/m231.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m249_saw",
      "displayName": "M249丨STANAG供弹具",
      "nameZh": "M249丨STANAG供弹具",
      "nameEn": "M249 | STANAG Feed",
      "nameKey": "ccrp.gun.m249_saw.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/m249_saw.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m27_iar",
      "displayName": "M27 IAR 班用机枪",
      "nameZh": "M27 IAR 班用机枪",
      "nameEn": "M27 IAR",
      "nameKey": "ccrp.gun.m27_iar.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/m27_iar.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 40,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m305a",
      "displayName": "北方工业 | M305A",
      "nameZh": "北方工业 | M305A",
      "nameEn": "Norinco | M305A",
      "nameKey": "ccrp.gun.m305a.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/m305a.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m38_spr",
      "displayName": "M38 SPR 精确射手步枪",
      "nameZh": "M38 SPR 精确射手步枪",
      "nameEn": "M38 SPR",
      "nameKey": "ccrp.gun.m38_spr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/m38_spr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m39_emr",
      "displayName": "M39 EMR 精确射手步枪",
      "nameZh": "M39 EMR 精确射手步枪",
      "nameEn": "M39 EMR",
      "nameKey": "ccrp.gun.m39_emr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/m39_emr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m4_bolter",
      "displayName": "战锤科技 | M4爆矢枪",
      "nameZh": "战锤科技 | M4爆矢枪",
      "nameEn": "Warhammer Technology | M4 BOLTER",
      "nameKey": "ccrp.gun.m4_bolter.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rpg",
      "indexPath": "data/ccrp/index/guns/m4_bolter.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:195x55",
      "ammoAmount": 8,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m4_cqbr",
      "displayName": "NSWC | Mk.18 Mod 0 CQBR",
      "nameZh": "NSWC | Mk.18 Mod 0 CQBR",
      "nameEn": "NSWC | Mk.18 Mod 0 CQBR",
      "nameKey": "ccrp.gun.m4_cqbr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/m4_cqbr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m4_sopmod2_fsp",
      "displayName": "柯尔特 | M4A1 Block2 FSP 突击步枪",
      "nameZh": "柯尔特 | M4A1 Block2 FSP 突击步枪",
      "nameEn": "Colt | M4A1 Block2 FSP",
      "nameKey": "ccrp.gun.m4_sopmod2_fsp.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/m4_sopmod2_fsp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:m4_ss",
      "displayName": "A.R.M.S&齐玛军械 | M4 SIR Systems",
      "nameZh": "A.R.M.S&齐玛军械 | M4 SIR Systems",
      "nameEn": "A.R.M.S&CAMG | M4 SIR Systems",
      "nameKey": "ccrp.gun.m4_ss.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/m4_ss.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:marlin_1895",
      "displayName": "马林 | Model 1895",
      "nameZh": "马林 | Model 1895",
      "nameEn": "Marlin | Model 1895",
      "nameKey": "ccrp.gun.marlin_1895.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/marlin_1895.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45_70",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle",
        "grip",
        "laser",
        "scope",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mcx_spear_tombstone",
      "displayName": "SIG&齐玛军械丨M7【墓石】套件",
      "nameZh": "SIG&齐玛军械丨M7【墓石】套件",
      "nameEn": "SIG Spear | \"TombStone\" Lever Action Kit",
      "nameKey": "ccrp.gun.mcx_spear_tombstone.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/mcx_spear_tombstone.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:68x51fury",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "stock",
        "grip",
        "laser",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mcx_virtus",
      "displayName": "SIG | MCX Virtus 突击步枪",
      "nameZh": "SIG | MCX Virtus 突击步枪",
      "nameEn": "MCX Virtus",
      "nameKey": "ccrp.gun.mcx_virtus.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/mcx_virtus.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mg36",
      "displayName": "HK | MG36 班用机枪",
      "nameZh": "HK | MG36 班用机枪",
      "nameEn": "HK | MG36",
      "nameKey": "ccrp.gun.mg36.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/mg36.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mk13_mod5",
      "displayName": "NSWC-CRANE | MK13 Mod5狙击步枪",
      "nameZh": "NSWC-CRANE | MK13 Mod5狙击步枪",
      "nameEn": "NSWC-CRANE | MK13 Mod5",
      "nameKey": "ccrp.gun.mk13_mod5.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/mk13_mod5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:300wm",
      "ammoAmount": 5,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mk17",
      "displayName": "FN 赫斯塔尔 | MK17 Mod0 战斗步枪",
      "nameZh": "FN 赫斯塔尔 | MK17 Mod0 战斗步枪",
      "nameEn": "FN | MK17 Mod0 Battle Rifle",
      "nameKey": "ccrp.gun.mk17.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/mk17.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "ccrp:mk18_mjolnir",
      "displayName": "长剑防卫丨MK18 SA-ASR",
      "nameZh": "长剑防卫丨MK18 SA-ASR",
      "nameEn": "SDI | \"Mjölnir\"MK18 SA-ASR",
      "nameKey": "ccrp.gun.mk18_mjolnir.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/mk18_mjolnir.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "laser",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mk556",
      "displayName": "HAENEL | MK556",
      "nameZh": "HAENEL | MK556",
      "nameEn": "HAENEL | MK556",
      "nameKey": "ccrp.gun.mk556.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/mk556.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mp5_sd",
      "displayName": "HK | MP5SD 微声冲锋枪",
      "nameZh": "HK | MP5SD 微声冲锋枪",
      "nameEn": "MP5SD Silenced SMG",
      "nameKey": "ccrp.gun.mp5_sd.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/mp5_sd.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mp5k",
      "displayName": "HK | MP5K 冲锋枪",
      "nameZh": "HK | MP5K 冲锋枪",
      "nameEn": "HK | MP5K",
      "nameKey": "ccrp.gun.mp5k.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/mp5k.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 15,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mp5k_pdw",
      "displayName": "HK | MP5K PDW冲锋枪",
      "nameZh": "HK | MP5K PDW冲锋枪",
      "nameEn": "HK | MP5K PDW",
      "nameKey": "ccrp.gun.mp5k_pdw.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/mp5k_pdw.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mp7a3",
      "displayName": "HK&齐玛军械 | Cavaliere4.6 个人防卫武器",
      "nameZh": "HK&齐玛军械 | Cavaliere4.6 个人防卫武器",
      "nameEn": "HK&CAM Group | MP7A3 Stinger Kit",
      "nameKey": "ccrp.gun.mp7a3.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/mp7a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:46x30",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mp9_thunder",
      "displayName": "MP9丨雷雲",
      "nameZh": "MP9丨雷雲",
      "nameEn": "MP9 | Thunder",
      "nameKey": "ccrp.gun.mp9_thunder.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/mp9_thunder.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:thunder_cell",
      "ammoAmount": 40,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:mpx",
      "displayName": "西格绍尔 | MPX冲锋枪",
      "nameZh": "西格绍尔 | MPX冲锋枪",
      "nameEn": "SIG | MPX",
      "nameKey": "ccrp.gun.mpx.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/mpx.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 25,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:msh41",
      "displayName": "MSH-41",
      "nameZh": "MSH-41",
      "nameEn": "MSH-41",
      "nameKey": "ccrp.gun.msh41.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/msh41.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:p90_effen_90",
      "displayName": "FN赫斯塔尔 P90 | EFFEN90导轨套件",
      "nameZh": "FN赫斯塔尔 P90 | EFFEN90导轨套件",
      "nameEn": "FN P90 | EFFEN90 Rail Kit",
      "nameKey": "ccrp.gun.p90_effen_90.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/p90_effen_90.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:p90_paw",
      "displayName": "FN赫斯塔尔 P90丨PAW延展套件",
      "nameZh": "FN赫斯塔尔 P90丨PAW延展套件",
      "nameEn": "FN P90 | PAW Extension Kit",
      "nameKey": "ccrp.gun.p90_paw.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/p90_paw.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:p90_shround_s",
      "displayName": "FN赫斯塔尔 P90丨静谧死神系统",
      "nameZh": "FN赫斯塔尔 P90丨静谧死神系统",
      "nameEn": "FN P90 | Shroud Reaper System",
      "nameKey": "ccrp.gun.p90.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/p90_shround_s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:psa_ak556",
      "displayName": "帕尔梅托州立兵工厂 | AK556 突击步枪",
      "nameZh": "帕尔梅托州立兵工厂 | AK556 突击步枪",
      "nameEn": "PSA | AK556",
      "nameKey": "ccrp.gun.psa_ak556.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/psa_ak556.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:qbu_191",
      "displayName": "QBU-191精确射手步枪",
      "nameZh": "QBU-191精确射手步枪",
      "nameEn": "QBU-191 DMR",
      "nameKey": "ccrp.gun.qbu_191.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/qbu_191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:qbz_191",
      "displayName": "QBZ-191突击步枪",
      "nameZh": "QBZ-191突击步枪",
      "nameEn": "",
      "nameKey": "ccrp.gun.qbz_191.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/qbz_191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:rd704",
      "displayName": "Rifle Dynamics | RD704",
      "nameZh": "Rifle Dynamics | RD704",
      "nameEn": "Rifle Dynamics | RD704",
      "nameKey": "ccrp.gun.rd704.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/rd704.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:requiem",
      "displayName": "齐玛军械 | 安魂",
      "nameZh": "齐玛军械 | 安魂",
      "nameEn": "CAMG | Requiem",
      "nameKey": "ccrp.gun.requiem.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/requiem.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:127x55",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:ro635",
      "displayName": "柯尔特 | RO635 冲锋枪",
      "nameZh": "柯尔特 | RO635 冲锋枪",
      "nameEn": "",
      "nameKey": "ccrp.gun.ro635.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/ro635.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:rpk74m",
      "displayName": "伊孜玛什 | RPK74M 突击步枪",
      "nameZh": "伊孜玛什 | RPK74M 突击步枪",
      "nameEn": "Izhmash | RPK74M",
      "nameKey": "ccrp.gun.rpk74m.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/rpk74m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 45,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:rpk_203",
      "displayName": "伊孜玛什 | RPK203 轻机枪",
      "nameZh": "伊孜玛什 | RPK203 轻机枪",
      "nameEn": "Izhmash | RPK203",
      "nameKey": "ccrp.gun.rpk_203.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/rpk_203.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 40,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:sai_gry_lite_black",
      "displayName": "SAI | GRY 突击步枪",
      "nameZh": "SAI | GRY 突击步枪",
      "nameEn": "SAI | GRY LITE Black",
      "nameKey": "ccrp.gun.sai_gry_lite_black.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/sai_gry_lite_black.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:samurai_edge",
      "displayName": "齐玛军械 | 武士之刃",
      "nameZh": "齐玛军械 | 武士之刃",
      "nameEn": "CAMG | Samurai's Edge",
      "nameKey": "ccrp.gun.samurai_edge.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/samurai_edge.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:scar16_ariana",
      "displayName": "FN 赫斯塔尔 | SCAR-16 艾莉安娜",
      "nameZh": "FN 赫斯塔尔 | SCAR-16 艾莉安娜",
      "nameEn": "FN | SCAR-16 Ariana",
      "nameKey": "ccrp.gun.scar16_ariana.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/scar16_ariana.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:scar17_armarise",
      "displayName": "FN 赫斯塔尔 | SCAR-17 阿玛瑞斯",
      "nameZh": "FN 赫斯塔尔 | SCAR-17 阿玛瑞斯",
      "nameEn": "FN | SCAR-17 Armarise",
      "nameKey": "ccrp.gun.scar17_armarise.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/scar17_armarise.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "ccrp:scar_16s",
      "displayName": "FN赫斯塔尔&Tangodown | SCAR-16S 突击步枪",
      "nameZh": "FN赫斯塔尔&Tangodown | SCAR-16S 突击步枪",
      "nameEn": "FN Herstal&Tangodown | SCAR-16S",
      "nameKey": "ccrp.gun.scar_16s.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/scar_16s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:scar_17s",
      "displayName": "FN 赫斯塔尔&X-Product | SCAR X-17S 精确射手步枪",
      "nameZh": "FN 赫斯塔尔&X-Product | SCAR X-17S 精确射手步枪",
      "nameEn": "FN & X-Product | SCAR X-17S DMR",
      "nameKey": "ccrp.gun.scar_17s.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/scar_17s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:65cm",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "ccrp:scar_sc",
      "displayName": "FN赫斯塔尔 | SCAR SC紧凑型卡宾枪",
      "nameZh": "FN赫斯塔尔 | SCAR SC紧凑型卡宾枪",
      "nameEn": "FN | SCAR SC",
      "nameKey": "ccrp.gun.scar_sc.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/scar_sc.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:shield_ots33",
      "displayName": "FORT | VantVM战术突击盾 配装 OTS-33 冲锋手枪",
      "nameZh": "FORT | VantVM战术突击盾 配装 OTS-33 冲锋手枪",
      "nameEn": "FORT | VantVM with OTS-33 pistol",
      "nameKey": "ccrp.gun.shield_ots33.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/shield_ots33.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 18,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle",
        "laser",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:sig277",
      "displayName": "SIG&齐玛军械 | M7 飞龙套件",
      "nameZh": "SIG&齐玛军械 | M7 飞龙套件",
      "nameEn": "M7A1",
      "nameKey": "ccrp.gun.sig277.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/sig277.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:68x51fury",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "laser",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:silence_meteor",
      "displayName": "限定典藏 |【缄默彗星】",
      "nameZh": "限定典藏 |【缄默彗星】",
      "nameEn": "CAMG | [Silence Meteor]",
      "nameKey": "ccrp.gun.silence_meteor.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/silence_meteor.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:solgw_mk1",
      "displayName": "自由之子 | MK1 突击步枪",
      "nameZh": "自由之子 | MK1 突击步枪",
      "nameEn": "SOLGW | MK1",
      "nameKey": "ccrp.gun.solgw_mk1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/solgw_mk1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ccrp:556x45_m855a1",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:springfield1873_tube_mag",
      "displayName": "春田 | Model 1873 弹仓供弹（实验型）",
      "nameZh": "春田 | Model 1873 弹仓供弹（实验型）",
      "nameEn": "SpringField | Model 1873 Tube-Mag (Experimental)",
      "nameKey": "ccrp.gun.springfield1873_tube_mag.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/springfield1873_tube_mag.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45_70",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:sr25",
      "displayName": "骑士军械 | SR25精确射手步枪",
      "nameZh": "骑士军械 | SR25精确射手步枪",
      "nameEn": "KAC | SR25 SR",
      "nameKey": "ccrp.gun.sr25.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/sr25.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:sr_3m",
      "displayName": "SR-3M 微声冲锋枪",
      "nameZh": "SR-3M 微声冲锋枪",
      "nameEn": "SR-3M",
      "nameKey": "ccrp.gun.sr_3m.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/sr_3m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:timeless_50",
      "displayName": " TAC | 永恒 .50 L",
      "nameZh": " TAC | 永恒 .50 L",
      "nameEn": " TAC | Timeless 50L",
      "nameKey": "ccrp.gun.timeless_50_long.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ccrp/index/guns/timeless_50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50ae",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:troy_m14_sass",
      "displayName": "Troy | M14 MCS 护木套件",
      "nameZh": "Troy | M14 MCS 护木套件",
      "nameEn": "Troy | M14 MCS Chasis",
      "nameKey": "ccrp.gun.troy_m14_sass.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ccrp/index/guns/troy_m14_sass.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:tti_mpx",
      "displayName": "塔兰战术 | MPX冲锋枪",
      "nameZh": "塔兰战术 | MPX冲锋枪",
      "nameEn": "Taran Tactical | MPX",
      "nameKey": "ccrp.gun.tti_mpx.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/tti_mpx.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 36,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:tti_tr1",
      "displayName": "塔兰战术 | TR1 突击步枪",
      "nameZh": "塔兰战术 | TR1 突击步枪",
      "nameEn": "Taran Tactical | TR1",
      "nameKey": "ccrp.gun.tti_tr1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/tti_tr1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:type_192",
      "displayName": "甲胄战术 | QBZ-191突击步枪 轩辕套件",
      "nameZh": "甲胄战术 | QBZ-191突击步枪 轩辕套件",
      "nameEn": "Armor Tactical | QBZ-191 XuanYuan Kit",
      "nameKey": "ccrp.gun.type_192.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/type_192.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:type_95_longbow",
      "displayName": "QJB95-1 轻机枪丨长弓套件",
      "nameZh": "QJB95-1 轻机枪丨长弓套件",
      "nameEn": "QJB95-1 LMG | Longbow Kit",
      "nameKey": "ccrp.gun.type_95_longbow.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/ccrp/index/guns/type_95_longbow.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 60,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:type_97_gen2",
      "displayName": "北方工业 | 第二代 97式突击步枪",
      "nameZh": "北方工业 | 第二代 97式突击步枪",
      "nameEn": "Norinco | Type97 Gen2",
      "nameKey": "ccrp.gun.type_97_gen2.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/type_97_gen2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ccrp:uzi45",
      "displayName": "UZI 冲锋枪丨.45ACP",
      "nameZh": "UZI 冲锋枪丨.45ACP",
      "nameEn": "UZI | .45",
      "nameKey": "ccrp.gun.uzi45.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/uzi45.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 20,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:v308",
      "displayName": "齐玛军械 | Rättvisa V308 战斗步枪",
      "nameZh": "齐玛军械 | Rättvisa V308 战斗步枪",
      "nameEn": "CAM Group | Rättvisa V308",
      "nameKey": "ccrp.gun.v308.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/v308.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:vector10",
      "displayName": "KRISS | 维克托 10mm冲锋枪",
      "nameZh": "KRISS | 维克托 10mm冲锋枪",
      "nameEn": "KRISS | Vector 10mm",
      "nameKey": "ccrp.gun.vector10.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/vector10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:10mm",
      "ammoAmount": 15,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:x95_smg",
      "displayName": "IWI丨X95 SMG",
      "nameZh": "IWI丨X95 SMG",
      "nameEn": "IMI | X95 SMG",
      "nameKey": "ccrp.gun.x95_smg.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/ccrp/index/guns/x95_smg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 32,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:x95r",
      "displayName": "IWI丨X95",
      "nameZh": "IWI丨X95",
      "nameEn": "IMI | X95",
      "nameKey": "ccrp.gun.x95r.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/x95r.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:zenit_ak104",
      "displayName": "泽宁特 | AK104 突击步枪",
      "nameZh": "泽宁特 | AK104 突击步枪",
      "nameEn": "Zenit | AK104",
      "nameKey": "ccrp.gun.zenit_ak104.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/zenit_ak104.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ccrp:zenit_ak105",
      "displayName": "泽宁特 | AK105 突击步枪",
      "nameZh": "泽宁特 | AK105 突击步枪",
      "nameEn": "Zenit | AK105",
      "nameKey": "ccrp.gun.zenit_ak105.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ccrp/index/guns/zenit_ak105.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:686",
      "displayName": "伯莱塔 686 双管霰弹枪",
      "nameZh": "伯莱塔 686 双管霰弹枪",
      "nameEn": "Beretta 686",
      "nameKey": "cib.gun.686.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/686.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "grip",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:881",
      "displayName": "88-2式 突击步枪",
      "nameZh": "88-2式 突击步枪",
      "nameEn": "Type 88-2",
      "nameKey": "cib.gun.881.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/881.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:882",
      "displayName": "88-2式 弹筒突击步枪",
      "nameZh": "88-2式 弹筒突击步枪",
      "nameEn": "Type 88-2(with helical magazine)",
      "nameKey": "cib.gun.882.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/882.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 100,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:9a91",
      "displayName": "9a-91 小型突击步枪",
      "nameZh": "9a-91 小型突击步枪",
      "nameEn": "9a-91",
      "nameKey": "cib.gun.9a91.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/9a91.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:9x39mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:ak103",
      "displayName": "AK103 突击步枪",
      "nameZh": "AK103 突击步枪",
      "nameEn": "AK103",
      "nameKey": "cib.gun.ak103.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/ak103.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:ak105",
      "displayName": "AK105 短管突击步枪",
      "nameZh": "AK105 短管突击步枪",
      "nameEn": "AK105",
      "nameKey": "cib.gun.ak105.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/ak105.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:ak24",
      "displayName": "SAKO AK24 突击步枪",
      "nameZh": "SAKO AK24 突击步枪",
      "nameEn": "SAKO AK24",
      "nameKey": "cib.gun.ak24.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/ak24.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:ar2",
      "displayName": "OSIPR AR2步枪丨半条命2",
      "nameZh": "OSIPR AR2步枪丨半条命2",
      "nameEn": "OSIPR AR2丨HALF-LIFE 2",
      "nameKey": "cib.gun.ar2.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/ar2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:battery",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:asval",
      "displayName": "AS Val 特种突击步枪",
      "nameZh": "AS Val 特种突击步枪",
      "nameEn": "AS Val",
      "nameKey": "cib.gun.asval.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/asval.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:9x39mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:cs_ak47",
      "displayName": "苏联 AK-47 突击步枪",
      "nameZh": "苏联 AK-47 突击步枪",
      "nameEn": "AK47",
      "nameKey": "cib.gun.cs_ak47.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/cs_ak47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:cs_awp",
      "displayName": "精密国际 AWP 狙击步枪",
      "nameZh": "精密国际 AWP 狙击步枪",
      "nameEn": "Accuracy International AWP",
      "nameKey": "cib.gun.cs_awp.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/cs_awp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:cslr3",
      "displayName": "建设工业 CS/LR-3丨QBU-141 狙击步枪",
      "nameZh": "建设工业 CS/LR-3丨QBU-141 狙击步枪",
      "nameEn": "CS/LR-3丨QBU-141",
      "nameKey": "cib.gun.cslr3.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/cslr3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:cslr4",
      "displayName": "建设工业 CS/LR-4 狙击步枪",
      "nameZh": "建设工业 CS/LR-4 狙击步枪",
      "nameEn": "CS/LR-4",
      "nameKey": "cib.gun.cslr4.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/cslr4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:dprkrpg",
      "displayName": "朝鲜火箭榴弹发射器",
      "nameZh": "朝鲜火箭榴弹发射器",
      "nameEn": "DPRK RPG",
      "nameKey": "cib.gun.dprkrpg.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rpg",
      "indexPath": "data/cib/index/guns/dprkrpg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:rpg_rocket",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:dzj08",
      "displayName": "北方工业 DZJ-08 火箭发射器",
      "nameZh": "北方工业 DZJ-08 火箭发射器",
      "nameEn": "DZJ-08",
      "nameKey": "cib.gun.dzj08.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rpg",
      "indexPath": "data/cib/index/guns/dzj08.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:80",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:error",
      "displayName": "cib.gun.error.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "cib.gun.error.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/error.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:error",
      "ammoAmount": 114514,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:evo3",
      "displayName": "蝎式 EVO3 A1 冲锋枪",
      "nameZh": "蝎式 EVO3 A1 冲锋枪",
      "nameEn": "SCORPION EVO 3 A1",
      "nameKey": "cib.gun.evo3.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/evo3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:fal",
      "displayName": "FN FAL 战斗步枪",
      "nameZh": "FN FAL 战斗步枪",
      "nameEn": "FN FAL",
      "nameKey": "cib.gun.fal.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/fal.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:g18c",
      "displayName": "格洛克 18 全自动手枪",
      "nameZh": "格洛克 18 全自动手枪",
      "nameEn": "Glock 18C",
      "nameKey": "cib.gun.g18c.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/g18c.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:g19",
      "displayName": "格洛克 19 手枪",
      "nameZh": "格洛克 19 手枪",
      "nameEn": "Glock 19",
      "nameKey": "cib.gun.g19.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/g19.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:g3sg1",
      "displayName": "G3/SG1 狙击步枪",
      "nameZh": "G3/SG1 狙击步枪",
      "nameEn": "G3/SG1",
      "nameKey": "cib.gun.g3sg1.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/g3sg1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "stock",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:galil",
      "displayName": "加利尔 ARM 突击步枪",
      "nameZh": "加利尔 ARM 突击步枪",
      "nameEn": "IMI Galil ARM",
      "nameKey": "cib.gun.galil.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/galil.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 35,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:galilace",
      "displayName": "加利尔 ACE-22 突击步枪",
      "nameZh": "加利尔 ACE-22 突击步枪",
      "nameEn": "IWI Galil ACE-22",
      "nameKey": "cib.gun.galilace.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/galilace.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 35,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:galilace32",
      "displayName": "加利尔 ACE-32 突击步枪",
      "nameZh": "加利尔 ACE-32 突击步枪",
      "nameEn": "IWI Galil ACE-32",
      "nameKey": "cib.gun.galilace32.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/galilace32.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 35,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:hawk97_1",
      "displayName": "雄鹰集团 97-1 霰弹枪",
      "nameZh": "雄鹰集团 97-1 霰弹枪",
      "nameEn": "HAWK 97-1",
      "nameKey": "cib.gun.hawk97_1.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/hawk97_1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:18.4",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:hawk97_2",
      "displayName": "雄鹰集团 97-2 霰弹枪",
      "nameZh": "雄鹰集团 97-2 霰弹枪",
      "nameEn": "HAWK 97-2",
      "nameKey": "cib.gun.hawk97_2.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/hawk97_2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:18.4",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:hk433",
      "displayName": "HK433 突击步枪",
      "nameZh": "HK433 突击步枪",
      "nameEn": "HK433",
      "nameKey": "cib.gun.hk433.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/hk433.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "cib:js9",
      "displayName": "建设工业 JS-9 冲锋枪",
      "nameZh": "建设工业 JS-9 冲锋枪",
      "nameEn": "JS-9",
      "nameKey": "cib.gun.js9.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/js9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:k2",
      "displayName": "大宇精密 K2 突击步枪",
      "nameZh": "大宇精密 K2 突击步枪",
      "nameEn": "Daewoo K2",
      "nameKey": "cib.gun.k2.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/k2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:k2c1",
      "displayName": "大宇精密 K2C1 突击步枪",
      "nameZh": "大宇精密 K2C1 突击步枪",
      "nameEn": "Daewoo K2C1",
      "nameKey": "cib.gun.k2c1.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/k2c1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:la89",
      "displayName": "LA-89式 突击步枪",
      "nameZh": "LA-89式 突击步枪",
      "nameEn": "LA-89",
      "nameKey": "cib.gun.la89.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/la89.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:m16a4",
      "displayName": "M16A4 自动步枪",
      "nameZh": "M16A4 自动步枪",
      "nameEn": "M16A4",
      "nameKey": "cib.gun.m16a4.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/m16a4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:m4",
      "displayName": "M4 URGI 突击步枪",
      "nameZh": "M4 URGI 突击步枪",
      "nameEn": "M4 URGI",
      "nameKey": "cib.gun.m4.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/m4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:m99",
      "displayName": "资江机器 M99 反器材半自动狙击步枪",
      "nameZh": "资江机器 M99 反器材半自动狙击步枪",
      "nameEn": "ZiJiang M99",
      "nameKey": "cib.gun.m99.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/m99.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:127x108",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:mg3",
      "displayName": "MG3 通用机枪",
      "nameZh": "MG3 通用机枪",
      "nameEn": "MG3",
      "nameKey": "cib.gun.mg3.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/mg3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 65,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:mini",
      "displayName": "M134 迷你炮机枪",
      "nameZh": "M134 迷你炮机枪",
      "nameEn": "M134 Minigun",
      "nameKey": "cib.gun.mini.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/mini.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 300,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:mk18",
      "displayName": "MK18 MOD 1 突击步枪",
      "nameZh": "MK18 MOD 1 突击步枪",
      "nameEn": "MK18 MOD 1",
      "nameKey": "cib.gun.mk18.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/mk18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:negev",
      "displayName": "内格夫 NG5 轻机枪",
      "nameZh": "内格夫 NG5 轻机枪",
      "nameEn": "IWI NEGEV NG-5",
      "nameKey": "cib.gun.negev.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/negev.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 65,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:nova",
      "displayName": "伯奈利 NOVA “新星” 霰弹枪",
      "nameZh": "伯奈利 NOVA “新星” 霰弹枪",
      "nameEn": "Benelli NOVA",
      "nameKey": "cib.gun.nova.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/nova.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:origin12",
      "displayName": "Origin-12 半自动霰弹枪",
      "nameZh": "Origin-12 半自动霰弹枪",
      "nameEn": "Origin-12",
      "nameKey": "cib.gun.origin12.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/origin12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "stock",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {
        "tac:8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:origin12db",
      "displayName": "Origin-12 半自动霰弹枪（60发全自动龙息弹）",
      "nameZh": "Origin-12 半自动霰弹枪（60发全自动龙息弹）",
      "nameEn": "Origin-12（60rds Drangon's Breath ammo）",
      "nameKey": "cib.gun.origin12db.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/origin12db.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 60,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {
        "tac:8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:ots14",
      "displayName": "OTs-14-1A “闪电-1” 突击步枪",
      "nameZh": "OTs-14-1A “闪电-1” 突击步枪",
      "nameEn": "OTs-14-1A “Groza-1”",
      "nameKey": "cib.gun.ots14.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/ots14.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:p250",
      "displayName": "西格绍尔 SIG-P250 手枪",
      "nameZh": "西格绍尔 SIG-P250 手枪",
      "nameEn": "SIG-P250",
      "nameKey": "cib.gun.p250.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/p250.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 13,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:pkp",
      "displayName": "PKP “佩切涅格” 机枪",
      "nameZh": "PKP “佩切涅格” 机枪",
      "nameEn": "“Pecheneg” PKP",
      "nameKey": "cib.gun.pkp.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/pkp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 70,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:pm9",
      "displayName": "美蓓亚 PM-9 冲锋枪",
      "nameZh": "美蓓亚 PM-9 冲锋枪",
      "nameEn": "Minebea PM-9",
      "nameKey": "cib.gun.pm9.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/pm9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 25,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:pp19",
      "displayName": "PP-19 “野牛” 冲锋枪",
      "nameZh": "PP-19 “野牛” 冲锋枪",
      "nameEn": "PP-19 “Bizon”",
      "nameKey": "cib.gun.pp19.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/pp19.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:ppk",
      "displayName": "瓦尔特 PPK 手枪",
      "nameZh": "瓦尔特 PPK 手枪",
      "nameEn": "Walter PPK",
      "nameKey": "cib.gun.ppk.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/ppk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:32acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:ppsh41",
      "displayName": "PPSh41 “波波沙冲锋枪”",
      "nameZh": "PPSh41 “波波沙冲锋枪”",
      "nameEn": "PPSh41 Submachine Gun",
      "nameKey": "cib.gun.ppsh41.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/ppsh41.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qba221",
      "displayName": "建设工业 QBA-221 自动霰弹枪",
      "nameZh": "建设工业 QBA-221 自动霰弹枪",
      "nameEn": "QBA-221",
      "nameKey": "cib.gun.qba221.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/qba221.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:18.4",
      "ammoAmount": 5,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "stock",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {
        "tac:8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:qba221_burst",
      "displayName": "建设工业 QBA-221 自动霰弹枪（60发爆炸独头弹）",
      "nameZh": "建设工业 QBA-221 自动霰弹枪（60发爆炸独头弹）",
      "nameEn": "QBA-221（60 rounds of explosive slug ammunition）",
      "nameKey": "cib.gun.qba221_burst.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/qba221_burst.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:18.4",
      "ammoAmount": 60,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {
        "tac:8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:qbs09",
      "displayName": "北方工业 QBS-09 战斗霰弹枪",
      "nameZh": "北方工业 QBS-09 战斗霰弹枪",
      "nameEn": "QBS-09",
      "nameKey": "cib.gun.qbs09.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/qbs09.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:18.4",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbu10",
      "displayName": "北方工业 QBU-10 反器材半自动狙击步枪",
      "nameZh": "北方工业 QBU-10 反器材半自动狙击步枪",
      "nameEn": "QBU-10",
      "nameKey": "cib.gun.qbu10.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/qbu10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:127x108",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbu191",
      "displayName": "建设工业 QBU-191 精确射手步枪",
      "nameZh": "建设工业 QBU-191 精确射手步枪",
      "nameEn": "QBU-191",
      "nameKey": "cib.gun.qbu191.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbu191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbu201",
      "displayName": "建设工业 QBU-201 反器材狙击步枪",
      "nameZh": "建设工业 QBU-201 反器材狙击步枪",
      "nameEn": "QBU-201",
      "nameKey": "cib.gun.qbu201.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/qbu201.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:127x108",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbu202",
      "displayName": "建设工业 QBU-202 狙击步枪",
      "nameZh": "建设工业 QBU-202 狙击步枪",
      "nameEn": "QBU-202",
      "nameKey": "cib.gun.qbu202.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/qbu202.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbu203",
      "displayName": "建设工业 QBU-203 狙击步枪",
      "nameZh": "建设工业 QBU-203 狙击步枪",
      "nameEn": "QBU-203",
      "nameKey": "cib.gun.qbu203.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/qbu203.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbu88",
      "displayName": "北方工业 QBU-88 精确射手步枪",
      "nameZh": "北方工业 QBU-88 精确射手步枪",
      "nameEn": "QBU-88",
      "nameKey": "cib.gun.qbu88.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbu88.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbz03",
      "displayName": "北方工业 QBZ-03 自动步枪",
      "nameZh": "北方工业 QBZ-03 自动步枪",
      "nameEn": "QBZ-03",
      "nameKey": "cib.gun.qbz03.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbz03.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:qbz191",
      "displayName": "建设工业 QBZ-191 突击步枪",
      "nameZh": "建设工业 QBZ-191 突击步枪",
      "nameEn": "QBZ-191",
      "nameKey": "cib.gun.qbz191.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbz191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbz192",
      "displayName": "建设工业 QBZ-192 短管突击步枪",
      "nameZh": "建设工业 QBZ-192 短管突击步枪",
      "nameEn": "QBZ-192",
      "nameKey": "cib.gun.qbz192.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbz192.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbz951",
      "displayName": "北方工业 QBZ-95-1 自动步枪",
      "nameZh": "北方工业 QBZ-95-1 自动步枪",
      "nameEn": "QBZ 95-1",
      "nameKey": "cib.gun.qbz951.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbz951.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qbz95b1",
      "displayName": "北方工业 QBZ-95B-1 短管自动步枪",
      "nameZh": "北方工业 QBZ-95B-1 短管自动步枪",
      "nameEn": "QBZ-95B-1",
      "nameKey": "cib.gun.qbz95b1.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/qbz95b1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qcq171",
      "displayName": "建设工业 QCQ-171 冲锋枪",
      "nameZh": "建设工业 QCQ-171 冲锋枪",
      "nameEn": "QCQ-171",
      "nameKey": "cib.gun.qcq171.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/qcq171.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:qcw05",
      "displayName": "建设工业 QCW-05 冲锋枪",
      "nameZh": "建设工业 QCW-05 冲锋枪",
      "nameEn": "QCW-05",
      "nameKey": "cib.gun.qcw05.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/qcw05.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:58x21",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qjb201",
      "displayName": "建设工业 QJB-201 班用机枪",
      "nameZh": "建设工业 QJB-201 班用机枪",
      "nameEn": "QJB 201",
      "nameKey": "cib.gun.qjb201.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/qjb201.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qjb951",
      "displayName": "北方工业 QJB-95-1 班用机枪",
      "nameZh": "北方工业 QJB-95-1 班用机枪",
      "nameEn": "QJB 95-1",
      "nameKey": "cib.gun.qjb951.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/qjb951.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 75,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qjy201",
      "displayName": "建设工业 QJY-201 通用机枪",
      "nameZh": "建设工业 QJY-201 通用机枪",
      "nameEn": "QJY-201",
      "nameKey": "cib.gun.qjy201.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/qjy201.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qjy88",
      "displayName": "北方工业 QJY-88 通用机枪",
      "nameZh": "北方工业 QJY-88 通用机枪",
      "nameEn": "QJY-88",
      "nameKey": "cib.gun.qjy88.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/qjy88.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 90,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qjz171",
      "displayName": "建设工业 QJZ-171 重机枪",
      "nameZh": "建设工业 QJZ-171 重机枪",
      "nameEn": "QJZ-171",
      "nameKey": "cib.gun.qjz171.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/qjz171.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:127x108",
      "ammoAmount": 60,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qlu11",
      "displayName": "资江机器 QLU-11丨QLU-131 狙击榴弹发射器",
      "nameZh": "资江机器 QLU-11丨QLU-131 狙击榴弹发射器",
      "nameEn": "QLU-11/QLU-131",
      "nameKey": "cib.gun.qlu11.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rpg",
      "indexPath": "data/cib/index/guns/qlu11.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:35mm",
      "ammoAmount": 3,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qsz193",
      "displayName": "建设工业 QSZ-193 手枪",
      "nameZh": "建设工业 QSZ-193 手枪",
      "nameEn": "QSZ-193",
      "nameKey": "cib.gun.qsz193.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/qsz193.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:qsz92",
      "displayName": "北方工业 QSZ-92 手枪",
      "nameZh": "北方工业 QSZ-92 手枪",
      "nameEn": "QSZ 92",
      "nameKey": "cib.gun.qsz92.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/qsz92.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:58x21",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:r8",
      "displayName": "M327-TRR8 转轮手枪",
      "nameZh": "M327-TRR8 转轮手枪",
      "nameEn": "M327-TRR8 revolver",
      "nameKey": "cib.gun.r8.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/r8.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:sig552",
      "displayName": "西格绍尔 SIG-552 短管突击步枪",
      "nameZh": "西格绍尔 SIG-552 短管突击步枪",
      "nameEn": "SIG-552",
      "nameKey": "cib.gun.sig552.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/sig552.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:sig556",
      "displayName": "西格绍尔 SIG-556 突击步枪",
      "nameZh": "西格绍尔 SIG-556 突击步枪",
      "nameEn": "SIG-556",
      "nameKey": "cib.gun.sig556.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/sig556.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:ssg08",
      "displayName": "斯太尔 SSG-08 狙击枪",
      "nameZh": "斯太尔 SSG-08 狙击枪",
      "nameEn": "SSG-08",
      "nameKey": "cib.gun.ssg08.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/ssg08.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:sv98",
      "displayName": "SV98 狙击步枪",
      "nameZh": "SV98 狙击步枪",
      "nameEn": "SV98",
      "nameKey": "cib.gun.sv98.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/sv98.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:svd",
      "displayName": "SVD “德拉戈诺夫” 半自动狙击步枪",
      "nameZh": "SVD “德拉戈诺夫” 半自动狙击步枪",
      "nameEn": "“Dragunov” SVD",
      "nameKey": "cib.gun.svd.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/svd.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:t91",
      "displayName": "T91 突击步枪",
      "nameZh": "T91 突击步枪",
      "nameEn": "T91",
      "nameKey": "cib.gun.t91.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/t91.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:type11",
      "displayName": "大正十一年式 轻机枪",
      "nameZh": "大正十一年式 轻机枪",
      "nameEn": "Type 11 Light Machine Gun",
      "nameKey": "cib.gun.type11.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/type11.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:65x50",
      "ammoAmount": 30,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:type14",
      "displayName": "南部十四手枪",
      "nameZh": "南部十四手枪",
      "nameEn": "Type 14",
      "nameKey": "cib.gun.type14.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/type14.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:8x22",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:type20",
      "displayName": "丰和工业 20式 突击步枪",
      "nameZh": "丰和工业 20式 突击步枪",
      "nameEn": "Howa Type 20",
      "nameKey": "cib.gun.type20.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/type20.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "cib:type38",
      "displayName": "有坂 三八式步枪",
      "nameZh": "有坂 三八式步枪",
      "nameEn": "Type 38 Rifle",
      "nameKey": "cib.gun.type38.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cib/index/guns/type38.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:65x50",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:type56",
      "displayName": "北方工业 56-1式 自动步枪",
      "nameZh": "北方工业 56-1式 自动步枪",
      "nameEn": "Type 56-1",
      "nameKey": "cib.gun.type56.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cib/index/guns/type56.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:type73",
      "displayName": "朝鲜 73式 机枪",
      "nameZh": "朝鲜 73式 机枪",
      "nameEn": "Type 73",
      "nameKey": "cib.gun.type73.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cib/index/guns/type73.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 30,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:type79",
      "displayName": "建设工业 79式 冲锋枪",
      "nameZh": "建设工业 79式 冲锋枪",
      "nameEn": "Type 79",
      "nameKey": "cib.gun.type79.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cib/index/guns/type79.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:usas12",
      "displayName": "吉尔伯特/大宇精密 USAS-12 自动霰弹枪",
      "nameZh": "吉尔伯特/大宇精密 USAS-12 自动霰弹枪",
      "nameEn": "Gilbert/Daewoo USAS-12 Automatic shotgun",
      "nameKey": "cib.gun.usas12.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/usas12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 10,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "stock",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {
        "tac:8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cib:usp",
      "displayName": "HK-USP .45ACP 战术型手枪",
      "nameZh": "HK-USP .45ACP 战术型手枪",
      "nameEn": "HK-USP .45ACP TACTICAL",
      "nameKey": "cib.gun.usp.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cib/index/guns/usp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cib:widow",
      "displayName": "寡妇制造者丨军团要塞2",
      "nameZh": "寡妇制造者丨军团要塞2",
      "nameEn": "Widowmaker丨Team Fortress 2",
      "nameKey": "cib.gun.widow.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cib/index/guns/widow.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tac:8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cibs:ak103_laffey",
      "displayName": "AK103 突击步枪丨碧蓝航线 联名(拉菲)",
      "nameZh": "AK103 突击步枪丨碧蓝航线 联名(拉菲)",
      "nameEn": "AK103丨Azur Lane (laffey)",
      "nameKey": "cibs.gun.ak103_laffey.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/ak103_laffey.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cibs:ak105_kaltsit",
      "displayName": "AK105 短管突击步枪丨明日方舟 联名(凯尔希)",
      "nameZh": "AK105 短管突击步枪丨明日方舟 联名(凯尔希)",
      "nameEn": "AK105丨Arknights (Kal'tsit)",
      "nameKey": "cibs.gun.ak105_kaltsit.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/ak105_kaltsit.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cibs:ak24_texas",
      "displayName": "SAKO AK24 突击步枪丨明日方舟 联名(德克萨斯)",
      "nameZh": "SAKO AK24 突击步枪丨明日方舟 联名(德克萨斯)",
      "nameEn": "SAKO AK24丨Arknights (Texas)",
      "nameKey": "cibs.gun.ak24_texas.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/ak24_texas.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:awp_hm",
      "displayName": "精密国际 AWP 狙击步枪丨绝区零 联名(星见雅)",
      "nameZh": "精密国际 AWP 狙击步枪丨绝区零 联名(星见雅)",
      "nameEn": "Accuracy International AWP丨ZZZ(Hoshimi Miyabi)",
      "nameKey": "cibs.gun.awp_hm.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cibs/index/guns/awp_hm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:galilace_lesh",
      "displayName": "加利尔 ACE-32 突击步枪丨LESHUAN_20 乐拴",
      "nameZh": "加利尔 ACE-32 突击步枪丨LESHUAN_20 乐拴",
      "nameEn": "Galil ACE-32丨LESHUAN_20",
      "nameKey": "cibs.gun.galilace_lesh.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/galilace_lesh.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 35,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:hawk97_2_gold",
      "displayName": "雄鹰集团 97-2 霰弹枪丨乌金烈芒",
      "nameZh": "雄鹰集团 97-2 霰弹枪丨乌金烈芒",
      "nameEn": "HAWK 97-2丨Black Steel Gold",
      "nameKey": "cibs.gun.hawk97_2_gold.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/cibs/index/guns/hawk97_2_gold.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cib:18.4",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:m4_koei",
      "displayName": "M4 URGI 突击步枪丨笨笨小狼",
      "nameZh": "M4 URGI 突击步枪丨笨笨小狼",
      "nameEn": "M4 URGI | Clumsy Little Wolf",
      "nameKey": "cibs.gun.m4_koei.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/m4_koei.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:mk18_jianjiu",
      "displayName": "MK18 MOD 1 突击步枪丨JIANJIU 建九",
      "nameZh": "MK18 MOD 1 突击步枪丨JIANJIU 建九",
      "nameEn": "MK18 MOD 1丨JIANJIU",
      "nameKey": "cibs.gun.mk18_jianjiu.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/mk18_jianjiu.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:qbu202_lingche",
      "displayName": "建设工业 QBU-202 狙击枪丨LINGCHE 凌彻",
      "nameZh": "建设工业 QBU-202 狙击枪丨LINGCHE 凌彻",
      "nameEn": "QBU-202丨LINGCHE",
      "nameKey": "cibs.gun.qbu202_lingche.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cibs/index/guns/qbu202_lingche.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:qbz191_warrior",
      "displayName": "建设工业 QBZ-191 突击步枪丨最优质的战士",
      "nameZh": "建设工业 QBZ-191 突击步枪丨最优质的战士",
      "nameEn": "QBZ-191丨BF4 High-Caliber Soldier",
      "nameKey": "cibs.gun.qbz191_warrior.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/qbz191_warrior.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:qbz951_asiimov",
      "displayName": "北方工业 QBZ-95-1 自动步枪丨二西莫夫",
      "nameZh": "北方工业 QBZ-95-1 自动步枪丨二西莫夫",
      "nameEn": "QBZ-95-1丨ASIIMOV",
      "nameKey": "cibs.gun.qbz951_asiimov.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/qbz951_asiimov.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:qcq171_ocean",
      "displayName": "建设工业 QCQ-171 冲锋枪丨蔚蓝之境",
      "nameZh": "建设工业 QCQ-171 冲锋枪丨蔚蓝之境",
      "nameEn": "QCQ-171丨Azure Realm",
      "nameKey": "cibs.gun.qcq171_ocean.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "smg",
      "indexPath": "data/cibs/index/guns/qcq171_ocean.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cibs:qjb201_sf2403",
      "displayName": "建设工业 QJB-201 班用机枪丨SF-2403",
      "nameZh": "建设工业 QJB-201 班用机枪丨SF-2403",
      "nameEn": "QJB-201丨SF-2403",
      "nameKey": "cibs.gun.qjb201_sf2403.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "mg",
      "indexPath": "data/cibs/index/guns/qjb201_sf2403.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "stock",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:sig556_shiroko",
      "displayName": "西格绍尔 SIG-556 突击步枪丨蔚蓝档案 联名(砂狼白子)",
      "nameZh": "西格绍尔 SIG-556 突击步枪丨蔚蓝档案 联名(砂狼白子)",
      "nameEn": "SIG-556丨Blue Archive (Sunaookami Shiroko)",
      "nameKey": "cibs.gun.sig556_shiroko.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/sig556_shiroko.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cibs:type20_hibiki",
      "displayName": "丰和工业 20式 突击步枪丨舰队Collection 联名(响)",
      "nameZh": "丰和工业 20式 突击步枪丨舰队Collection 联名(响)",
      "nameEn": "Howa Type 20丨kan Collection (HIBIKI)",
      "nameKey": "cibs.gun.type20_hibiki.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/type20_hibiki.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "cibs:type56_commemorate",
      "displayName": "北方工业 56-1式 自动步枪丨镀铬",
      "nameZh": "北方工业 56-1式 自动步枪丨镀铬",
      "nameEn": "Type 56-1丨Chrome plated",
      "nameKey": "cibs.gun.type56_commemorate.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cibs/index/guns/type56_commemorate.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "cibs:usps",
      "displayName": "HK-USP .45ACP 战术型手枪丨不锈钢",
      "nameZh": "HK-USP .45ACP 战术型手枪丨不锈钢",
      "nameEn": "HK-USP .45ACP TACTICAL丨Stainless",
      "nameKey": "cibs.gun.usps.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cibs/index/guns/usps.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:aa410",
      "displayName": "AA-410 突击霰弹枪",
      "nameZh": "AA-410 突击霰弹枪",
      "nameEn": "AA-410",
      "nameKey": "classicr.gun.aa410.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/classicr/index/guns/aa410.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:410_bore",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "burst",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "laser",
        "grip",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:ak12",
      "displayName": "AK12 突击步枪",
      "nameZh": "AK12 突击步枪",
      "nameEn": "AK12",
      "nameKey": "classicr.gun.ak12.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/ak12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:b93r",
      "displayName": "伯莱塔 M93 Raffica",
      "nameZh": "伯莱塔 M93 Raffica",
      "nameEn": "Berretta 93R Machine Pistol",
      "nameKey": "classicr.gun.b93r.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/b93r.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 15,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:colt_python",
      "displayName": "柯尔特 蟒蛇",
      "nameZh": "柯尔特 蟒蛇",
      "nameEn": "Colt Python .357Magnum",
      "nameKey": "classicr.gun.colt_python.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/colt_python.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:dp28",
      "displayName": "捷格加廖夫 | DP28",
      "nameZh": "捷格加廖夫 | DP28",
      "nameEn": "Degtyaryova | DP28",
      "nameKey": "classicr.gun.dp28.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/classicr/index/guns/dp28.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54r",
      "ammoAmount": 47,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "classicr:fn_fal_tac",
      "displayName": "DSA SA58",
      "nameZh": "DSA SA58",
      "nameEn": "DSA SA58",
      "nameKey": "classicr.gun.fn_fal_tac.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/fn_fal_tac.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "laser",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "classicr:glock_18",
      "displayName": "格洛克 18c 冲锋手枪",
      "nameZh": "格洛克 18c 冲锋手枪",
      "nameEn": "Glock 18c",
      "nameKey": "classicr.gun.glock_18.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/glock_18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:hk416_a5",
      "displayName": "HK416 A5 突击步枪",
      "nameZh": "HK416 A5 突击步枪",
      "nameEn": "HK416 A5",
      "nameKey": "classicr.gun.hk416_a5.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/hk416_a5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:kar98",
      "displayName": "Kar 98",
      "nameZh": "Kar 98",
      "nameEn": "Kar 98",
      "nameKey": "classicr.gun.kar98.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/kar98.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:m1a1_smg",
      "displayName": "汤姆逊 M1A1 冲锋枪",
      "nameZh": "汤姆逊 M1A1 冲锋枪",
      "nameEn": "Thompson M1A1",
      "nameKey": "classicr.gun.m1a1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/classicr/index/guns/m1a1_smg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 30,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:m24_renewed",
      "displayName": "M24 狙击步枪",
      "nameZh": "M24 狙击步枪",
      "nameEn": "M24 Sniper Rifle",
      "nameKey": "classicr.gun.m24_renewed.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/m24_renewed.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:m60",
      "displayName": "M60 通用机枪",
      "nameZh": "M60 通用机枪",
      "nameEn": "M60 GPMG",
      "nameKey": "classicr.gun.m60.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/classicr/index/guns/m60.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:m82a2",
      "displayName": "巴雷特 M82A1",
      "nameZh": "巴雷特 M82A1",
      "nameEn": "Barrett M82A1",
      "nameKey": "classicr.gun.m82a2.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/m82a2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "classicr:m92fs",
      "displayName": "伯莱塔 M92FS",
      "nameZh": "伯莱塔 M92FS",
      "nameEn": "Berretta M92FS",
      "nameKey": "classicr.gun.m92fs.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/m92fs.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mac10",
      "displayName": "英格拉姆 M10 冲锋枪",
      "nameZh": "英格拉姆 M10 冲锋枪",
      "nameEn": "Ingram M10 SMG",
      "nameKey": "classicr.gun.mac10.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/mac10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mauser_c96",
      "displayName": "毛瑟 | C96",
      "nameZh": "毛瑟 | C96",
      "nameEn": "Mauser | C96 Schnellfeuer",
      "nameKey": "classicr.gun.mauser_c96.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/mauser_c96.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:763mauser",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mgl_40mm",
      "displayName": "MGL 榴弹发射器",
      "nameZh": "MGL 榴弹发射器",
      "nameEn": "MGL 40mm Grenade Launcher",
      "nameKey": "classicr.gun.mgl_40mm.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rpg",
      "indexPath": "data/classicr/index/guns/mgl_40mm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "grip",
        "stock",
        "scope",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:minigun",
      "displayName": "通用电气 | M134 转管机枪",
      "nameZh": "通用电气 | M134 转管机枪",
      "nameEn": "General Electric | M134 MINIGUN",
      "nameKey": "classicr.gun.minigun.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "mg",
      "indexPath": "data/classicr/index/guns/minigun.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 500,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mk18_mod1",
      "displayName": "MK18 Mod1 突击步枪",
      "nameZh": "MK18 Mod1 突击步枪",
      "nameEn": "MK18 Mod1",
      "nameKey": "classicr.gun.mk18_mod1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/mk18_mod1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mk47",
      "displayName": "MK47 突击步枪",
      "nameZh": "MK47 突击步枪",
      "nameEn": "MK47 Assault Rifle",
      "nameKey": "classicr.gun.mk47.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/mk47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39copper",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "stock",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "classicr:mp7",
      "displayName": "HK MP7 冲锋枪",
      "nameZh": "HK MP7 冲锋枪",
      "nameEn": "HK MP7 SMG",
      "nameKey": "classicr.gun.mp7.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/classicr/index/guns/mp7.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:46x30",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mp9",
      "displayName": "MP9 冲锋枪",
      "nameZh": "MP9 冲锋枪",
      "nameEn": "MP9 SMG",
      "nameKey": "classicr.gun.mp9.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/classicr/index/guns/mp9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mrad",
      "displayName": "巴雷特 | MRAD",
      "nameZh": "巴雷特 | MRAD",
      "nameEn": "Barrett | MRAD",
      "nameKey": "classicr.gun.mrad_white.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/mrad.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:mrad_elr",
      "displayName": "巴雷特 | MRAD ELR",
      "nameZh": "巴雷特 | MRAD ELR",
      "nameEn": "Barrett MRAD ELR",
      "nameKey": "classicr.gun.mrad.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/mrad_elr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:416barrett",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:msr",
      "displayName": "雷明顿 | MSR 狙击步枪",
      "nameZh": "雷明顿 | MSR 狙击步枪",
      "nameEn": "Remington | MSR Sniper Rifle",
      "nameKey": "classicr.gun.msr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/msr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:ngsw_r_renewed",
      "displayName": "M7 战斗步枪",
      "nameZh": "M7 战斗步枪",
      "nameEn": "M7 Battle Rifle",
      "nameKey": "classicr.gun.ngsw_r_renewed.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/ngsw_r_renewed.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:68x51fury",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:qbz191_renewed",
      "displayName": "QBZ-192 突击步枪",
      "nameZh": "QBZ-192 突击步枪",
      "nameEn": "QBZ-192 Assault Rifle",
      "nameKey": "classicr.gun.qbz191_renewed.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/qbz191_renewed.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "stock",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "classicr:scar_mk20",
      "displayName": "Scar MK20 狙击支援步枪",
      "nameZh": "Scar MK20 狙击支援步枪",
      "nameEn": "MK20 SSR",
      "nameKey": "classicr.gun.scar_mk20.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "sniper",
      "indexPath": "data/classicr/index/guns/scar_mk20.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "laser",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "classicr:spr_15",
      "displayName": "射手座 SPR-15 特种用途步枪",
      "nameZh": "射手座 SPR-15 特种用途步枪",
      "nameEn": "SPR 5.56",
      "nameKey": "classicr.gun.spr_15.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "rifle",
      "indexPath": "data/classicr/index/guns/spr_15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "extended_mag",
        "stock",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "classicr:sti2011",
      "displayName": "STI 2011",
      "nameZh": "STI 2011",
      "nameEn": "STI 2011",
      "nameKey": "classicr.gun.sti2011.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/sti2011.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle",
        "scope",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:tec_9",
      "displayName": "英特拉泰克 Tec-9",
      "nameZh": "英特拉泰克 Tec-9",
      "nameEn": "Intratec Tec-9",
      "nameKey": "classicr.gun.tec_9.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/tec_9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag",
        "stock",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:tti_g34",
      "displayName": "塔兰战术 TTI G34",
      "nameZh": "塔兰战术 TTI G34",
      "nameEn": "TTI G34",
      "nameKey": "classicr.gun.tti_g34.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "pistol",
      "indexPath": "data/classicr/index/guns/tti_g34.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 19,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "classicr:udp_9",
      "displayName": "UDP-9 个人防卫武器",
      "nameZh": "UDP-9 个人防卫武器",
      "nameEn": "UDP-9 PDW",
      "nameKey": "classicr.gun.udp_9.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "smg",
      "indexPath": "data/classicr/index/guns/udp_9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 22,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:ar30",
      "displayName": "阿玛莱特 AR-30 狙击步枪",
      "nameZh": "阿玛莱特 AR-30 狙击步枪",
      "nameEn": "ArmaLite AR-30 Sinper Rifle",
      "nameKey": "cpse.gun.ar30.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/ar30.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:ar50",
      "displayName": "阿玛莱特 AR-50 .50口径反器材步枪",
      "nameZh": "阿玛莱特 AR-50 .50口径反器材步枪",
      "nameEn": "ArmaLite AR-50 Sinper Rifle",
      "nameKey": "cpse.gun.ar50.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/ar50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:cdx33",
      "displayName": "Cadex防务 CDX-33 Lite 狙击步枪",
      "nameZh": "Cadex防务 CDX-33 Lite 狙击步枪",
      "nameEn": "Cadex Defence CDX-33 Lite Sniper Rifle",
      "nameKey": "cpse.gun.cdx33.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/cdx33.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "laser",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:cdx40",
      "displayName": "Cadex防务 CDX-40 Shadow 狙击步枪",
      "nameZh": "Cadex防务 CDX-40 Shadow 狙击步枪",
      "nameEn": "Cadex Defence CDX-40 Shadow Sniper Rifle",
      "nameKey": "cpse.gun.cdx40.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/cdx40.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cpse:375ct",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "laser",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:cdx50",
      "displayName": "Cadex防务 CDX-50 Tremor 反器材狙击步枪",
      "nameZh": "Cadex防务 CDX-50 Tremor 反器材狙击步枪",
      "nameEn": "Cadex Defence CDX-50 Tremor Sniper Rifle",
      "nameKey": "cpse.gun.cdx_50.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/cdx50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "laser",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:cdx_r7_cps",
      "displayName": "Cadex防务 CDX-R7 CPS 战术步枪",
      "nameZh": "Cadex防务 CDX-R7 CPS 战术步枪",
      "nameEn": "Cadex Defence CDX-R7 CPS Sniper Rifle",
      "nameKey": "cpse.gun.cdx_r7_cps.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/cdx_r7_cps.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cpse:65cm",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "laser",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:g43x",
      "displayName": "格洛克 43X 手枪",
      "nameZh": "格洛克 43X 手枪",
      "nameEn": "Glock 43X",
      "nameKey": "cpse.gun.g43x.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/cpse/index/guns/g43x.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:m110_sass",
      "displayName": "M110 半自动狙击手系统",
      "nameZh": "M110 半自动狙击手系统",
      "nameEn": "M110 SASS",
      "nameKey": "cpse.gun.m110_sass.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cpse/index/guns/m110_sass.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:mk13",
      "displayName": "MK13 Mod 0 狙击步枪",
      "nameZh": "MK13 Mod 0 狙击步枪",
      "nameEn": "MK13 Mod 0 Sinper Rifle",
      "nameKey": "cpse.gun.mk13.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/mk13.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "cpse:300wm",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "stock",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:qbu97",
      "displayName": "北方工业 QBU-97 狙击步枪",
      "nameZh": "北方工业 QBU-97 狙击步枪",
      "nameEn": "QBU 97",
      "nameKey": "cpse.gun.qbu97.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cpse/index/guns/qbu97.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:svch86",
      "displayName": "Svch 8.6mm 半自动狙击步枪",
      "nameZh": "Svch 8.6mm 半自动狙击步枪",
      "nameEn": "Svch 8.6mm Semi Sinper Rifle",
      "nameKey": "cpse.gun.svch86.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/svch86.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:svdm",
      "displayName": "SVD M 精确射手步枪",
      "nameZh": "SVD M 精确射手步枪",
      "nameEn": "SVD M DMR",
      "nameKey": "cpse.gun.svdm.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/cpse/index/guns/svdm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "cpse:up10",
      "displayName": "尤因塔精密 UP-10 栓动步枪",
      "nameZh": "尤因塔精密 UP-10 栓动步枪",
      "nameEn": "Uintah Precision UP10 Sinper Rifle",
      "nameKey": "cpse.gun.up10.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/cpse/index/guns/up10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "k16:k16",
      "displayName": "K-16",
      "nameZh": "",
      "nameEn": "K-16",
      "nameKey": "k16.gun.k16.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/k16/index/guns/k16.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "kpp:870magpul",
      "displayName": "M87A1",
      "nameZh": "M87A1",
      "nameEn": "M87A1",
      "nameKey": "kpp.gun.870magpul.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/kpp/index/guns/870magpul.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "grip",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "kpp:870magpul_1",
      "displayName": "马盖普 M870",
      "nameZh": "马盖普 M870",
      "nameEn": "Magpul M870",
      "nameKey": "kpp.gun.870magpul_1.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/kpp/index/guns/870magpul_1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "grip",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "kpp:k1a",
      "displayName": "K1A",
      "nameZh": "K1A",
      "nameEn": "K1A",
      "nameKey": "kpp.gun.k1a.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "rifle",
      "indexPath": "data/kpp/index/guns/k1a.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "kpp:k7",
      "displayName": "K7 微声冲锋枪",
      "nameZh": "K7 微声冲锋枪",
      "nameEn": "K7 SMG",
      "nameKey": "kpp.gun.k7.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "smg",
      "indexPath": "data/kpp/index/guns/k7.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "kpp:kp31",
      "displayName": "索米KP-31",
      "nameZh": "索米KP-31",
      "nameEn": "Suomi KP-31",
      "nameKey": "kpp.gun.kp31.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "smg",
      "indexPath": "data/kpp/index/guns/kp31.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "kpp:m870_t",
      "displayName": "M870",
      "nameZh": "M870",
      "nameEn": "M870",
      "nameKey": "kpp.gun.m870_t.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/kpp/index/guns/m870_t.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "stock",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "m72_law:m72_law",
      "displayName": "M72火箭筒",
      "nameZh": "M72火箭筒",
      "nameEn": "M72 Light Anti-armor Weapon",
      "nameKey": "m72_law.gun.m72_law.name",
      "source": "tacz/m72_law_converted.zip",
      "sources": [
        "tacz/m72_law_converted.zip"
      ],
      "type": "rpg",
      "indexPath": "data/m72_law/index/guns/m72_law.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:rpg_rocket",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:ak224",
      "displayName": "AK-224 DMR",
      "nameZh": "",
      "nameEn": "AK-224 DMR",
      "nameKey": "ra1k.gun.ak224.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ra1k/index/guns/ak224.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ra1k:224valkyrie",
      "ammoAmount": 10,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:ak50",
      "displayName": "AK-50",
      "nameZh": "",
      "nameEn": "AK-50",
      "nameKey": "ra1k.gun.ak50.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ra1k/index/guns/ak50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:ak50_lite",
      "displayName": "AK-50 lite",
      "nameZh": "",
      "nameEn": "AK-50 lite",
      "nameKey": "ra1k.gun.ak50lite.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/ak50_lite.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:50beowulf",
      "ammoAmount": 7,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:ak57",
      "displayName": "AK-57 SMG",
      "nameZh": "",
      "nameEn": "AK-57 SMG",
      "nameKey": "ra1k.gun.ak57.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/ra1k/index/guns/ak57.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:ak74",
      "displayName": "AK-74",
      "nameZh": "",
      "nameEn": "AK-74",
      "nameKey": "ra1k.gun.ak74.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/ak74.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "damage_addend": 100,
          "ads_addend": 100.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:ak74_gp25",
      "displayName": "AK-74 with GP-25",
      "nameZh": "",
      "nameEn": "AK-74 with GP-25",
      "nameKey": "ra1k.gun.ak74_gp25.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/ra1k/index/guns/ak74_gp25.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ra1k:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "damage_addend": 100,
          "ads_addend": 100.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:aks74u",
      "displayName": "AKS-74u",
      "nameZh": "",
      "nameEn": "AKS-74u",
      "nameKey": "ra1k.gun.aksu.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/aks74u.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:asval_mod4",
      "displayName": "AsVal Mod4",
      "nameZh": "",
      "nameEn": "AsVal Mod4",
      "nameKey": "ra1k.gun.mod4.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/asval_mod4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "extended_mag",
        "stock",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:fn57",
      "displayName": "Fn Five/SeveN",
      "nameZh": "",
      "nameEn": "Fn Five/SeveN",
      "nameKey": "ra1k.gun.fn57.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ra1k/index/guns/fn57.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:fpx_ak105",
      "displayName": "Fuller Phoenix AK-105",
      "nameZh": "",
      "nameEn": "Fuller Phoenix AK-105",
      "nameKey": "ra1k.gun.ak105.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/fpx_ak105.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "damage_addend": 100,
          "ads_addend": 100.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:groza",
      "displayName": "Ots-14 \"Groza\"",
      "nameZh": "",
      "nameEn": "Ots-14 \"Groza\"",
      "nameKey": "ra1k.gun.groza.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/groza.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "damage_addend": 100,
          "ads_addend": 100.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:groza_gp25",
      "displayName": "Ots-14 \"Groza\" with GP-25",
      "nameZh": "",
      "nameEn": "Ots-14 \"Groza\" with GP-25",
      "nameKey": "ra1k.gun.groza_gp25.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/ra1k/index/guns/groza_gp25.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ra1k:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:hk_mp5a5_bolt",
      "displayName": "HK-MP5A5 bolt action",
      "nameZh": "",
      "nameEn": "HK-MP5A5 bolt action",
      "nameKey": "ra1k.gun.hk_mp5a5.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/ra1k/index/guns/hk_mp5a5_bolt.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:m590",
      "displayName": "Mossberg 590 tactical shotgun",
      "nameZh": "",
      "nameEn": "Mossberg 590 tactical shotgun",
      "nameKey": "ra1k.gun.m590.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/ra1k/index/guns/m590.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "grip",
        "scope",
        "laser",
        "stock",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:mk47",
      "displayName": "CMMG MK-47 \"Mutant\"",
      "nameZh": "",
      "nameEn": "CMMG MK-47 \"Mutant\"",
      "nameKey": "ra1k.gun.mk47.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/mk47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:mp155",
      "displayName": "MP-155 shotgun",
      "nameZh": "",
      "nameEn": "MP-155 shotgun",
      "nameKey": "ra1k.gun.mp155.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/ra1k/index/guns/mp155.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 4,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:nl545",
      "displayName": "Custom Guns NL-545",
      "nameZh": "",
      "nameEn": "Custom Guns NL-545",
      "nameKey": "ra1k.gun.nl545.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/nl545.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:nl545_fde",
      "displayName": "Custom Guns NL-545 GP",
      "nameZh": "",
      "nameEn": "Custom Guns NL-545 GP",
      "nameKey": "ra1k.gun.nl545_fde.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/nl545_fde.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:rpk74",
      "displayName": "RPK-74",
      "nameZh": "",
      "nameEn": "RPK-74",
      "nameKey": "ra1k.gun.rpk74.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/ra1k/index/guns/rpk74.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 45,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "damage_addend": 100,
          "ads_addend": 100.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:rsh12",
      "displayName": "RSH-12 anti matereal revolver",
      "nameZh": "",
      "nameEn": "RSH-12 anti matereal revolver",
      "nameKey": "ra1k.gun.rsh12.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/ra1k/index/guns/rsh12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:127x55",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:sa58",
      "displayName": "DS Arms SA58 rifle",
      "nameZh": "",
      "nameEn": "DS Arms SA58 rifle",
      "nameKey": "ra1k.gun.sa58.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/sa58.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:sa58_m203",
      "displayName": "DS Arms SA58 with M203",
      "nameZh": "",
      "nameEn": "DS Arms SA58 with M203",
      "nameKey": "ra1k.gun.sa58_m203.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/ra1k/index/guns/sa58_m203.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:sr_3m",
      "displayName": "SR-3M",
      "nameZh": "",
      "nameEn": "SR-3M",
      "nameKey": "ra1k.gun.sr_3m.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/sr_3m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:super_shorty",
      "displayName": "Serbu Super Shorty",
      "nameZh": "",
      "nameEn": "Serbu Super Shorty",
      "nameKey": "ra1k.gun.shorty.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/ra1k/index/guns/super_shorty.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:uar15",
      "displayName": "Zbroyar UAR-15",
      "nameZh": "",
      "nameEn": "Zbroyar UAR-15",
      "nameKey": "ra1k.gun.uar15.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/uar15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "extended_mag",
        "stock",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:velociraptor",
      "displayName": "Aklys Defense Velociraptor",
      "nameZh": "",
      "nameEn": "Aklys Defense Velociraptor",
      "nameKey": "ra1k.gun.akv.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/velociraptor.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:voldemort",
      "displayName": "Voldemorts wsnd",
      "nameZh": "",
      "nameEn": "Voldemorts wsnd",
      "nameKey": "ra1k.gun.voldemort.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/ra1k/index/guns/voldemort.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ra1k:avada",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:vsk",
      "displayName": "VSK-94 rifle",
      "nameZh": "",
      "nameEn": "VSK-94 rifle",
      "nameKey": "ra1k.gun.vsk.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/vsk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:vss",
      "displayName": "VSS Sniper rifle",
      "nameZh": "",
      "nameEn": "VSS Sniper rifle",
      "nameKey": "ra1k.gun.vss.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ra1k/index/guns/vss.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9x39",
      "ammoAmount": 10,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "ra1k:vssk",
      "displayName": "VSSK \"Vykhlop\"",
      "nameZh": "",
      "nameEn": "VSSK \"Vykhlop\"",
      "nameKey": "ra1k.gun.vssk.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/ra1k/index/guns/vssk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:127x55",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "ra1k:vulkan",
      "displayName": "IPI Malyuk \"Vulcan\"",
      "nameZh": "",
      "nameEn": "IPI Malyuk \"Vulcan\"",
      "nameKey": "ra1k.gun.vulkan.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/ra1k/index/guns/vulkan.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rcp:at4",
      "displayName": "AT4 M136",
      "nameZh": "AT4 M136",
      "nameEn": "AT4 M136",
      "nameKey": "rcp.gun.at4.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "rpg",
      "indexPath": "data/rcp/index/guns/at4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "rcp:backupat4",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "rcp:mk19",
      "displayName": "MK19 自动榴弹发射器",
      "nameZh": "MK19 自动榴弹发射器",
      "nameEn": "MK19 AGL",
      "nameKey": "rcp.gun.mk19.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rcp/index/guns/mk19.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 50,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rcp:rpg26",
      "displayName": "RPG-26",
      "nameZh": "RPG-26",
      "nameEn": "RPG-26",
      "nameKey": "rcp.gun.rpg26.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "rpg",
      "indexPath": "data/rcp/index/guns/rpg26.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "rcp:backup26",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "rcp:t74",
      "displayName": "七四式火焰喷射器",
      "nameZh": "七四式火焰喷射器",
      "nameEn": "Type74 flamethrower",
      "nameKey": "rcp.gun.t74.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/rcp/index/guns/t74.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "rcp:t74_fuel",
      "ammoAmount": 30,
      "fireModes": [
        "burst"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:6p41bp",
      "displayName": "无托 6P41 机枪",
      "nameZh": "无托 6P41 机枪",
      "nameEn": "Bullpup 6P41 Machine Gun",
      "nameKey": "rfp.gun.6p41bp.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/6p41bp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:dshkm",
      "displayName": "DShK-M",
      "nameZh": "DShK-M",
      "nameEn": "DShK-M",
      "nameKey": "rfp.gun.dshkm.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/dshkm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "rfp:127x108",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:lwmmg",
      "displayName": "LWMMG 通用机枪",
      "nameZh": "LWMMG 通用机枪",
      "nameEn": "LWMMG GPMG",
      "nameKey": "rfp.gun.lwmmg.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/lwmmg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "rfp:nm338",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:m249_t",
      "displayName": "M249 SAW",
      "nameZh": "M249 SAW",
      "nameEn": "M249 SAW",
      "nameKey": "rfp.gun.m249_t.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/m249_t.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 75,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:m2hb",
      "displayName": "FN M2HB QCB",
      "nameZh": "FN M2HB QCB",
      "nameEn": "FN M2HB QCB",
      "nameKey": "rfp.gun.m2hb.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/m2hb.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 100,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:mg43",
      "displayName": "MG43 “机枪”",
      "nameZh": "MG43 “机枪”",
      "nameEn": "MG43 \"Machine gun\"",
      "nameKey": "rfp.gun.mg43.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/mg43.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 100,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:reapr",
      "displayName": "REAPR",
      "nameZh": "REAPR",
      "nameEn": "REAPR",
      "nameKey": "rfp.gun.reapr.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/reapr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "rfp:nm338",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "rfp:rpl20",
      "displayName": "RPL 20 机枪",
      "nameZh": "RPL 20 机枪",
      "nameEn": "RPL 20 MG",
      "nameKey": "rfp.gun.rpl20.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "mg",
      "indexPath": "data/rfp/index/guns/rpl20.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:aiyasinrpg",
      "displayName": "RPG-7 亚辛 105毫米 火箭发射器",
      "nameZh": "RPG-7 亚辛 105毫米 火箭发射器",
      "nameEn": "RPG-7 Al-Yassin 105mm Rocket Launcher",
      "nameKey": "suffuse.gun.aiyasinrpg.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/aiyasinrpg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:rpg_rocket",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:aks74u",
      "displayName": "AKs74u 突击步枪",
      "nameZh": "AKs74u 突击步枪",
      "nameEn": "AKs74u Assault Rifle",
      "nameKey": "suffuse.gun.aks74u.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/aks74u.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:an94",
      "displayName": "AN94 突击步枪",
      "nameZh": "AN94 突击步枪",
      "nameEn": "AN94 Assault Rifle",
      "nameKey": "suffuse.gun.an94.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/an94.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:ar57",
      "displayName": "AR57 ULT 卡宾步枪",
      "nameZh": "AR57 ULT 卡宾步枪",
      "nameEn": "AR-57 Carbine",
      "nameKey": "suffuse.gun.ar57.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/suffuse/index/guns/ar57.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:ash12",
      "displayName": "ASH12 突击步枪",
      "nameZh": "ASH12 突击步枪",
      "nameEn": "ASH12 Assault Rifle",
      "nameKey": "suffuse.gun.ash12.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/ash12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:12.7x55",
      "ammoAmount": 10,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:aw50",
      "displayName": "精密国际 AW-50 反器材狙击步枪",
      "nameZh": "精密国际 AW-50 反器材狙击步枪",
      "nameEn": "Accuracy International AW-50  Anti-Material Rifle",
      "nameKey": "suffuse.gun.aw50.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/aw50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:axmc",
      "displayName": "精密国际 AXMC 狙击步枪",
      "nameZh": "精密国际 AXMC 狙击步枪",
      "nameEn": "Accurate International AXMC Sniper Rifle",
      "nameKey": "suffuse.gun.axmc.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/axmc.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "laser",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:axsr",
      "displayName": "精密国际 AXSR 狙击步枪",
      "nameZh": "精密国际 AXSR 狙击步枪",
      "nameEn": "Accuracy International AXSR Sniper Rifle",
      "nameKey": "suffuse.gun.axsr.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/axsr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "laser",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:cslr4",
      "displayName": "CS/LR4型 狙击步枪",
      "nameZh": "CS/LR4型 狙击步枪",
      "nameEn": "CS/LR4 Sniper Rifle",
      "nameKey": "suffuse.gun.cslr4.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/cslr4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:desert",
      "displayName": "Desert Eagle Mk XIX 大威力半自动手枪",
      "nameZh": "Desert Eagle Mk XIX 大威力半自动手枪",
      "nameEn": "Desert Eagle Mk XIX Semi-Automatic Pistol",
      "nameKey": "suffuse.gun.desert.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/desert.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50ae",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:dvl10",
      "displayName": "DVL-10 狙击步枪",
      "nameZh": "DVL-10 狙击步枪",
      "nameEn": "DVL-10 Sniper Rifle",
      "nameKey": "suffuse.gun.dvl10.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/dvl10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:famas",
      "displayName": "FAMAS 自动步枪",
      "nameZh": "FAMAS 自动步枪",
      "nameEn": "FAMAS Assault Rifle",
      "nameKey": "suffuse.gun.famas.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/famas.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 25,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:gen12",
      "displayName": "TTI Gen-12 全自动霰弹枪",
      "nameZh": "TTI Gen-12 全自动霰弹枪",
      "nameEn": "TTI Gen-12 Auto Shotgun",
      "nameKey": "suffuse.gun.gen12.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/suffuse/index/guns/gen12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:gepardpdw",
      "displayName": "Gepard “猎豹” PDW 个人防卫武器",
      "nameZh": "Gepard “猎豹” PDW 个人防卫武器",
      "nameEn": "Gepard PDW",
      "nameKey": "suffuse.gun.gepardpdw.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/suffuse/index/guns/gepardpdw.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 40,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:gm6",
      "displayName": "GM6 Lynx “山猫” 反器材狙击步枪",
      "nameZh": "GM6 Lynx “山猫” 反器材狙击步枪",
      "nameEn": "GM6 Lynx Anti-Material Rifle",
      "nameKey": "suffuse.gun.gm6.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/gm6.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:kacpdw",
      "displayName": "KAC PDW 个人防卫武器",
      "nameZh": "KAC PDW 个人防卫武器",
      "nameEn": "KAC PDW",
      "nameKey": "suffuse.gun.kacpdw.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/kacpdw.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:6x35mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:knife",
      "displayName": "suffuse.gun.knife.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "suffuse.gun.knife.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/knife.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic"
    },
    {
      "id": "suffuse:ks23m",
      "displayName": "KS-23M 霰弹枪",
      "nameZh": "KS-23M 霰弹枪",
      "nameEn": "KS-23M Shotgun",
      "nameKey": "suffuse.gun.ks23m.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/suffuse/index/guns/ks23m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:23mm",
      "ammoAmount": 3,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "stock",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:l115a3",
      "displayName": "L115A3 狙击枪",
      "nameZh": "L115A3 狙击枪",
      "nameEn": "L115A3 Sniper Rifle",
      "nameKey": "suffuse.gun.l115a3.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/l115a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:l119a2",
      "displayName": "L119A2 突击步枪",
      "nameZh": "L119A2 突击步枪",
      "nameEn": "L119A2 Assault Rifle",
      "nameKey": "suffuse.gun.l119a2.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/l119a2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:lifecard",
      "displayName": "LifeCard 单发折叠手枪",
      "nameZh": "LifeCard 单发折叠手枪",
      "nameEn": "LifeCard Single-Shot Foldable Weapon",
      "nameKey": "suffuse.gun.lifecard.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/lifecard.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:.22wmr",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:m1895",
      "displayName": "马林 M1895 杠杆步枪",
      "nameZh": "马林 M1895 杠杆步枪",
      "nameEn": "Marlin M1895 Lever-Action rifle",
      "nameKey": "suffuse.gun.m1895.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/m1895.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45_70",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:m200",
      "displayName": "CheyTac M200 “干预” 狙击步枪",
      "nameZh": "CheyTac M200 “干预” 狙击步枪",
      "nameEn": "CheyTac M200 Intervention Sniper Rifle",
      "nameKey": "suffuse.gun.m200.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/m200.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:.408ct",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:m203",
      "displayName": "M203 榴弹发射器",
      "nameZh": "M203 榴弹发射器",
      "nameEn": "M203 Grenade Launcher",
      "nameKey": "suffuse.gun.m203.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/m203.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:m32a1",
      "displayName": "米尔科 M32A1 转轮连发式榴弹发射器",
      "nameZh": "米尔科 M32A1 转轮连发式榴弹发射器",
      "nameEn": "Milkor M32A1 Multiple Grenade Launcher",
      "nameKey": "suffuse.gun.m32a1.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/m32a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:m79",
      "displayName": "M79 40毫米 榴弹发射器",
      "nameZh": "M79 40毫米 榴弹发射器",
      "nameEn": "M79 40mm Grenade Launcher",
      "nameKey": "suffuse.gun.m79.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/m79.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:mas38",
      "displayName": "MAS-38型 冲锋枪",
      "nameZh": "MAS-38型 冲锋枪",
      "nameEn": "MAS-38 Submachinegun",
      "nameKey": "suffuse.gun.mas38.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/suffuse/index/guns/mas38.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:7.65x20mm",
      "ammoAmount": 32,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:mg338",
      "displayName": "SIG MG-338 通用机枪",
      "nameZh": "SIG MG-338 通用机枪",
      "nameEn": "SIG MG-338 General Purpose Machine Gun",
      "nameKey": "suffuse.gun.mg338.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/mg338.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:338nm",
      "ammoAmount": 50,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:mk3",
      "displayName": "FN 米尼米 MK3 班用机枪",
      "nameZh": "FN 米尼米 MK3 班用机枪",
      "nameEn": "FN Minimi MK3 Squad Automated Weapon",
      "nameKey": "suffuse.gun.mk3.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/mk3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:mk47",
      "displayName": "CMMG Mk47 Mutant 突击步枪",
      "nameZh": "CMMG Mk47 Mutant 突击步枪",
      "nameEn": "CMMG Mk47 Mutant Assault Rifle",
      "nameKey": "suffuse.gun.mk47.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/mk47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:mpdr",
      "displayName": "马盖普 PDR 个人防卫武器",
      "nameZh": "马盖普 PDR 个人防卫武器",
      "nameEn": "Magpul PDR",
      "nameKey": "suffuse.gun.mpdr.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/mpdr.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:n4",
      "displayName": "Noveske N4 突击步枪",
      "nameZh": "Noveske N4 突击步枪",
      "nameEn": "Noveske N4 Assault Rifle",
      "nameKey": "suffuse.gun.n4.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/n4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:np762",
      "displayName": "北方工业 NP762型 自动手枪",
      "nameZh": "北方工业 NP762型 自动手枪",
      "nameEn": "Norinco type NP762 Pistol",
      "nameKey": "suffuse.gun.np762.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/np762.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:pf98a",
      "displayName": "PF-98A 120毫米 火箭发射器",
      "nameZh": "PF-98A 120毫米 火箭发射器",
      "nameEn": "PF-98A 120mm Rocket Launcher",
      "nameKey": "suffuse.gun.pf98a.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/pf98a.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:120mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:pkp",
      "displayName": "PKP “佩切涅格” 轻机枪",
      "nameZh": "PKP “佩切涅格” 轻机枪",
      "nameEn": "PKP Pecheneg Light Machine Gun",
      "nameKey": "suffuse.gun.pkp.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/pkp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 120,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:pp19",
      "displayName": "PP19 “野牛” 冲锋枪",
      "nameZh": "PP19 “野牛” 冲锋枪",
      "nameEn": "PP19 Bizon Submachine Gun",
      "nameKey": "suffuse.gun.pp19.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/suffuse/index/guns/pp19.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 64,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:ptrd41",
      "displayName": "PTRD-41 反坦克步枪",
      "nameZh": "PTRD-41 反坦克步枪",
      "nameEn": "PTRD-41 Anti-Tank Rifle",
      "nameKey": "suffuse.gun.ptrd41.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/ptrd41.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:14.5x114mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:python",
      "displayName": "Python Colt “蟒蛇” 左轮手枪",
      "nameZh": "Python Colt “蟒蛇” 左轮手枪",
      "nameEn": "Python Colt Revolver",
      "nameKey": "suffuse.gun.python.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/python.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qba221",
      "displayName": "QBA-221式 全自动霰弹枪",
      "nameZh": "QBA-221式 全自动霰弹枪",
      "nameEn": "QBA-221式 Auto Shotgun",
      "nameKey": "suffuse.gun.qba221.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/suffuse/index/guns/qba221.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 8,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qbu191",
      "displayName": "QBU-191 精确射手步枪",
      "nameZh": "QBU-191 精确射手步枪",
      "nameEn": "QBU-191 Designated Marksman Rifle",
      "nameKey": "suffuse.gun.qbu191.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/qbu191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qbu88",
      "displayName": "QBU-88式 狙击步枪",
      "nameZh": "QBU-88式 狙击步枪",
      "nameEn": "QBU-88式 Sniper Rifle",
      "nameKey": "suffuse.gun.qbu88.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/qbu88.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qbz191",
      "displayName": "QBZ-191 突击步枪",
      "nameZh": "QBZ-191 突击步枪",
      "nameEn": "QBZ-191 Assault Rifle",
      "nameKey": "suffuse.gun.qbz191.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/qbz191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qbz192",
      "displayName": "QBZ-192 短突击步枪",
      "nameZh": "QBZ-192 短突击步枪",
      "nameEn": "QBZ-192 Compact Assault Rifle",
      "nameKey": "suffuse.gun.qbz192.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/qbz192.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qbz951",
      "displayName": "QBZ-95-1 突击步枪",
      "nameZh": "QBZ-95-1 突击步枪",
      "nameEn": "QBZ-95-1 Assault Rifle",
      "nameKey": "suffuse.gun.qbz951.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/qbz951.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qbz951b",
      "displayName": "QBZ-95B-1 短突击步枪",
      "nameZh": "QBZ-95B-1 短突击步枪",
      "nameEn": "QBZ-95B-1 Assault Carbine",
      "nameKey": "suffuse.gun.qbz951b.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/qbz951b.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qbz951s",
      "displayName": "QBZ-95-1 赤霄战术改件 突击步枪",
      "nameZh": "QBZ-95-1 赤霄战术改件 突击步枪",
      "nameEn": "QBZ-95-1 Tactical Modification Assault Rifle",
      "nameKey": "suffuse.gun.qbz951s.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/qbz951s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qcq171",
      "displayName": "QCQ-171型 冲锋枪",
      "nameZh": "QCQ-171型 冲锋枪",
      "nameEn": "QCQ-171 Submachine Gun",
      "nameKey": "suffuse.gun.qcq171.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/suffuse/index/guns/qcq171.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qjb951",
      "displayName": "QJB-95-1 班用机枪",
      "nameZh": "QJB-95-1 班用机枪",
      "nameEn": "QJB-95-1 Squad Automated Weapon",
      "nameKey": "suffuse.gun.qjb951.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/qjb951.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:qjb_201",
      "displayName": "QJB-201式 班用机枪",
      "nameZh": "QJB-201式 班用机枪",
      "nameEn": "QJB-201 Squad Automatic Weapon",
      "nameKey": "suffuse.gun.qjb_201.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/qjb_201.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qjz171",
      "displayName": "QJZ-171式 重机枪",
      "nameZh": "QJZ-171式 重机枪",
      "nameEn": "QJZ-171 Heavy Machine Gun",
      "nameKey": "suffuse.gun.qjz171.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/qjz171.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 50,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qlu11",
      "displayName": "QLU-11 狙击榴弹发射器",
      "nameZh": "QLU-11 狙击榴弹发射器",
      "nameEn": "QLU-11 Grenade Launcher",
      "nameKey": "suffuse.gun.qlu11.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/qlu11.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:35x32mm",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qlz87",
      "displayName": "QLZ-87式 自动榴弹发射器",
      "nameZh": "QLZ-87式 自动榴弹发射器",
      "nameEn": "QLZ-87 Automatic Grenade Launcher",
      "nameKey": "suffuse.gun.qlz87.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rpg",
      "indexPath": "data/suffuse/index/guns/qlz87.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:35x32mm",
      "ammoAmount": 6,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:qsz92g",
      "displayName": "QSZ-92G 手枪",
      "nameZh": "QSZ-92G 手枪",
      "nameEn": "QSZ-92G Pistol",
      "nameKey": "suffuse.gun.qsz92g.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/qsz92g.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 16,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:rm277",
      "displayName": "RM277 突击步枪",
      "nameZh": "RM277 突击步枪",
      "nameEn": "RM277 Assault Rifle",
      "nameKey": "suffuse.gun.rm277.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/rm277.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:6.8tvcm",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "suffuse:saddam_golden_ak",
      "displayName": "萨达姆的黄金AK",
      "nameZh": "萨达姆的黄金AK",
      "nameEn": "Saddam`s Golden AK",
      "nameKey": "suffuse.gun.saddam_golden_ak.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/saddam_golden_ak.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "suffuse:spas12",
      "displayName": "弗兰基 SPAS-12 多用途霰弹枪",
      "nameZh": "弗兰基 SPAS-12 多用途霰弹枪",
      "nameEn": "Franchi SPAS-12 Multi-Purpose Shotgun",
      "nameKey": "suffuse.gun.spas12.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/suffuse/index/guns/spas12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:svd",
      "displayName": "德拉贡诺夫 SVD 狙击步枪",
      "nameZh": "德拉贡诺夫 SVD 狙击步枪",
      "nameEn": "Dragunov SVD Sniper Rifle",
      "nameKey": "suffuse.gun.svd.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "sniper",
      "indexPath": "data/suffuse/index/guns/svd.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:sw_686",
      "displayName": "史密斯威森 686 左轮",
      "nameZh": "史密斯威森 686 左轮",
      "nameEn": "Smith & Wesson Model 686 Revolver",
      "nameKey": "suffuse.gun.sw_686.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/sw_686.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:44mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:tec9",
      "displayName": "Tec9 冲锋手枪",
      "nameZh": "Tec9 冲锋手枪",
      "nameEn": "Tec9 Machine Pistol",
      "nameKey": "suffuse.gun.tec9.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/tec9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 15,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:trapper50cal",
      "displayName": "Traditions Trapper 50口径 火帽枪",
      "nameZh": "Traditions Trapper 50口径 火帽枪",
      "nameEn": "Traditions Trapper 50Cal Caplock Rifle",
      "nameKey": "suffuse.gun.trapper50cal.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/trapper50cal.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "suffuse:boomstickshot",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:tt33",
      "displayName": "TT-33 手枪",
      "nameZh": "TT-33 手枪",
      "nameEn": "TT-33 Pistol",
      "nameKey": "suffuse.gun.tt33.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/tt33.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:tti2011",
      "displayName": "TTI Pit Viper “蝮蛇” 手枪",
      "nameZh": "TTI Pit Viper “蝮蛇” 手枪",
      "nameEn": "TTI Pit Viper Pistol",
      "nameKey": "suffuse.gun.tti2011.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/tti2011.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:type81_lmg",
      "displayName": "81式 班用机枪",
      "nameZh": "81式 班用机枪",
      "nameEn": "Type 81 Squad Automatic Weapon",
      "nameKey": "suffuse.gun.type81_lmg.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "mg",
      "indexPath": "data/suffuse/index/guns/type81_lmg.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:ump45",
      "displayName": "UMP45 冲锋枪",
      "nameZh": "UMP45 冲锋枪",
      "nameEn": "UMP45 Submachine Gun",
      "nameKey": "suffuse.gun.ump45.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "smg",
      "indexPath": "data/suffuse/index/guns/ump45.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:usp45",
      "displayName": "HK USP45 手枪 沙色款",
      "nameZh": "HK USP45 手枪 沙色款",
      "nameEn": "HK USP45 Pistol .ver FDE",
      "nameKey": "suffuse.gun.usp45.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/usp45.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:usp45_black",
      "displayName": "HK USP45 手枪 黑色款",
      "nameZh": "HK USP45 手枪 黑色款",
      "nameEn": "HK USP45 Pistol .ver Black",
      "nameKey": "suffuse.gun.usp45_black.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/usp45_black.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:viper2011",
      "displayName": "Viper 2011 “蝰蛇” 手枪",
      "nameZh": "Viper 2011 “蝰蛇” 手枪",
      "nameEn": "Viper 2011 Pistol",
      "nameKey": "suffuse.gun.viper2011.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/viper2011.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:webley1913",
      "displayName": "Webley Mk. 1 自动装填手枪",
      "nameZh": "Webley Mk. 1 自动装填手枪",
      "nameEn": "Webley Mk. 1 Self-Loading Pistol",
      "nameKey": "suffuse.gun.webley1913.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "pistol",
      "indexPath": "data/suffuse/index/guns/webley1913.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "suffuse:xm7",
      "displayName": "SIG XM7 突击步枪",
      "nameZh": "SIG XM7 突击步枪",
      "nameEn": "SIG MCX Spear Assault Rifle",
      "nameKey": "suffuse.gun.xm7.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "rifle",
      "indexPath": "data/suffuse/index/guns/xm7.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "tacz:1522",
      "displayName": "M&P 15-22",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M&P 15-22",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/1522.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:22lr",
      "ammoAmount": 25,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:1873",
      "displayName": "Winchester Model 1873",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Winchester Model 1873",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/1873.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:44_40",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:1881",
      "displayName": "Marlin Model 1881",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Marlin Model 1881",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/1881.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45_70",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:1889",
      "displayName": "Colt New Army Model 1889",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt New Army Model 1889",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/1889.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:38lc",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:1911_tac",
      "displayName": "M1911 TAC",
      "nameZh": "",
      "nameEn": "M1911 TAC",
      "nameKey": "tacz.gun.1911_tac.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/1911_tac.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:27",
      "displayName": "S&W Model 27",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W Model 27",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/27.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:29",
      "displayName": "S&W Model 29",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W Model 29",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/29.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:44magnum",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:4570",
      "displayName": "Marlin Model 1895",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Marlin Model 1895",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/4570.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45_70",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:500",
      "displayName": "S&W .500",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W .500",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/500.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:500mag",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:500s",
      "displayName": "S&W .500 Short Barrel",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W .500 Short Barrel",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/500s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:500mag",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:870",
      "displayName": "Magpul M870",
      "nameZh": "",
      "nameEn": "Magpul M870",
      "nameKey": "tacz.gun.870.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/870.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "grip",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:870magpul",
      "displayName": "M87A1",
      "nameZh": "",
      "nameEn": "M87A1",
      "nameKey": "tacz.gun.870magpul.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/870magpul.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "grip",
        "laser",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:940",
      "displayName": "Mossberg 940",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mossberg 940",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/940.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:9a91",
      "displayName": "9A-91",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "9A-91",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/9a91.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:aa12",
      "displayName": "AA12 霰弹枪",
      "nameZh": "AA12 霰弹枪",
      "nameEn": "AA12 Shotgun",
      "nameKey": "tacz.gun.aa12.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/aa12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 8,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "grip",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ai_awp",
      "displayName": "精密国际 AWM 狙击步枪",
      "nameZh": "精密国际 AWM 狙击步枪",
      "nameEn": "Accuracy International AWM",
      "nameKey": "tacz.gun.ai_awp.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/ai_awp.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ak101",
      "displayName": "AK-101",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AK-101",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ak101.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:ak103",
      "displayName": "AK-103",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AK-103",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ak103.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:ak47",
      "displayName": "AKM 突击步枪",
      "nameZh": "AKM 突击步枪",
      "nameEn": "AKM",
      "nameKey": "tacz.gun.ak47.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ak47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ak47_e",
      "displayName": "AKM Elysium Aiming Kit",
      "nameZh": "",
      "nameEn": "AKM Elysium Aiming Kit",
      "nameKey": "tacz.gun.ak47_e.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ak47_e.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ak74",
      "displayName": "AK-74",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AK-74",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ak74.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "stock",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ak74m",
      "displayName": "AK-74M",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AK-74M",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ak74m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "stock",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:aklys",
      "displayName": "Aklys Defense Velociraptor",
      "nameZh": "",
      "nameEn": "Aklys Defense Velociraptor",
      "nameKey": "tacz.gun.aklys.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/aklys.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:akm",
      "displayName": "AK-47",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AK-47",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/akm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:aks74u",
      "displayName": "AKS-74U",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AKS-74U",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/aks74u.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ar15",
      "displayName": "Colt AR-15A4",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt AR-15A4",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ar15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "laser",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ar18",
      "displayName": "Armalite AR18",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Armalite AR18",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ar18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:arka",
      "displayName": "FN ARKA STD",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "FN ARKA STD",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/arka.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:asval",
      "displayName": "AS Val",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AS Val",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/asval.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:ata",
      "displayName": "ATA 20 Gauge Birdshot Over and Under",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "ATA 20 Gauge Birdshot Over and Under",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/ata.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:20g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:aug",
      "displayName": "AUG 突击步枪",
      "nameZh": "AUG 突击步枪",
      "nameEn": "AUG",
      "nameKey": "tacz.gun.aug.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/aug.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:auga3",
      "displayName": "Steyr AUGA3",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Steyr AUGA3",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/auga3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:aughb",
      "displayName": "Steyr AUGA3 Heavy barrel",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Steyr AUGA3 Heavy barrel",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/aughb.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:augs",
      "displayName": "Steyr AUG A2 SA",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Steyr AUG A2 SA",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/augs.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:auto5",
      "displayName": "FN Browning Auto-5",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "FN Browning Auto-5",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/auto5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:avs36",
      "displayName": "AVS-36",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AVS-36",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/avs36.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 15,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:avt40",
      "displayName": "AVT-40",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AVT-40",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/avt40.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:b93r",
      "displayName": "B93R 冲锋手枪",
      "nameZh": "B93R 冲锋手枪",
      "nameEn": "B93R",
      "nameKey": "tacz.gun.b93r.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/b93r.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 20,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:benm4",
      "displayName": "Benelli M4 Tactical",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Benelli M4 Tactical",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/benm4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:bih",
      "displayName": "The Bitch",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "§dThe Bitch",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/bih.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:bm59",
      "displayName": "BM59 Rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "BM59 Rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/bm59.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:bren",
      "displayName": "Brno-Enfield MkI Machine Gun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Brno-Enfield MkI Machine Gun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/bren.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:303",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:brn16a1",
      "displayName": "BRN-16A1 Rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "BRN-16A1 Rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/brn16a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:c96",
      "displayName": "Mauser C96",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mauser C96",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/c96.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:30mauser",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:cns",
      "displayName": "Colt New Service .455",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt New Service .455",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/cns.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:455web",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:colt_m4",
      "displayName": "Colt M4A1",
      "nameZh": "",
      "nameEn": "Colt M4A1",
      "nameKey": "tacz.gun.colt_m4.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/colt_m4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:cph",
      "displayName": "Colt Model 1903 Pocket Hammerless",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt Model 1903 Pocket Hammerless",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/cph.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:32acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:cz75",
      "displayName": "CZ 75 自动手枪",
      "nameZh": "CZ 75 自动手枪",
      "nameEn": "CZ 75",
      "nameKey": "tacz.gun.cz75.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/cz75.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 16,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:cz75s",
      "displayName": "CZ75 Semi automatic",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "CZ75 Semi automatic",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/cz75s.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 16,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:db_long",
      "displayName": "DB-4 乌萨斯",
      "nameZh": "DB-4 乌萨斯",
      "nameEn": "DB-4 Ursus",
      "nameKey": "tacz.gun.db_long.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/db_long.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:db_short",
      "displayName": "DB-2 杜林人",
      "nameZh": "DB-2 杜林人",
      "nameEn": "DB-2 Durin",
      "nameKey": "tacz.gun.db_short.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/db_short.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:oem_stock_tactical": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.125,
          "recoil_modifier": {
            "pitch": -0.3,
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "tacz:deagle",
      "displayName": ".50 沙漠之鹰",
      "nameZh": ".50 沙漠之鹰",
      "nameEn": "Deagle 50",
      "nameKey": "tacz.gun.deagle.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/deagle.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50ae",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:deagle_golden",
      "displayName": ".357 黄金沙漠之鹰",
      "nameZh": ".357 黄金沙漠之鹰",
      "nameEn": "Golden Deagle 357",
      "nameKey": "tacz.gun.deagle_golden.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/deagle_golden.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 9,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:dp28",
      "displayName": "Пулемёт Дегтярёва Пехотный",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Пулемёт Дегтярёва Пехотный",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/dp28.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 47,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:dragunov",
      "displayName": "SVD Dragunov",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "SVD Dragunov",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/dragunov.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:elephant",
      "displayName": ".700 Nitro Express rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": ".700 Nitro Express rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rpg",
      "indexPath": "data/tacz/index/guns/elephant.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:700ne",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:emg_p90",
      "displayName": "EMG P90 Custom Edition",
      "nameZh": "",
      "nameEn": "EMG P90 Custom Edition",
      "nameKey": "tacz.gun.emg_p90.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/emg_p90.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:fal",
      "displayName": "FN FAL Black",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "FN FAL Black",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/fal.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:famas",
      "displayName": "FAMAS F1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "FAMAS F1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/famas.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 25,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:fg42",
      "displayName": "FG-42",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "FG-42",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/fg42.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:fn_evolys",
      "displayName": "FN EVOLYS 机枪",
      "nameZh": "FN EVOLYS 机枪",
      "nameEn": "FN EVOLYS Machine Gun",
      "nameKey": "tacz.gun.fn_evolys.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/fn_evolys.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:fn_fal",
      "displayName": "FN FAL 战斗步枪",
      "nameZh": "FN FAL 战斗步枪",
      "nameEn": "FN FAL Battle Rifle",
      "nameKey": "tacz.gun.fn_fal.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/fn_fal.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:frolovka",
      "displayName": "Frolovka shotgun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Frolovka shotgun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/frolovka.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:20g",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g18",
      "displayName": "Glock 18C",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Glock 18C",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/g18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g19",
      "displayName": "Glock 19X",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Glock 19X",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/g19.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g21",
      "displayName": "Glock 21",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Glock 21",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/g21.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 13,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g36c",
      "displayName": "HK G36C",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK G36C",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/g36c.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g36k",
      "displayName": "G36K 突击步枪",
      "nameZh": "G36K 突击步枪",
      "nameEn": "G36K",
      "nameKey": "tacz.gun.g36k.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/g36k.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g3a3",
      "displayName": "HK G3A3",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK G3A3",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/g3a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g3ka4",
      "displayName": "HK G3KA4",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK G3KA4",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/g3ka4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:g44",
      "displayName": "Glock 44",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Glock 44",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/g44.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:22lr",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:gak47",
      "displayName": "Saddam's Golden AK",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Saddam's Golden AK",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/gak47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:gew96",
      "displayName": "Swedish Gewehr M/96",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Swedish Gewehr M/96",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/gew96.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:65x55",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:gew98",
      "displayName": "Gewehr 98",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Gewehr 98",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/gew98.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:glock_17",
      "displayName": "格洛克 17 手枪",
      "nameZh": "格洛克 17 手枪",
      "nameEn": "Glock 17",
      "nameKey": "tacz.gun.glock_17.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/glock_17.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:grizzly",
      "displayName": "LAR Grizzly Win Mag",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "LAR Grizzly Win Mag",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/grizzly.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:44magnum",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk33",
      "displayName": "HK33",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK33",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/hk33.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk416a8",
      "displayName": "HK-416a8",
      "nameZh": "",
      "nameEn": "HK-416a8",
      "nameKey": "tacz.gun.hk416a8.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/hk416a8.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk416c",
      "displayName": "HK-416C",
      "nameZh": "",
      "nameEn": "HK-416C",
      "nameKey": "tacz.gun.hk416c.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/hk416c.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk416d",
      "displayName": "HK-416A5 突击步枪",
      "nameZh": "HK-416A5 突击步枪",
      "nameEn": "HK-416A5",
      "nameKey": "tacz.gun.hk416d.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/hk416d.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk5",
      "displayName": "HK5A2",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK5A2",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/hk5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk53",
      "displayName": "HK53",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK53",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/hk53.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk_g3",
      "displayName": "G3 战斗步枪",
      "nameZh": "G3 战斗步枪",
      "nameEn": "HK G3 Battle rifle",
      "nameKey": "tacz.gun.hk_g3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/hk_g3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk_mk23",
      "displayName": "MK23 进攻手枪",
      "nameZh": "MK23 进攻手枪",
      "nameEn": "MK23 Offensive Pistol",
      "nameKey": "tacz.gun.hk_mk23.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/hk_mk23.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk_mp5a5",
      "displayName": "MP5A5 冲锋枪",
      "nameZh": "MP5A5 冲锋枪",
      "nameEn": "HK-MP5A5",
      "nameKey": "tacz.gun.hk_mp5a5.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/hk_mp5a5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:hk_sl8",
      "displayName": "HK SL8",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK SL8",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/hk_sl8.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "stock",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:honey",
      "displayName": "AAC Honey Badger",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "AAC Honey Badger",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/honey.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:300blk",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:intervention",
      "displayName": "M200 Intervention",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M200 Intervention",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/intervention.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:408cheytac",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:kar98",
      "displayName": "毛瑟 KAR98K 制式步枪",
      "nameZh": "毛瑟 KAR98K 制式步枪",
      "nameEn": "Mauser Kar98k Rifle",
      "nameKey": "tacz.gun.kar98.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/kar98.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:kar98k",
      "displayName": "Karabiner 98 Kurz",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Karabiner 98 Kurz",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/kar98k.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l110a1",
      "displayName": "L110A1 LMG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L110A1 LMG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/l110a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l1a1",
      "displayName": "L1A1 SLR",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L1A1 SLR",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/l1a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l22a1",
      "displayName": "L22A1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L22A1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/l22a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l403a1",
      "displayName": "KAC L403A1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "KAC L403A1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/l403a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l42a1",
      "displayName": "L42A1 Sniper Rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L42A1 Sniper Rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/l42a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l4a1",
      "displayName": "Brno-Enfield L4A1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Brno-Enfield L4A1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/l4a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l7a2",
      "displayName": "L7A2 GPMG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L7A2 GPMG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/l7a2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 50,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l85a1",
      "displayName": "L85A1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L85A1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/l85a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l85a3",
      "displayName": "L85A2",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L85A2",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/l85a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l86a1",
      "displayName": "L86A1 LSW",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L86A1 LSW",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/l86a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:l96a1",
      "displayName": "L96A1 Arctic Warfare",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "L96A1 Arctic Warfare",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/l96a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:lewis",
      "displayName": "Lewis M1914 Machine Gun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Lewis M1914 Machine Gun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/lewis.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:303",
      "ammoAmount": 47,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:lonetrail",
      "displayName": ".30-06 孤星 手炮",
      "nameZh": ".30-06 孤星 手炮",
      "nameEn": ".30-06 Lonetrail Hand Cannon",
      "nameKey": "tacz.gun.lonetrail.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/lonetrail.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:lupara",
      "displayName": "Lupara",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Lupara",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/lupara.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1",
      "displayName": "M1 Carbine Light Rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1 Carbine Light Rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30c",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle",
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m10",
      "displayName": "S&W Model 10",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W Model 10",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:38special",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1014",
      "displayName": "M1014 战斗霰弹枪",
      "nameZh": "M1014 战斗霰弹枪",
      "nameEn": "M1014 Battle Shotgun",
      "nameKey": "tacz.gun.m1014.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m1014.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m107",
      "displayName": "M107 .50口径反器材步枪",
      "nameZh": "M107 .50口径反器材步枪",
      "nameEn": "M107 Sniper Rifle",
      "nameKey": "tacz.gun.m107.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m107.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m110",
      "displayName": "M110 SASS",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M110 SASS",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m110.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1187",
      "displayName": "Remington M11-87",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Remington M11-87",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m1187.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m14",
      "displayName": "M14 Battle rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M14 Battle rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m14.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m16",
      "displayName": "M16",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M16",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m16.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m16a1",
      "displayName": "M16A1 制式步枪",
      "nameZh": "M16A1 制式步枪",
      "nameEn": "M16A1 Service Rifle",
      "nameKey": "tacz.gun.m16a1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m16a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m16a2",
      "displayName": "M16A2",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M16A2",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m16a2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "grip",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m16a3",
      "displayName": "M16A3",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M16A3",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m16a3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "laser",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m16a4",
      "displayName": "M16A4 制式步枪",
      "nameZh": "M16A4 制式步枪",
      "nameEn": "M16A4 Service Rifle",
      "nameKey": "tacz.gun.m16a4.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m16a4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "grip",
        "stock",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m17",
      "displayName": "SIG M17",
      "nameZh": "",
      "nameEn": "SIG M17",
      "nameKey": "tacz.gun.m17.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m17.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1887",
      "displayName": "Winchester M1887",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Winchester M1887",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m1887.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1895",
      "displayName": "Nagant M1895",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Nagant M1895",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m1895.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762r",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1897",
      "displayName": "Winchester M1897",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Winchester M1897",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m1897.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1903",
      "displayName": "Springfield M1903",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Springfield M1903",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m1903.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1907",
      "displayName": "M1907 Self-loading",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1907 Self-loading",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m1907.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:351wsl",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1911",
      "displayName": "M1911 手枪",
      "nameZh": "M1911 手枪",
      "nameEn": "M1911",
      "nameKey": "tacz.gun.m1911.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m1911.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1911a1",
      "displayName": "Colt M1911A1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt M1911A1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m1911a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "laser",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1917",
      "displayName": "Colt M1917",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt M1917",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m1917.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1918",
      "displayName": "M1918A2 BAR",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1918A2 BAR",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/m1918.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "grip"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m1918a2",
      "displayName": "M1918 BAR",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1918 BAR",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m1918a2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m1919",
      "displayName": "Browning .30 Cal M1919A6",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Browning .30 Cal M1919A6",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/m1919.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1921",
      "displayName": "M1921 Thompson",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1921 Thompson",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/m1921.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m1938",
      "displayName": "Mosin M38 Carbine",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mosin M38 Carbine",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m1938.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1944",
      "displayName": "Mosin M44 Carbine",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mosin M44 Carbine",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m1944.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m1a1",
      "displayName": "M1A1 Thomspon",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1A1 Thomspon",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/m1a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m1g",
      "displayName": "M1 Garand",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1 Garand",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m1g.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m2",
      "displayName": "M2 Carbine Light Rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M2 Carbine Light Rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m2.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30c",
      "ammoAmount": 15,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle",
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m21",
      "displayName": "XM21 SWS",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "XM21 SWS",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m21.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m240b",
      "displayName": "M240B GPMG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M240B GPMG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/m240b.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 0,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m249",
      "displayName": "M249 机枪",
      "nameZh": "M249 机枪",
      "nameEn": "M249 Machine Gun",
      "nameKey": "tacz.gun.m249.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/m249.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m27rwk",
      "displayName": "M27 RWK",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M27 RWK",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m27rwk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m320",
      "displayName": "M320 榴弹发射器",
      "nameZh": "M320 榴弹发射器",
      "nameEn": "M320 Grenade Launcher",
      "nameKey": "tacz.gun.m320.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rpg",
      "indexPath": "data/tacz/index/guns/m320.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m37",
      "displayName": "Ithaca M37 shotgun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Ithaca M37 shotgun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m37.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 3,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m3a1",
      "displayName": "M3A1 Grease Gun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M3A1 Grease Gun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/m3a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 30,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "stock"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:m40a1",
      "displayName": "M40",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M40",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m40a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m4a1",
      "displayName": "M4A1 卡宾枪",
      "nameZh": "M4A1 卡宾枪",
      "nameEn": "M4A1 Carbine",
      "nameKey": "tacz.gun.m4a1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m4a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "laser",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m4burst",
      "displayName": "M4",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M4",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/m4burst.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "grip",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m500a",
      "displayName": "Mossberg 500A",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mossberg 500A",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m500a.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m51",
      "displayName": "Beretta M1951",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Beretta M1951",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m51.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m60",
      "displayName": "M60 GMPG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M60 GMPG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/m60.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m60e4",
      "displayName": "M60E4 GMPG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M60E4 GMPG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/m60e4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 100,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m70",
      "displayName": "Winchester M70",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Winchester M70",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m70.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 4,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m700",
      "displayName": "M700 狙击步枪",
      "nameZh": "M700 狙击步枪",
      "nameEn": "M700 Sniper Rifle",
      "nameKey": "tacz.gun.m700.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m700.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:30_06",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m79",
      "displayName": "M79 Grenade launcher",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M79 Grenade launcher",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rpg",
      "indexPath": "data/tacz/index/guns/m79.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m82a1",
      "displayName": "M82A1 Barrett Anti-materiel rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M82A1 Barrett Anti-materiel rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m82a1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m870",
      "displayName": "M870 霰弹枪",
      "nameZh": "M870 霰弹枪",
      "nameEn": "M870",
      "nameKey": "tacz.gun.m870.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m870.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m870l",
      "displayName": "Long Barreled M870 Shotgun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Long Barreled M870 Shotgun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m870l.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m88",
      "displayName": "Mossberg Maverick 88 tactical shotgun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mossberg Maverick 88 tactical shotgun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/m88.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m9",
      "displayName": "Beretta M9",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Beretta M9",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m9.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 15,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m95",
      "displayName": "M95 .50口径反器材步枪",
      "nameZh": "M95 .50口径反器材步枪",
      "nameEn": "M95 .50 Cal Antimaterial",
      "nameKey": "tacz.gun.m95.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/m95.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50bmg",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m9a4",
      "displayName": "伯莱塔 M9A4 手枪",
      "nameZh": "伯莱塔 M9A4 手枪",
      "nameEn": "M9A4",
      "nameKey": "tacz.gun.m9a4.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m9a4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 17,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:m9a4_albert",
      "displayName": "M9A4 Albert",
      "nameZh": "",
      "nameEn": "M9A4 Albert",
      "nameKey": "tacz.gun.m9a4_albert.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/m9a4_albert.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic"
    },
    {
      "id": "tacz:mac10",
      "displayName": "MAC M10 Machine Pistol",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MAC M10 Machine Pistol",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/mac10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mac11",
      "displayName": "MAC M11 Machine Pistol",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MAC M11 Machine Pistol",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/mac11.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:380auto",
      "ammoAmount": 32,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mag10",
      "displayName": "Ithaca Mag 10",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Ithaca Mag 10",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/mag10.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:10g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mg08",
      "displayName": "MG 08/15",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MG 08/15",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/mg08.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 0,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mg3",
      "displayName": "MG3 GPMG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MG3 GPMG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/mg3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 65,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mg36",
      "displayName": "HK MG36",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK MG36",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/mg36.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 100,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mg42",
      "displayName": "MG-42 ",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MG-42 ",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/mg42.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:792x57",
      "ammoAmount": 75,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mgl",
      "displayName": "Milkor Multi-Grenade Launcher",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Milkor Multi-Grenade Launcher",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rpg",
      "indexPath": "data/tacz/index/guns/mgl.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mini14",
      "displayName": "Ruger Mini-14 Tactical",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Ruger Mini-14 Tactical",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/mini14.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:minigun",
      "displayName": "M134 转管机枪",
      "nameZh": "M134 转管机枪",
      "nameEn": "M134 Minigun",
      "nameKey": "tacz.gun.minigun.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/minigun.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 0,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mk14",
      "displayName": "MK14 EBR 精确射手步枪",
      "nameZh": "MK14 EBR 精确射手步枪",
      "nameEn": "MK14 EBR",
      "nameKey": "tacz.gun.mk14.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/mk14.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mk18",
      "displayName": "Mk18 Mod 0",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mk18 Mod 0",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/mk18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "laser",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mk48",
      "displayName": "Mk 48 Mod 0 GMPG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mk 48 Mod 0 GMPG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/mk48.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mkii",
      "displayName": "S&W Mk II .455",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W Mk II .455",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/mkii.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:455web",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mosin",
      "displayName": "Mosin Nagant M91/30",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mosin Nagant M91/30",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/mosin.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mp18",
      "displayName": "MP18-I",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MP18-I",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/mp18.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 32,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:mp40",
      "displayName": "MP40",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MP40",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/mp40.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 32,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "stock",
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mp5k",
      "displayName": "HK MP5K",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK MP5K",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/mp5k.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mp5sd",
      "displayName": "MP5SD",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "MP5SD",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/mp5sd.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ms16",
      "displayName": "M1A SOCOM 16",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1A SOCOM 16",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ms16.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:mustang",
      "displayName": "Colt Mustang",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt Mustang",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/mustang.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:380auto",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:nagant1891",
      "displayName": "Mosin-Nagant M1891",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Mosin-Nagant M1891",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/nagant1891.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:no3",
      "displayName": "S&W Model 3",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W Model 3",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/no3.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:44_40",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:no4",
      "displayName": "Lee Enfield No.4 Mk1 Rifle",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Lee Enfield No.4 Mk1 Rifle",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/no4.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:303",
      "ammoAmount": 9,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:p08",
      "displayName": "Luger P08",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Luger P08",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/p08.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:p320",
      "displayName": "P320 手枪",
      "nameZh": "P320 手枪",
      "nameEn": "P320",
      "nameKey": "tacz.gun.p320.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/p320.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:p35",
      "displayName": "FN Browning Hi-Power",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "FN Browning Hi-Power",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/p35.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 13,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:p38",
      "displayName": "Walther P38",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Walther P38",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/p38.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:p60",
      "displayName": "Panzerfaust 60",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Panzerfaust 60",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rpg",
      "indexPath": "data/tacz/index/guns/p60.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:40mm",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:p90",
      "displayName": "P90 冲锋枪",
      "nameZh": "P90 冲锋枪",
      "nameEn": "P90 PDW",
      "nameKey": "tacz.gun.p90.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/p90.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pa15",
      "displayName": "PSA PA15",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PSA PA15",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/pa15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pkm",
      "displayName": "PKM GPMG",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PKM GPMG",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/pkm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 75,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pm",
      "displayName": "Makarov PM",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Makarov PM",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/pm.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x18",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pp19",
      "displayName": "PP-19 Bizon",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PP-19 Bizon",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/pp19.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x18",
      "ammoAmount": 64,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pp19_01",
      "displayName": "PP-19-01 Vityaz",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PP-19-01 Vityaz",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/pp19_01.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x18",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ppk",
      "displayName": "Walther PPK",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Walther PPK",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/ppk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:32acp",
      "ammoAmount": 7,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pps",
      "displayName": "PPS-43",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PPS-43",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/pps.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 35,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:ppsh41",
      "displayName": "PPSH-41",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PPSH-41",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/ppsh41.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 35,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ps90",
      "displayName": "PS90",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PS90",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/ps90.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:57x28",
      "ammoAmount": 50,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:psak47",
      "displayName": "PSAK-47",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PSAK-47",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/psak47.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:psg1",
      "displayName": "HK PSG-1",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK PSG-1",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/psg1.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ptr91",
      "displayName": "PTR 91",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PTR 91",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/ptr91.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:python",
      "displayName": "Colt Python",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt Python",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/python.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:pythons",
      "displayName": "Colt Python (Short)",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt Python (Short)",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/pythons.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:qbz_191",
      "displayName": "191式 突击步枪",
      "nameZh": "191式 突击步枪",
      "nameEn": "QBZ-191 Assault Rifle",
      "nameKey": "tacz.gun.qbz_191.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/qbz_191.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "grip",
        "laser",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:qbz_95",
      "displayName": "95式 \"长弓\" 突击步枪",
      "nameZh": "95式 \"长弓\" 突击步枪",
      "nameEn": "QBZ-95 \"Longbow\"",
      "nameKey": "tacz.gun.qbz_95.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/qbz_95.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:58x42",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "grip",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rhino357",
      "displayName": ".357 犀牛 左轮手枪",
      "nameZh": ".357 犀牛 左轮手枪",
      "nameEn": ".357 Rhino Revolver",
      "nameKey": "tacz.gun.rhino357.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/rhino357.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:357mag",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rpg7",
      "displayName": "RPG-7 火箭筒",
      "nameZh": "RPG-7 火箭筒",
      "nameEn": "RPG-7",
      "nameKey": "tacz.gun.rpg7.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rpg",
      "indexPath": "data/tacz/index/guns/rpg7.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:rpg_rocket",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rpk",
      "displayName": "RPK 轻机枪",
      "nameZh": "RPK 轻机枪",
      "nameEn": "RPK",
      "nameKey": "tacz.gun.rpk.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/rpk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 41,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rpk74",
      "displayName": "RPK-74 Light Machine Gun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "RPK-74 Light Machine Gun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/rpk74.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "stock",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rpk74m",
      "displayName": "RPK-74M Light Machine Gun",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "RPK-74M Light Machine Gun",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "mg",
      "indexPath": "data/tacz/index/guns/rpk74m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:545x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "stock",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rsh",
      "displayName": "RSH 12",
      "nameZh": "",
      "nameEn": "RSH 12",
      "nameKey": "tacz.gun.rsh.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/rsh.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:127x55",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:rsh12",
      "displayName": "RSH-12 Anti materiel revolver",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "RSH-12 Anti materiel revolver",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/rsh12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:127x55",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "grip",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:sa58",
      "displayName": "DS Arms SA-58",
      "nameZh": "",
      "nameEn": "DS Arms SA-58",
      "nameKey": "tacz.gun.sa58.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/sa58.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "grip",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:saiga",
      "displayName": "Saiga-12",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Saiga-12",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/saiga.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sawedoff",
      "displayName": "Sawed Off Model 870",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Sawed Off Model 870",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/sawedoff.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 3,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:scar_h",
      "displayName": "SCAR-H 战斗步枪",
      "nameZh": "SCAR-H 战斗步枪",
      "nameEn": "SCAR-H Battle Rifle",
      "nameKey": "tacz.gun.scar_h.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/scar_h.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 20,
      "fireModes": [
        "semi",
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "recoil_modifier": {
            "yaw": -0.2
          }
        }
      }
    },
    {
      "id": "tacz:scar_l",
      "displayName": "SCAR-L 突击步枪",
      "nameZh": "SCAR-L 突击步枪",
      "nameEn": "SCAR-L Assault Rifle",
      "nameKey": "tacz.gun.scar_l.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/scar_l.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:scar_sc",
      "displayName": "SCAR-SC",
      "nameZh": "",
      "nameEn": "SCAR-SC",
      "nameKey": "tacz.gun.scar_sc.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/scar_sc.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:shorty",
      "displayName": "Serbu Super Shorty",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Serbu Super Shorty",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/shorty.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 2,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:skorpion",
      "displayName": "Vz.61 Skorpion",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Vz.61 Skorpion",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/skorpion.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x18",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:sks_tactical",
      "displayName": "SKS 战术步枪",
      "nameZh": "SKS 战术步枪",
      "nameEn": "Sks Tactical Rifle",
      "nameKey": "tacz.gun.sks_tactical.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/sks_tactical.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:smle",
      "displayName": "Short, Magazine, Lee Enfield No.1 MkIII",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Short, Magazine, Lee Enfield No.1 MkIII",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/smle.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:303",
      "ammoAmount": 9,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sp5",
      "displayName": "HK SP5",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK SP5",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/sp5.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:spas_12",
      "displayName": "SPAS-12 多功能霰弹枪",
      "nameZh": "SPAS-12 多功能霰弹枪",
      "nameEn": "SPAS-12 Multi-purpose Shotgun",
      "nameKey": "tacz.gun.spas_12.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "shotgun",
      "indexPath": "data/tacz/index/guns/spas_12.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:12g",
      "ammoAmount": 5,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "stock",
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:spr15hb",
      "displayName": "SPR-15 HB “射手座” 反狙击步枪",
      "nameZh": "SPR-15 HB “射手座” 反狙击步枪",
      "nameEn": "SPR-15 HB \"Sagittarius\"",
      "nameKey": "tacz.gun.spr15hb.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/spr15hb.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 15,
      "fireModes": [
        "semi",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:springfield1873",
      "displayName": "春田 1873 活门步枪",
      "nameZh": "春田 1873 活门步枪",
      "nameEn": "Springfield 1873 Trapdoor Rifle",
      "nameKey": "tacz.gun.springfield1873.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/springfield1873.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45_70",
      "ammoAmount": 1,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sr15",
      "displayName": "SR-15",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "SR-15",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/sr15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "laser",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sr25",
      "displayName": "KAC SR25",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "KAC SR25",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/sr25.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:308",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sr3m",
      "displayName": "SR-3M",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "SR-3M",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/sr3m.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:stealth",
      "displayName": "S&W Stealth Hunter",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W Stealth Hunter",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/stealth.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:44magnum",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sten",
      "displayName": "Sten MkII",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Sten MkII",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/sten.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 32,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:stg44",
      "displayName": "StG 44",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "StG 44",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/stg44.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:792x33",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:svt_40",
      "displayName": "SVT-40",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "SVT-40",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/svt_40.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x54",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:sw15",
      "displayName": "M&P 15",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M&P 15",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/sw15.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "scope",
        "grip",
        "stock"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:sw686",
      "displayName": "S&W 686",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "S&W 686",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/sw686.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:38special",
      "ammoAmount": 6,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:tac338",
      "displayName": "TAC-338",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "TAC-338",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/tac338.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:338",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "extended_mag",
        "scope",
        "muzzle",
        "grip"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:taurus500",
      "displayName": "金牛座 \"狂暴猎手\" 手炮",
      "nameZh": "金牛座 \"狂暴猎手\" 手炮",
      "nameEn": "Taurus \"Raging Hunter\" Hand Cannon",
      "nameKey": "tacz.gun.taurus500.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/taurus500.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:500mag",
      "ammoAmount": 5,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:taurus943",
      "displayName": ".22 943型 左轮手枪",
      "nameZh": ".22 943型 左轮手枪",
      "nameEn": ".22 Modle 943 Revolver",
      "nameKey": "tacz.gun.taurus943.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/taurus943.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:22wmr",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:timeless50",
      "displayName": "永恒 .50 Z型",
      "nameZh": "永恒 .50 Z型",
      "nameEn": "Timeless .50 Z-Type",
      "nameKey": "tacz.gun.timeless50.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/timeless50.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:50ae",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:timeless50_tac",
      "displayName": "Timeless .50 Tac-Type",
      "nameZh": "",
      "nameEn": "Timeless .50 Tac-Type",
      "nameKey": "tacz.gun.timeless50_tac.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/timeless50_tac.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic"
    },
    {
      "id": "tacz:tt33",
      "displayName": "Tokarev TT-33",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Tokarev TT-33",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/tt33.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x25",
      "ammoAmount": 8,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:type_81",
      "displayName": "81-1式 制式步枪",
      "nameZh": "81-1式 制式步枪",
      "nameEn": "Type 81-1 Service Rifle",
      "nameKey": "tacz.gun.type_81.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/type_81.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:type_81_viper",
      "displayName": "Type 81-1 Viper",
      "nameZh": "",
      "nameEn": "Type 81-1 Viper",
      "nameKey": "tacz.gun.type_81_viper.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/type_81_viper.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope",
        "stock",
        "grip",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:ump45",
      "displayName": "UMP45 冲锋枪",
      "nameZh": "UMP45 冲锋枪",
      "nameEn": "UMP45 SMG",
      "nameKey": "tacz.gun.ump45.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/ump45.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 25,
      "fireModes": [
        "auto",
        "burst"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "grip",
        "extended_mag",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:usp45",
      "displayName": "HK USP 45",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "HK USP 45",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "pistol",
      "indexPath": "data/tacz/index/guns/usp45.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 12,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "laser"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:uzi",
      "displayName": "乌兹冲锋枪",
      "nameZh": "乌兹冲锋枪",
      "nameEn": "UZI",
      "nameKey": "tacz.gun.uzi.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/uzi.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:9mm",
      "ammoAmount": 20,
      "fireModes": [
        "auto"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:uzicar",
      "displayName": "Uzi Carbine",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Uzi Carbine",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/uzicar.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:22lr",
      "ammoAmount": 10,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "muzzle"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:vecar",
      "displayName": "Vector 45 Carbine",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Vector 45 Carbine",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/vecar.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 13,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:vector45",
      "displayName": "维克托 冲锋枪",
      "nameZh": "维克托 冲锋枪",
      "nameEn": "Vector SMG",
      "nameKey": "tacz.gun.vector45.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "smg",
      "indexPath": "data/tacz/index/guns/vector45.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:45acp",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "burst",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "stock",
        "grip",
        "muzzle",
        "laser",
        "extended_mag"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:vsk",
      "displayName": "VSK-94",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "VSK-94",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/vsk.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x39",
      "ammoAmount": 20,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:vss",
      "displayName": "VSS Vintorez",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "VSS Vintorez",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "sniper",
      "indexPath": "data/tacz/index/guns/vss.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "ea:9x39",
      "ammoAmount": 10,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "scope",
        "extended_mag"
      ],
      "exclusiveAttachments": {
        "tacz:scope_standard_8x": {
          "weight": 2.0,
          "ads_addend": 0.04,
          "inaccuracy_addend": -0.4,
          "recoil_modifier": {
            "pitch": -0.2,
            "yaw": -0.1
          }
        }
      }
    },
    {
      "id": "tacz:xm177",
      "displayName": "Colt XM177 Commando",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Colt XM177 Commando",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/xm177.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:556x45",
      "ammoAmount": 30,
      "fireModes": [
        "auto",
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "scope"
      ],
      "exclusiveAttachments": {}
    },
    {
      "id": "tacz:z70",
      "displayName": "Zastava M70",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Zastava M70",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "rifle",
      "indexPath": "data/tacz/index/guns/z70.json",
      "itemId": "tacz:modern_kinetic_gun",
      "itemType": "modern_kinetic",
      "ammoId": "tacz:762x39",
      "ammoAmount": 30,
      "fireModes": [
        "semi"
      ],
      "allowedAttachmentTypes": [
        "muzzle",
        "extended_mag",
        "stock",
        "scope"
      ],
      "exclusiveAttachments": {}
    }
  ],
  "attachments": [
    {
      "id": "arip:aa12ex",
      "displayName": "ARIP | \"幻灭挽歌\" AA-12 前端套件",
      "nameZh": "ARIP | \"幻灭挽歌\" AA-12 前端套件",
      "nameEn": "ARIP | \"Disillusioned Elegy\" AA-12 Fore Kit",
      "nameKey": "arip.attachment.aa12ex.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/arip/index/attachments/aa12ex.json"
    },
    {
      "id": "arip:arrogance",
      "displayName": "ARIP | \"嚣张\" 制退器",
      "nameZh": "ARIP | \"嚣张\" 制退器",
      "nameEn": "ARIP | \"Arrogance\" Muzzle Brake",
      "nameKey": "arip.attachment.arrogance.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/arip/index/attachments/arrogance.json"
    },
    {
      "id": "arip:calibration",
      "displayName": "ARIP | \"校正·凝集\" 微缩光电装置 (原型)",
      "nameZh": "ARIP | \"校正·凝集\" 微缩光电装置 (原型)",
      "nameEn": "ARIP | \"Calibration\" Visual Effects Stabilizer (Prototype)",
      "nameKey": "arip.attachment.calibration.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "laser",
      "indexPath": "data/arip/index/attachments/calibration.json"
    },
    {
      "id": "arip:coercion",
      "displayName": "ARIP | \"制压四野\" 特化弹种及扩容供弹具",
      "nameZh": "ARIP | \"制压四野\" 特化弹种及扩容供弹具",
      "nameEn": "ARIP | \"Coercion\" Hyper AmmoMod & ExtMag",
      "nameKey": "arip.attachment.coercion.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/arip/index/attachments/coercion.json"
    },
    {
      "id": "arip:open",
      "displayName": "ARIP | \"开放\" 全息衍射瞄具",
      "nameZh": "ARIP | \"开放\" 全息衍射瞄具",
      "nameEn": "ARIP | \"Open\" Holographic Sight",
      "nameKey": "arip.attachment.open.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/arip/index/attachments/open.json"
    },
    {
      "id": "arip:openz",
      "displayName": "ARIP | \"开放·瞭望\" 全息衍射瞄具及增倍镜组 (需要特定光影)",
      "nameZh": "ARIP | \"开放·瞭望\" 全息衍射瞄具及增倍镜组 (需要特定光影)",
      "nameEn": "ARIP | \"Open Z\" Holographic Scope (specific shader required)",
      "nameKey": "arip.attachment.openz.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/arip/index/attachments/openz.json"
    },
    {
      "id": "arip:sangfroid",
      "displayName": "ARIP | \"镇定\" 复合前握把",
      "nameZh": "ARIP | \"镇定\" 复合前握把",
      "nameEn": "ARIP | \"Sangfroid\" Complex Foregrip",
      "nameKey": "arip.attachment.sangfroid.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/arip/index/attachments/sangfroid.json"
    },
    {
      "id": "arip:sangfroid_shield",
      "displayName": "ARIP | \"镇定·盘踞\" 复合前握把 附加盾牌",
      "nameZh": "ARIP | \"镇定·盘踞\" 复合前握把 附加盾牌",
      "nameEn": "ARIP | \"Sangfroid\" Complex Foregrip (Added on: Ballistic-proof Shield)",
      "nameKey": "arip.attachment.sangfroid_shield.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/arip/index/attachments/sangfroid_shield.json"
    },
    {
      "id": "arip:tacit",
      "displayName": "ARIP | \"默示启霆闪\" 缓冲偏力装置 (原型)",
      "nameZh": "ARIP | \"默示启霆闪\" 缓冲偏力装置 (原型)",
      "nameEn": "ARIP | \"Tacit\" Buffer (Prototype)",
      "nameKey": "arip.attachment.tacit.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/arip/index/attachments/tacit.json"
    },
    {
      "id": "arip:vicissitude",
      "displayName": "ARIP | \"变迁终归寂\" 复合抑制器 (原型)",
      "nameZh": "ARIP | \"变迁终归寂\" 复合抑制器 (原型)",
      "nameEn": "ARIP | \"Vicissitude\" Complex Suppressor (Prototype)",
      "nameKey": "arip.attachment.vicissitude.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/arip/index/attachments/vicissitude.json"
    },
    {
      "id": "asos:laser_3eir_dirv",
      "displayName": "三电红外 | DIR'指定红外'-V 激光指示辅助瞄准具",
      "nameZh": "三电红外 | DIR'指定红外'-V 激光指示辅助瞄准具",
      "nameEn": "3EIR | DesignateIR-V Tactical Laser Pointer",
      "nameKey": "asos.attachment.laser_3eir_dirv.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "laser",
      "indexPath": "data/asos/index/attachments/laser_3eir_dirv.json"
    },
    {
      "id": "asos:laser_eotech_ogl",
      "displayName": "电子光学科技 | OGL'枪上激光' 激光指示辅助瞄准具",
      "nameZh": "电子光学科技 | OGL'枪上激光' 激光指示辅助瞄准具",
      "nameEn": "EOTech | OGL Laser Pointer",
      "nameKey": "asos.attachment.laser_eotech_ogl.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "laser",
      "indexPath": "data/asos/index/attachments/laser_eotech_ogl.json"
    },
    {
      "id": "asos:laser_eotech_ogl_sight",
      "displayName": "电子光学科技 | OGL'枪上激光' 激光指示辅助瞄准具 (瞄具槽位)",
      "nameZh": "电子光学科技 | OGL'枪上激光' 激光指示辅助瞄准具 (瞄具槽位)",
      "nameEn": "EOTech | OGL Laser Pointer (Sight Slot)",
      "nameKey": "asos.attachment.laser_eotech_ogl_sight.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/laser_eotech_ogl_sight.json"
    },
    {
      "id": "asos:laser_vwt_c5",
      "displayName": "翠绿武器科技 | 新版 C5 手枪形制激光辅助瞄准器",
      "nameZh": "翠绿武器科技 | 新版 C5 手枪形制激光辅助瞄准器",
      "nameEn": "Viridian | New C5 Pistol Compact Laser Pointer",
      "nameKey": "asos.attachment.laser_vwt_c5.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "laser",
      "indexPath": "data/asos/index/attachments/laser_vwt_c5.json"
    },
    {
      "id": "asos:laser_vwt_c5l",
      "displayName": "翠绿武器科技 | 新版 C5L 手枪形制激光辅助瞄准器",
      "nameZh": "翠绿武器科技 | 新版 C5L 手枪形制激光辅助瞄准器",
      "nameEn": "Viridian | New C5L Pistol Compact Laser Pointer",
      "nameKey": "asos.attachment.laser_vwt_c5l.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "laser",
      "indexPath": "data/asos/index/attachments/laser_vwt_c5l.json"
    },
    {
      "id": "asos:sight_deon_d80hv56wtixgr",
      "displayName": "Deon光学技研 | March-X \"高级专家-广角\" \"Majesta\" 8-80x56mm 第二焦平面光学瞄准具",
      "nameZh": "Deon光学技研 | March-X \"高级专家-广角\" \"Majesta\" 8-80x56mm 第二焦平面光学瞄准具",
      "nameEn": "Deon Optical Design | March-X \"Majesta\" 8-80x56mm SFP Optical Sight",
      "nameKey": "asos.attachment.sight_deon_d80hv56wtixgr.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_deon_d80hv56wtixgr.json"
    },
    {
      "id": "asos:sight_eotech_eflx",
      "displayName": "电子光学科技 | EFLX 微型反射式瞄准具",
      "nameZh": "电子光学科技 | EFLX 微型反射式瞄准具",
      "nameEn": "EOTech | EFLX Mini Red Dot Sight",
      "nameKey": "asos.attachment.sight_eotech_eflx.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_eflx.json"
    },
    {
      "id": "asos:sight_eotech_eflx_pica",
      "displayName": "电子光学科技 | EFLX 微型反射式瞄准具 (规格: 皮轨)",
      "nameZh": "电子光学科技 | EFLX 微型反射式瞄准具 (规格: 皮轨)",
      "nameEn": "EOTech | EFLX Mini Red Dot Sight (Picatinny)",
      "nameKey": "asos.attachment.sight_eotech_eflx_pica.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_eflx_pica.json"
    },
    {
      "id": "asos:sight_eotech_eflx_scorpion",
      "displayName": "电子光学科技 | EFLX 微型反射式瞄准具 (规格: 格洛克)",
      "nameZh": "电子光学科技 | EFLX 微型反射式瞄准具 (规格: 格洛克)",
      "nameEn": "EOTech | EFLX Mini Red Dot Sight (Glock)",
      "nameKey": "asos.attachment.sight_eotech_eflx_scorpion.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_eflx_scorpion.json"
    },
    {
      "id": "asos:sight_eotech_hhs1",
      "displayName": "电子光学科技 | HHS-Ⅰ 全息衍射式瞄准具组合",
      "nameZh": "电子光学科技 | HHS-Ⅰ 全息衍射式瞄准具组合",
      "nameEn": "EOTech | HHS-I Combo Sights",
      "nameKey": "asos.attachment.sight_eotech_hhs1.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs1.json"
    },
    {
      "id": "asos:sight_eotech_hhs1_nozoom",
      "displayName": "电子光学科技 | HHS-Ⅰ 全息衍射式瞄准具组合 (放大具翻折)",
      "nameZh": "电子光学科技 | HHS-Ⅰ 全息衍射式瞄准具组合 (放大具翻折)",
      "nameEn": "EOTech | HHS-I Combo (Magnifier Switched)",
      "nameKey": "asos.attachment.sight_eotech_hhs1_nozoom.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs1_nozoom.json"
    },
    {
      "id": "asos:sight_eotech_hhs5_ogl_ftc",
      "displayName": "电子光学科技 | HHS-Ⅴ & OGL 全息衍射式瞄准具组合",
      "nameZh": "电子光学科技 | HHS-Ⅴ & OGL 全息衍射式瞄准具组合",
      "nameEn": "EOTech | HHS-V & OGL Combo Sights",
      "nameKey": "asos.attachment.sight_eotech_hhs5_ogl_ftc.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs5_ogl_ftc.json"
    },
    {
      "id": "asos:sight_eotech_hhs5_ogl_ftc_nozoom",
      "displayName": "电子光学科技 | HHS-Ⅴ & OGL 全息衍射式瞄准具组合 (放大具翻折)",
      "nameZh": "电子光学科技 | HHS-Ⅴ & OGL 全息衍射式瞄准具组合 (放大具翻折)",
      "nameEn": "EOTech | HHS-V & OGL Combo Sights (Magnifier Switched)",
      "nameKey": "asos.attachment.sight_eotech_hhs5_ogl_ftc_nozoom.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs5_ogl_ftc_nozoom.json"
    },
    {
      "id": "asos:sight_eotech_hhs6_hdl",
      "displayName": "电子光学科技 | HHS-Ⅵ 全息衍射式瞄准具组合",
      "nameZh": "电子光学科技 | HHS-Ⅵ 全息衍射式瞄准具组合",
      "nameEn": "EOTech | HHS-VI Combo Sights",
      "nameKey": "asos.attachment.sight_eotech_hhs6_hdl.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs6_hdl.json"
    },
    {
      "id": "asos:sight_eotech_hhs6_hdl_nozoom",
      "displayName": "电子光学科技 | HHS-Ⅵ 全息衍射式瞄准具组合 (放大具翻折)",
      "nameZh": "电子光学科技 | HHS-Ⅵ 全息衍射式瞄准具组合 (放大具翻折)",
      "nameEn": "EOTech | HHS-VI Combo Sights (Magnifier Switched)",
      "nameKey": "asos.attachment.sight_eotech_hhs6_hdl_nozoom.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs6_hdl_nozoom.json"
    },
    {
      "id": "asos:sight_eotech_hhs8",
      "displayName": "电子光学科技 | HHS-Ⅷ 全息衍射式瞄准具组合",
      "nameZh": "电子光学科技 | HHS-Ⅷ 全息衍射式瞄准具组合",
      "nameEn": "EOTech | HHS-VIII Combo Sights",
      "nameKey": "asos.attachment.sight_eotech_hhs8.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs8.json"
    },
    {
      "id": "asos:sight_eotech_hhs8_nozoom",
      "displayName": "电子光学科技 | HHS-Ⅷ 全息衍射式瞄准具组合 (放大具翻折)",
      "nameZh": "电子光学科技 | HHS-Ⅷ 全息衍射式瞄准具组合 (放大具翻折)",
      "nameEn": "EOTech | HHS-VIII Combo (Magnifier Switched)",
      "nameKey": "asos.attachment.sight_eotech_hhs8_nozoom.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_hhs8_nozoom.json"
    },
    {
      "id": "asos:sight_eotech_vudu_ffp_5t25x50",
      "displayName": "电子光学科技 | Vudu 5-25x50mm 光学瞄准具",
      "nameZh": "电子光学科技 | Vudu 5-25x50mm 光学瞄准具",
      "nameEn": "EOTech | Vudu 5-25x50mm FFP Optical Sight",
      "nameKey": "asos.attachment.sight_eotech_vudu_ffp_5t25x50.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_vudu_ffp_5t25x50.json"
    },
    {
      "id": "asos:sight_eotech_vudu_sfp_3t9x32",
      "displayName": "电子光学科技 | Vudu 3-9x32mm 第二焦平面光学瞄准具",
      "nameZh": "电子光学科技 | Vudu 3-9x32mm 第二焦平面光学瞄准具",
      "nameEn": "EOTech | Vudu 3-9x32mm SFP Optical Sight",
      "nameKey": "asos.attachment.sight_eotech_vudu_sfp_3t9x32.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_eotech_vudu_sfp_3t9x32.json"
    },
    {
      "id": "asos:sight_hilux_m20vts",
      "displayName": "海拉克斯 | \"马尔科姆\" VTS'古董寻靶瞄具' 20x 光学瞄准具",
      "nameZh": "海拉克斯 | \"马尔科姆\" VTS'古董寻靶瞄具' 20x 光学瞄准具",
      "nameEn": "Hi-Lux | Malcolm VTS 20x Optical Sight",
      "nameKey": "asos.attachment.sight_hilux_m20vts.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_hilux_m20vts.json"
    },
    {
      "id": "asos:sight_okp7",
      "displayName": "OKP-7 反射式瞄准具 (规格: 皮轨)",
      "nameZh": "OKP-7 反射式瞄准具 (规格: 皮轨)",
      "nameEn": "OKP-7 Picatinny Reflex Sight",
      "nameKey": "asos.attachment.sight_okp7.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_okp7.json"
    },
    {
      "id": "asos:sight_phantom_xlrd310",
      "displayName": "幻影 | XL RD310 反射式瞄准具 (分划板: 防空)",
      "nameZh": "幻影 | XL RD310 反射式瞄准具 (分划板: 防空)",
      "nameEn": "Phantom | XL RD310 Reflex Sight (Reticle: Anti-Air)",
      "nameKey": "asos.attachment.sight_phantom_xlrd310.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_phantom_xlrd310.json"
    },
    {
      "id": "asos:sight_si_sidewinder2",
      "displayName": "打击工业 | \"响尾蛇\" 二型 可二级翻折机械瞄准具组合",
      "nameZh": "打击工业 | \"响尾蛇\" 二型 可二级翻折机械瞄准具组合",
      "nameEn": "Strike Industries | Sidewinder2 Back-up Iron Sights",
      "nameKey": "asos.attachment.sight_si_sidewinder2.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_si_sidewinder2.json"
    },
    {
      "id": "asos:sight_smtbd_cm2hp_10t60x56",
      "displayName": "施密特与本德 | CM'竞赛大师'-Ⅱ \"高表现\" 10-60x56mm 光学瞄准具",
      "nameZh": "施密特与本德 | CM'竞赛大师'-Ⅱ \"高表现\" 10-60x56mm 光学瞄准具",
      "nameEn": "Schmidt & Bender | CMⅡ High Performance 10-60x56mm Optical Sight",
      "nameKey": "asos.attachment.sight_smtbd_cm2hp_10t60x56.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_smtbd_cm2hp_10t60x56.json"
    },
    {
      "id": "asos:sight_te_mrpro3x30ir",
      "displayName": "突鹰 | MR PRO 3X30 IR 光学瞄准具",
      "nameZh": "突鹰 | MR PRO 3X30 IR 光学瞄准具",
      "nameEn": "T-Eagle | MR PRO 3X30 IR Optical Sight",
      "nameKey": "asos.attachment.sight_te_mrpro3x30ir.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_te_mrpro3x30ir.json"
    },
    {
      "id": "asos:sight_vortex_razorhdg3_1t10x24",
      "displayName": "涡旋 | \"剃刀\" \"高清\" 三代 1-10x24mm 光学瞄准具",
      "nameZh": "涡旋 | \"剃刀\" \"高清\" 三代 1-10x24mm 光学瞄准具",
      "nameEn": "Vortex | Razor HD Gen III 1-10x24mm FFP Optical Sight",
      "nameKey": "asos.attachment.sight_vortex_razorhdg3_1t10x24.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_razorhdg3_1t10x24.json"
    },
    {
      "id": "asos:sight_vortex_razorhdg3_6t36x56",
      "displayName": "涡旋 | \"剃刀\" \"高清\" 三代 6-36x56mm 光学瞄准具",
      "nameZh": "涡旋 | \"剃刀\" \"高清\" 三代 6-36x56mm 光学瞄准具",
      "nameEn": "Vortex | Razor HD Gen III 6-36x56mm FFP Optical Sight",
      "nameKey": "asos.attachment.sight_vortex_razorhdg3_6t36x56.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_razorhdg3_6t36x56.json"
    },
    {
      "id": "asos:sight_vortex_venom_rd",
      "displayName": "涡旋 | \"毒液\" 微型反射式瞄准具",
      "nameZh": "涡旋 | \"毒液\" 微型反射式瞄准具",
      "nameEn": "Vortex | Venom Mini Red Dot Sight",
      "nameKey": "asos.attachment.sight_vortex_venom_rd.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_venom_rd.json"
    },
    {
      "id": "asos:sight_vortex_venom_rd_ambush",
      "displayName": "涡旋 | \"毒液\" 微型反射式瞄准具 (规格: 皮轨)",
      "nameZh": "涡旋 | \"毒液\" 微型反射式瞄准具 (规格: 皮轨)",
      "nameEn": "Vortex | Venom Mini Red Dot Sight (Picatinny)",
      "nameKey": "asos.attachment.sight_vortex_venom_rd_ambush.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_venom_rd_ambush.json"
    },
    {
      "id": "asos:sight_vortex_viper_rd",
      "displayName": "涡旋 | \"毒蛇\" 微型反射式瞄准具",
      "nameZh": "涡旋 | \"毒蛇\" 微型反射式瞄准具",
      "nameEn": "Vortex | Viper Mini Red Dot Sight",
      "nameKey": "asos.attachment.sight_vortex_viper_rd.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_viper_rd.json"
    },
    {
      "id": "asos:sight_vortex_viper_rd_rex",
      "displayName": "涡旋 | \"毒蛇\" 微型反射式瞄准具 (规格: 皮轨)",
      "nameZh": "涡旋 | \"毒蛇\" 微型反射式瞄准具 (规格: 皮轨)",
      "nameEn": "Vortex | Viper Mini Red Dot Sight (Picatinny)",
      "nameKey": "asos.attachment.sight_vortex_viper_rd_rex.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_viper_rd_rex.json"
    },
    {
      "id": "asos:sight_vortex_viper_rd_rex_painted",
      "displayName": "涡旋 | \"毒蛇\" 微型反射式瞄准具 (规格: 皮轨; 涂色: \"染天幻梦\")",
      "nameZh": "涡旋 | \"毒蛇\" 微型反射式瞄准具 (规格: 皮轨; 涂色: \"染天幻梦\")",
      "nameEn": "Vortex | Viper Mini Red Dot Sight (Picatinny)",
      "nameKey": "asos.attachment.sight_vortex_viper_rd_rex_painted.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_vortex_viper_rd_rex_painted.json"
    },
    {
      "id": "asos:sight_zs_cpt",
      "displayName": "蔡司 | \"紧凑点\" 微型反射式瞄准具",
      "nameZh": "蔡司 | \"紧凑点\" 微型反射式瞄准具",
      "nameEn": "Zeiss | Compact-Point Reflex Sight",
      "nameKey": "asos.attachment.sight_zs_cpt.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "scope",
      "indexPath": "data/asos/index/attachments/sight_zs_cpt.json"
    },
    {
      "id": "atea:bayonet_6kh2",
      "displayName": "图拉 | «6Kh2» 1955年制 剑形刺刀",
      "nameZh": "图拉 | «6Kh2» 1955年制 剑形刺刀",
      "nameEn": "Tula | «6Kh2» 1955 Bayonet",
      "nameKey": "atea.attachment.bayonet_6kh2.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/bayonet_6kh2.json"
    },
    {
      "id": "atea:bayonet_6kh4",
      "displayName": "伊热夫斯克 | «6Kh4» 1972年制 多功能刺刀",
      "nameZh": "伊热夫斯克 | «6Kh4» 1972年制 多功能刺刀",
      "nameEn": "Izhevsk | «6Kh4» 1972 Bayonet",
      "nameKey": "atea.attachment.bayonet_6kh4.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/bayonet_6kh4.json"
    },
    {
      "id": "atea:bayonet_6kh5",
      "displayName": "伊孜玛什 | «6Kh5» 1991年制 多功能刺刀 (安装于: AK-100 原厂制退器)",
      "nameZh": "伊孜玛什 | «6Kh5» 1991年制 多功能刺刀 (安装于: AK-100 原厂制退器)",
      "nameEn": "Izhmash | «6Kh5» 1991 Bayonet",
      "nameKey": "atea.attachment.bayonet_6kh5.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/bayonet_6kh5.json"
    },
    {
      "id": "atea:bayonet_sks_blade",
      "displayName": "图拉 | SKS-45配用 剑形刺刀",
      "nameZh": "图拉 | SKS-45配用 剑形刺刀",
      "nameEn": "Tula | SKS-45 Blade Bayonet",
      "nameKey": "atea.attachment.bayonet_sks_blade.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/bayonet_sks_blade.json"
    },
    {
      "id": "atea:buttstock_416c_contract",
      "displayName": "黑克勒与科赫 | HK416C 缓冲组件与枪托",
      "nameZh": "黑克勒与科赫 | HK416C 缓冲组件与枪托",
      "nameEn": "H&K | HK416C Buttstock",
      "nameKey": "atea.attachment.buttstock_416c_contract.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_416c_contract.json"
    },
    {
      "id": "atea:buttstock_416c_expand",
      "displayName": "黑克勒与科赫 | HK416C 缓冲组件与枪托 (延展配置)",
      "nameZh": "黑克勒与科赫 | HK416C 缓冲组件与枪托 (延展配置)",
      "nameEn": "H&K | HK416C Buttstock (Extended)",
      "nameKey": "atea.attachment.buttstock_416c_expand.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_416c_expand.json"
    },
    {
      "id": "atea:buttstock_ambidextrous",
      "displayName": "短剑 | \"向量\" 第2.1代 \"灵巧\" 功能枪托",
      "nameZh": "短剑 | \"向量\" 第2.1代 \"灵巧\" 功能枪托",
      "nameEn": "Kriss | Ambidextrous Folding Buttstock",
      "nameKey": "atea.attachment.buttstock_ambidextrous.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_ambidextrous.json"
    },
    {
      "id": "atea:buttstock_crab",
      "displayName": "TACCOM | \"可调增高托腮板枪托(CRAB)\" 枪托 (规格: M&P15-22)",
      "nameZh": "TACCOM | \"可调增高托腮板枪托(CRAB)\" 枪托 (规格: M&P15-22)",
      "nameEn": "TACCOM | Cheek Riser Adjustable Buttstock",
      "nameKey": "atea.attachment.buttstock_crab.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_crab.json"
    },
    {
      "id": "atea:buttstock_magpul_dtpr",
      "displayName": "马盖普 | DT-PR 枪托",
      "nameZh": "马盖普 | DT-PR 枪托",
      "nameEn": "Magpul | DT-PR Buttstock",
      "nameKey": "atea.attachment.buttstock_magpul_dtpr.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_magpul_dtpr.json"
    },
    {
      "id": "atea:buttstock_mxm_ccb",
      "displayName": "马克西姆防务 | CCB 缓冲组件与臂箍 (延展配置)",
      "nameZh": "马克西姆防务 | CCB 缓冲组件与臂箍 (延展配置)",
      "nameEn": "Maxim Defense | CCB Brace (Extended)",
      "nameKey": "atea.attachment.buttstock_mxm_ccb.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_mxm_ccb.json"
    },
    {
      "id": "atea:buttstock_mxm_ccs",
      "displayName": "马克西姆防务 | CCS 缓冲组件与枪托 (延展配置)",
      "nameZh": "马克西姆防务 | CCS 缓冲组件与枪托 (延展配置)",
      "nameEn": "Maxim Defense | CCS Buttstock (Extended)",
      "nameKey": "atea.attachment.buttstock_mxm_ccs.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_mxm_ccs.json"
    },
    {
      "id": "atea:buttstock_mxm_cqb6",
      "displayName": "马克西姆防务 | CQB 第六代 缓冲组件与枪托 (延展配置)",
      "nameZh": "马克西姆防务 | CQB 第六代 缓冲组件与枪托 (延展配置)",
      "nameEn": "Maxim Defense | CQB GEN:6 Buttstock (Extended)",
      "nameKey": "atea.attachment.buttstock_mxm_cqb6.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_mxm_cqb6.json"
    },
    {
      "id": "atea:buttstock_mxm_cqb7",
      "displayName": "马克西姆防务 | CQB 第七代 缓冲组件与枪托",
      "nameZh": "马克西姆防务 | CQB 第七代 缓冲组件与枪托",
      "nameEn": "Maxim Defense | CQB GEN:7 Buttstock",
      "nameKey": "atea.attachment.buttstock_mxm_cqb7.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_mxm_cqb7.json"
    },
    {
      "id": "atea:buttstock_mxm_exc",
      "displayName": "马克西姆防务 | CQB EXC 缓冲组件与臂箍 (延展配置)",
      "nameZh": "马克西姆防务 | CQB EXC 缓冲组件与臂箍 (延展配置)",
      "nameEn": "Maxim Defense | CQB EXC Brace (Extended)",
      "nameKey": "atea.attachment.buttstock_mxm_exc.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_mxm_exc.json"
    },
    {
      "id": "atea:buttstock_mxm_scw",
      "displayName": "马克西姆防务 | SCW 缓冲组件与枪托",
      "nameZh": "马克西姆防务 | SCW 缓冲组件与枪托",
      "nameEn": "Maxim Defense | SCW Buttstock",
      "nameKey": "atea.attachment.buttstock_mxm_scw.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_mxm_scw.json"
    },
    {
      "id": "atea:buttstock_pt1",
      "displayName": "泽宁特 | PT-1 功能枪托",
      "nameZh": "泽宁特 | PT-1 功能枪托",
      "nameEn": "Zenit | PT-1 Telescopic Buttstock",
      "nameKey": "atea.attachment.buttstock_pt1.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_pt1.json"
    },
    {
      "id": "atea:buttstock_pt1s",
      "displayName": "泽宁特 | PT-1S 功能枪托 (延展配置)",
      "nameZh": "泽宁特 | PT-1S 功能枪托 (延展配置)",
      "nameEn": "Zenit | PT-1S Telescopic Buttstock (Extended)",
      "nameKey": "atea.attachment.buttstock_pt1s.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_pt1s.json"
    },
    {
      "id": "atea:buttstock_pt3",
      "displayName": "泽宁特 | PT-3 功能枪托",
      "nameZh": "泽宁特 | PT-3 功能枪托",
      "nameEn": "Zenit | PT-3 Telescopic Buttstock",
      "nameKey": "atea.attachment.buttstock_pt3.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_pt3.json"
    },
    {
      "id": "atea:buttstock_pt3s",
      "displayName": "泽宁特 | PT-3S 功能枪托 (延展配置)",
      "nameZh": "泽宁特 | PT-3S 功能枪托 (延展配置)",
      "nameEn": "Zenit | PT-3S Telescopic Buttstock (Extended)",
      "nameKey": "atea.attachment.buttstock_pt3s.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_pt3s.json"
    },
    {
      "id": "atea:buttstock_si_viper1",
      "displayName": "打击工业 | \"毒蛇\" 一型 枪托",
      "nameZh": "打击工业 | \"毒蛇\" 一型 枪托",
      "nameEn": "Strike Industries | Viper MOD1 Buttstock",
      "nameKey": "atea.attachment.buttstock_si_viper1.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_si_viper1.json"
    },
    {
      "id": "atea:buttstock_si_viper2",
      "displayName": "打击工业 | \"毒蛇\" 二型 枪托",
      "nameZh": "打击工业 | \"毒蛇\" 二型 枪托",
      "nameEn": "Strike Industries | Viper MOD2 Buttstock",
      "nameKey": "atea.attachment.buttstock_si_viper2.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_si_viper2.json"
    },
    {
      "id": "atea:buttstock_si_vipercqb",
      "displayName": "打击工业 | \"毒蛇\" 近距离战斗型 枪托",
      "nameZh": "打击工业 | \"毒蛇\" 近距离战斗型 枪托",
      "nameEn": "Strike Industries | Viper CQB Buttstock",
      "nameKey": "atea.attachment.buttstock_si_vipercqb.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_si_vipercqb.json"
    },
    {
      "id": "atea:buttstock_tds",
      "displayName": "战术动理 | \"镂空\" 枪托",
      "nameZh": "战术动理 | \"镂空\" 枪托",
      "nameEn": "Tactical Dynamics | Skeletonized Buttstock",
      "nameKey": "atea.attachment.buttstock_tds.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_tds.json"
    },
    {
      "id": "atea:buttstock_zhukovs",
      "displayName": "马盖普 | \"朱可夫\"-S 功能枪托",
      "nameZh": "马盖普 | \"朱可夫\"-S 功能枪托",
      "nameEn": "Magpul | Zhukov-S Buttstock",
      "nameKey": "atea.attachment.buttstock_zhukovs.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "stock",
      "indexPath": "data/atea/index/attachments/buttstock_zhukovs.json"
    },
    {
      "id": "atea:grip_lb1",
      "displayName": "怪胎战术 | \"激光鸟\" 第一代 皮轨款 激光辅瞄具",
      "nameZh": "怪胎战术 | \"激光鸟\" 第一代 皮轨款 激光辅瞄具",
      "nameEn": "Monstrum Tactical | Laserbeak Picatinny Laser Sight",
      "nameKey": "atea.attachment.grip_lb1.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_lb1.json"
    },
    {
      "id": "atea:grip_rk1",
      "displayName": "泽宁特 | RK-1 垂直前握把",
      "nameZh": "泽宁特 | RK-1 垂直前握把",
      "nameEn": "Zenit | RK-1 Vertical Foregrip",
      "nameKey": "atea.attachment.grip_rk1.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_rk1.json"
    },
    {
      "id": "atea:grip_rk4",
      "displayName": "泽宁特 | RK-4 垂直前握把",
      "nameZh": "泽宁特 | RK-4 垂直前握把",
      "nameEn": "Zenit | RK-4 Vertical Foregrip",
      "nameKey": "atea.attachment.grip_rk4.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_rk4.json"
    },
    {
      "id": "atea:grip_rk5",
      "displayName": "泽宁特 | RK-5 垂直前握把",
      "nameZh": "泽宁特 | RK-5 垂直前握把",
      "nameEn": "Zenit | RK-5 Vertical Foregrip",
      "nameKey": "atea.attachment.grip_rk5.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_rk5.json"
    },
    {
      "id": "atea:grip_sf900a",
      "displayName": "神火 | M900A 垂直握把带手电",
      "nameZh": "神火 | M900A 垂直握把带手电",
      "nameEn": "Surefire | M900A Vertical Foregrip Weaponlight",
      "nameKey": "atea.attachment.grip_sf900a.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_sf900a.json"
    },
    {
      "id": "atea:grip_shift",
      "displayName": "弗缇斯 | Shift 斜握把",
      "nameZh": "弗缇斯 | Shift 斜握把",
      "nameEn": "Fortis | Shift Foregrip",
      "nameKey": "atea.attachment.grip_shift.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_shift.json"
    },
    {
      "id": "atea:grip_shiftshort",
      "displayName": "弗缇斯 | Shift 短版 小握把",
      "nameZh": "弗缇斯 | Shift 短版 小握把",
      "nameEn": "Fortis | Shift Short Foregrip",
      "nameKey": "atea.attachment.grip_shiftshort.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_shiftshort.json"
    },
    {
      "id": "atea:grip_sigurd",
      "displayName": "傲雷 | \"齐格鲁德\" 转角握把带手电",
      "nameZh": "傲雷 | \"齐格鲁德\" 转角握把带手电",
      "nameEn": "Olight | Sigurd Angled Foregrip Weaponlight",
      "nameKey": "atea.attachment.grip_sigurd.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_sigurd.json"
    },
    {
      "id": "atea:grip_utg_usa",
      "displayName": "UTG | \"超细线\" 皮轨款 转角前握把",
      "nameZh": "UTG | \"超细线\" 皮轨款 转角前握把",
      "nameEn": "UTG | Ultra Slim Picatinny Angled Foregrip",
      "nameKey": "atea.attachment.grip_utg_usa.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_utg_usa.json"
    },
    {
      "id": "atea:grip_utg_usv",
      "displayName": "UTG | \"超细线\" 皮轨款 垂直前握把",
      "nameZh": "UTG | \"超细线\" 皮轨款 垂直前握把",
      "nameEn": "UTG | Ultra Slim Picatinny Vertical Foregrip",
      "nameKey": "atea.attachment.grip_utg_usv.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_utg_usv.json"
    },
    {
      "id": "atea:grip_vector",
      "displayName": "短剑 | 垂直握把带阻手",
      "nameZh": "短剑 | 垂直握把带阻手",
      "nameEn": "Kriss | Vertical Foregrip with Intergrated Finger Stop",
      "nameKey": "atea.attachment.grip_vector.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_vector.json"
    },
    {
      "id": "atea:grip_zolfaqar_gc",
      "displayName": "防务工业组织 | \"佐勒菲卡尔剑\" 前握把组合",
      "nameZh": "防务工业组织 | \"佐勒菲卡尔剑\" 前握把组合",
      "nameEn": "DIO | Zolfaqar Combo Foregrips",
      "nameKey": "atea.attachment.grip_zolfaqar_gc.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_zolfaqar_gc.json"
    },
    {
      "id": "atea:grip_zolfaqar_gh",
      "displayName": "防务工业组织 | \"佐勒菲卡尔剑\" 转角握把",
      "nameZh": "防务工业组织 | \"佐勒菲卡尔剑\" 转角握把",
      "nameEn": "DIO | Zolfaqar Angled Grip",
      "nameKey": "atea.attachment.grip_zolfaqar_gh.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_zolfaqar_gh.json"
    },
    {
      "id": "atea:grip_zolfaqar_gp",
      "displayName": "防务工业组织 | \"佐勒菲卡尔剑\" 脚架握把",
      "nameZh": "防务工业组织 | \"佐勒菲卡尔剑\" 脚架握把",
      "nameEn": "DIO | Zolfaqar Gripod",
      "nameKey": "atea.attachment.grip_zolfaqar_gp.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "grip",
      "indexPath": "data/atea/index/attachments/grip_zolfaqar_gp.json"
    },
    {
      "id": "atea:muzzle_2a_x3",
      "displayName": "2A军械 | X3 双挡板 补偿器",
      "nameZh": "2A军械 | X3 双挡板 补偿器",
      "nameEn": "2A Armament | X3 Dual Baffle Compensator",
      "nameKey": "atea.attachment.muzzle_2a_x3.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_2a_x3.json"
    },
    {
      "id": "atea:muzzle_cb30",
      "displayName": "雷兽军械 | 30CB'紧凑制退器\" 制退器",
      "nameZh": "雷兽军械 | 30CB'紧凑制退器\" 制退器",
      "nameEn": "Thunder Beast Armament | 30CB Brake",
      "nameKey": "atea.attachment.muzzle_cb30.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_cb30.json"
    },
    {
      "id": "atea:muzzle_fd917",
      "displayName": "费舍研发 | FD917 抑制器",
      "nameZh": "费舍研发 | FD917 抑制器",
      "nameEn": "Fischer Development | FD917 Suppressor",
      "nameKey": "atea.attachment.muzzle_fd917.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_fd917.json"
    },
    {
      "id": "atea:muzzle_fd919",
      "displayName": "费舍研发 | FD919 抑制器",
      "nameZh": "费舍研发 | FD919 抑制器",
      "nameEn": "Fischer Development | FD919 Suppressor",
      "nameKey": "atea.attachment.muzzle_fd919.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_fd919.json"
    },
    {
      "id": "atea:muzzle_glock_doublediamond",
      "displayName": "双金刚石 | 格洛克规格 消焰器",
      "nameZh": "双金刚石 | 格洛克规格 消焰器",
      "nameEn": "Double Diamond | Glock Flash Hider",
      "nameKey": "atea.attachment.muzzle_glock_doublediamond.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_glock_doublediamond.json"
    },
    {
      "id": "atea:muzzle_hybrid46m",
      "displayName": "消音器公司 | \"混合\"-46M 多口径兼用 抑制器 (最大长度配置)",
      "nameZh": "消音器公司 | \"混合\"-46M 多口径兼用 抑制器 (最大长度配置)",
      "nameEn": "Silencer Co. | Hybrid-46M Multi-Caliber Suppressor (Extended)",
      "nameKey": "atea.attachment.muzzle_hybrid46m.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_hybrid46m.json"
    },
    {
      "id": "atea:muzzle_keymount",
      "displayName": "死寂消音器 | KeyMo 制退器",
      "nameZh": "死寂消音器 | KeyMo 制退器",
      "nameEn": "Dead Air Silencers | KeyMo Brake",
      "nameKey": "atea.attachment.muzzle_keymount.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_keymount.json"
    },
    {
      "id": "atea:muzzle_mxm_dskm",
      "displayName": "马克西姆防务 | DSK-M 抑制器",
      "nameZh": "马克西姆防务 | DSK-M 抑制器",
      "nameEn": "Maxim Defense | DSK-M Suppressor",
      "nameKey": "atea.attachment.muzzle_mxm_dskm.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_mxm_dskm.json"
    },
    {
      "id": "atea:muzzle_mxm_dsxm",
      "displayName": "马克西姆防务 | DSX-M 抑制器",
      "nameZh": "马克西姆防务 | DSX-M 抑制器",
      "nameEn": "Maxim Defense | DSX-M Suppressor",
      "nameKey": "atea.attachment.muzzle_mxm_dsxm.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_mxm_dsxm.json"
    },
    {
      "id": "atea:muzzle_mxm_hb",
      "displayName": "马克西姆防务 | \"憎恨制断\" 助燃抑焰罩",
      "nameZh": "马克西姆防务 | \"憎恨制断\" 助燃抑焰罩",
      "nameEn": "Maxim Defense | Hatebrake Muzzle Booster",
      "nameKey": "atea.attachment.muzzle_mxm_hb.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_mxm_hb.json"
    },
    {
      "id": "atea:muzzle_omega300",
      "displayName": "消音器公司 | \"欧米茄\"-300 DTM轻量版 抑制器",
      "nameZh": "消音器公司 | \"欧米茄\"-300 DTM轻量版 抑制器",
      "nameEn": "Silencer Co. | Omega-300 DTM Suppressor",
      "nameKey": "atea.attachment.muzzle_omega300.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_omega300.json"
    },
    {
      "id": "atea:muzzle_osd2",
      "displayName": "斯泰尔军械 | \"微风\" OSD Ⅱ 抑制器",
      "nameZh": "斯泰尔军械 | \"微风\" OSD Ⅱ 抑制器",
      "nameEn": "Styer Arms | Breezer OSD 2 Suppressor",
      "nameKey": "atea.attachment.muzzle_osd2.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_osd2.json"
    },
    {
      "id": "atea:muzzle_osprey45",
      "displayName": "消音器公司 | \"鱼鹰\"-45 第一代 抑制器",
      "nameZh": "消音器公司 | \"鱼鹰\"-45 第一代 抑制器",
      "nameEn": "Silencer Co. | Osprey-45 1.0 Suppressor",
      "nameKey": "atea.attachment.muzzle_osprey45.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_osprey45.json"
    },
    {
      "id": "atea:muzzle_osprey9",
      "displayName": "消音器公司 | \"鱼鹰\"-9 第二代 抑制器",
      "nameZh": "消音器公司 | \"鱼鹰\"-9 第二代 抑制器",
      "nameEn": "Silencer Co. | Osprey-9 2.0 Suppressor",
      "nameKey": "atea.attachment.muzzle_osprey9.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_osprey9.json"
    },
    {
      "id": "atea:muzzle_rotex5i",
      "displayName": "布鲁加与托梅 | \"罗泰克斯\"-Ⅴ-\"界面\" 枪口燃气重定向装置",
      "nameZh": "布鲁加与托梅 | \"罗泰克斯\"-Ⅴ-\"界面\" 枪口燃气重定向装置",
      "nameEn": "Brügger & Thomet | Rotex-V Interface Blast Deflector",
      "nameKey": "atea.attachment.muzzle_rotex5i.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_rotex5i.json"
    },
    {
      "id": "atea:muzzle_salvo12",
      "displayName": "消音器公司 | \"齐射\"-12 抑制器 (最大长度配置)",
      "nameZh": "消音器公司 | \"齐射\"-12 抑制器 (最大长度配置)",
      "nameEn": "Silencer Co. | Salvo-12 Suppressor (Maximum Length)",
      "nameKey": "atea.attachment.muzzle_salvo12.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_salvo12.json"
    },
    {
      "id": "atea:muzzle_si_cc",
      "displayName": "打击工业 | \"曲奇切刀\" 补偿器",
      "nameZh": "打击工业 | \"曲奇切刀\" 补偿器",
      "nameEn": "Strike Industries | Cookie Cutter Compensator",
      "nameKey": "atea.attachment.muzzle_si_cc.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_si_cc.json"
    },
    {
      "id": "atea:muzzle_si_md",
      "displayName": "打击工业 | \"质量驱动器\" 补偿器",
      "nameZh": "打击工业 | \"质量驱动器\" 补偿器",
      "nameEn": "Strike Industries | Mass Driver Compensator",
      "nameKey": "atea.attachment.muzzle_si_md.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_si_md.json"
    },
    {
      "id": "atea:muzzle_ultra5",
      "displayName": "雷兽军械 | \"超级\"-5 抑制器",
      "nameZh": "雷兽军械 | \"超级\"-5 抑制器",
      "nameEn": "Thunder Beast Armament | Ultra-5 Suppressor",
      "nameKey": "atea.attachment.muzzle_ultra5.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_ultra5.json"
    },
    {
      "id": "atea:muzzle_ultra50",
      "displayName": "雷兽军械 | \"超级\"-50 抑制器",
      "nameZh": "雷兽军械 | \"超级\"-50 抑制器",
      "nameEn": "Thunder Beast Armament | Ultra-50 Suppressor",
      "nameKey": "atea.attachment.muzzle_ultra50.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_ultra50.json"
    },
    {
      "id": "atea:muzzle_vector06",
      "displayName": "短剑 | \"向量\" 6.5英寸枪管 配用 MK5M 护手",
      "nameZh": "短剑 | \"向量\" 6.5英寸枪管 配用 MK5M 护手",
      "nameEn": "Kriss | Vector 6.5\" Barrel & MK5M Handguard",
      "nameKey": "atea.attachment.muzzle_vector06.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_vector06.json"
    },
    {
      "id": "atea:muzzle_vector10",
      "displayName": "短剑 | \"向量\" 10英寸枪管 配用 MK9 护手",
      "nameZh": "短剑 | \"向量\" 10英寸枪管 配用 MK9 护手",
      "nameEn": "Kriss | Vector 10\" Barrel & MK9 Handguard",
      "nameKey": "atea.attachment.muzzle_vector10.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_vector10.json"
    },
    {
      "id": "atea:muzzle_vector12",
      "displayName": "短剑 | \"向量\" 12英寸枪管 配用 MK11 护手",
      "nameZh": "短剑 | \"向量\" 12英寸枪管 配用 MK11 护手",
      "nameEn": "Kriss | Vector 12\" Barrel & MK11 Handguard",
      "nameKey": "atea.attachment.muzzle_vector12.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_vector12.json"
    },
    {
      "id": "atea:muzzle_vector14",
      "displayName": "短剑 | \"向量\" 14英寸枪管 配用 MK3 护手",
      "nameZh": "短剑 | \"向量\" 14英寸枪管 配用 MK3 护手",
      "nameEn": "Kriss | Vector 14\" Barrel & MK3 Handguard",
      "nameKey": "atea.attachment.muzzle_vector14.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_vector14.json"
    },
    {
      "id": "atea:muzzle_vector16",
      "displayName": "短剑 | \"向量\" CRB'卡宾型' 16英寸枪管及护罩 配用 MK1 护手",
      "nameZh": "短剑 | \"向量\" CRB'卡宾型' 16英寸枪管及护罩 配用 MK1 护手",
      "nameEn": "Kriss | Vector CRB 16\" Barrel and Shroud & MK1 Handguard",
      "nameKey": "atea.attachment.muzzle_vector16.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_vector16.json"
    },
    {
      "id": "atea:muzzle_vector18",
      "displayName": "短剑 | \"向量\" CRB'卡宾型' 加拿大版 18.6英寸枪管及护罩",
      "nameZh": "短剑 | \"向量\" CRB'卡宾型' 加拿大版 18.6英寸枪管及护罩",
      "nameEn": "Kriss | Vector CRB Canadian 18.6\" Barrel and Shroud",
      "nameKey": "atea.attachment.muzzle_vector18.name",
      "source": "tacz/ARIPS_ver.1.3.0.zip",
      "sources": [
        "tacz/ARIPS_ver.1.3.0.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/atea/index/attachments/muzzle_vector18.json"
    },
    {
      "id": "carl_gustaf_m4:ammo_mod_adm",
      "displayName": "84毫米霰弹（未完成）",
      "nameZh": "84毫米霰弹（未完成）",
      "nameEn": "ADM 401",
      "nameKey": "carl_gustaf_m4.attachment.ammo_mod_adm.name",
      "source": "tacz/carl_gustaf_m4_converted_fixed.zip",
      "sources": [
        "tacz/carl_gustaf_m4_converted_fixed.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/carl_gustaf_m4/index/attachments/ammo_mod_adm.json"
    },
    {
      "id": "carl_gustaf_m4:ammo_mod_asm",
      "displayName": "84毫米温压弹",
      "nameZh": "84毫米温压弹",
      "nameEn": "ASM 509",
      "nameKey": "carl_gustaf_m4.attachment.ammo_mod_asm.name",
      "source": "tacz/carl_gustaf_m4_converted_fixed.zip",
      "sources": [
        "tacz/carl_gustaf_m4_converted_fixed.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/carl_gustaf_m4/index/attachments/ammo_mod_asm.json"
    },
    {
      "id": "carl_gustaf_m4:ammo_mod_heat",
      "displayName": "84毫米串联高爆破甲弹",
      "nameZh": "84毫米串联高爆破甲弹",
      "nameEn": "HEAT 751",
      "nameKey": "carl_gustaf_m4.attachment.ammo_mod_heat.name",
      "source": "tacz/carl_gustaf_m4_converted_fixed.zip",
      "sources": [
        "tacz/carl_gustaf_m4_converted_fixed.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/carl_gustaf_m4/index/attachments/ammo_mod_heat.json"
    },
    {
      "id": "ccrp:ammo_mod_hap",
      "displayName": "复合弹头超穿弹",
      "nameZh": "复合弹头超穿弹",
      "nameEn": "Hyper Armor-Piercing Ammo",
      "nameKey": "ccrp.attachment.ammo_mod_hap.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/ccrp/index/attachments/ammo_mod_hap.json"
    },
    {
      "id": "ccrp:flash",
      "displayName": "[盾装]战术爆闪模块",
      "nameZh": "[盾装]战术爆闪模块",
      "nameEn": "Tactical Strobe Flashlight [Shield]",
      "nameKey": "ccrp.attachment.flash.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/flash.json"
    },
    {
      "id": "ccrp:grip_192",
      "displayName": "兵器装备集团 | 19式垂直握把",
      "nameZh": "兵器装备集团 | 19式垂直握把",
      "nameEn": "",
      "nameKey": "ccrp.attachment.grip_192.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/ccrp/index/attachments/grip_192.json"
    },
    {
      "id": "ccrp:grip_cqr",
      "displayName": "Hera Arms | 一代CQR 握把",
      "nameZh": "Hera Arms | 一代CQR 握把",
      "nameEn": "Hera Arms | CQR Grip Gen1",
      "nameKey": "ccrp.attachment.grip_cqr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/ccrp/index/attachments/grip_cqr.json"
    },
    {
      "id": "ccrp:laser_eotech_ogl",
      "displayName": "EOTech | OGL 镭射指示器",
      "nameZh": "EOTech | OGL 镭射指示器",
      "nameEn": "EOTech | OGL Laser",
      "nameKey": "ccrp.attachment.laser_eotech_ogl.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "laser",
      "indexPath": "data/ccrp/index/attachments/laser_eotech_ogl.json"
    },
    {
      "id": "ccrp:laser_peq15",
      "displayName": "L3 AN/PEQ-15镭射指示器",
      "nameZh": "L3 AN/PEQ-15镭射指示器",
      "nameEn": "L3 AN/PEQ-15 Laser",
      "nameKey": "ccrp.attachment.laser_peq15.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "laser",
      "indexPath": "data/ccrp/index/attachments/laser_peq15.json"
    },
    {
      "id": "ccrp:laser_samurai_edge",
      "displayName": "武士之刃 镭射指示器",
      "nameZh": "武士之刃 镭射指示器",
      "nameEn": "Samurai's Edge Laser",
      "nameKey": "ccrp.attachment.laser_samurai_edge.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "laser",
      "indexPath": "data/ccrp/index/attachments/laser_samurai_edge.json"
    },
    {
      "id": "ccrp:m14_bipod",
      "displayName": "M14A1两脚架",
      "nameZh": "M14A1两脚架",
      "nameEn": "M14A1 bipod",
      "nameKey": "ccrp.attachment.stock_m14_bipod",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "module",
      "indexPath": "data/ccrp/index/attachments/m14_bipod.json"
    },
    {
      "id": "ccrp:m14_cheek_support",
      "displayName": "M14托腮板",
      "nameZh": "M14托腮板",
      "nameEn": "M14 Cheek Support",
      "nameKey": "ccrp.attachment.m14_cheek_support.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/m14_cheek_support.json"
    },
    {
      "id": "ccrp:muzzle_apc_9k_silencer",
      "displayName": "APC 9K 集成一体式消音器",
      "nameZh": "APC 9K 集成一体式消音器",
      "nameEn": "APC 9K Integrated silencer",
      "nameKey": "ccrp.attachment.muzzle_apc_9k_silencer.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_apc_9k_silencer.json"
    },
    {
      "id": "ccrp:muzzle_deagle_long_barrel",
      "displayName": "IMI | 沙漠之鹰一体式消音器",
      "nameZh": "IMI | 沙漠之鹰一体式消音器",
      "nameEn": "Deagle Silencer",
      "nameKey": "ccrp.attachment.muzzle_deagle_long_barrel.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_deagle_long_barrel.json"
    },
    {
      "id": "ccrp:muzzle_dtk_1",
      "displayName": "泽宁特 | DTK-1制退器",
      "nameZh": "泽宁特 | DTK-1制退器",
      "nameEn": "Zenit | DTK-1",
      "nameKey": "ccrp.attachment.muzzle_dtk_1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_dtk_1.json"
    },
    {
      "id": "ccrp:muzzle_m110_silencer",
      "displayName": "骑士军械 | M110原厂消音器",
      "nameZh": "骑士军械 | M110原厂消音器",
      "nameEn": "KAC | M110 OEM Suppressor",
      "nameKey": "ccrp.attachment.muzzle_m110_silencer.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_m110_silencer.json"
    },
    {
      "id": "ccrp:muzzle_rotex5c",
      "displayName": "B&T | Rotex-V紧凑型消音器",
      "nameZh": "B&T | Rotex-V紧凑型消音器",
      "nameEn": "B&T | Rotex-V Combat Silencer",
      "nameKey": "ccrp.attachment.muzzle_rotex5c.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_rotex5c.json"
    },
    {
      "id": "ccrp:muzzle_silencer_192",
      "displayName": "兵器装备集团 | 19式消音器",
      "nameZh": "兵器装备集团 | 19式消音器",
      "nameEn": "",
      "nameKey": "ccrp.attachment.muzzle_silencer_192.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_silencer_192.json"
    },
    {
      "id": "ccrp:muzzle_silencer_hel_e4a",
      "displayName": "HEL | E4A 消音器",
      "nameZh": "HEL | E4A 消音器",
      "nameEn": "HEL | E4A Silencer",
      "nameKey": "ccrp.attachment.muzzle_silencer_hel_e4a.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_silencer_hel_e4a.json"
    },
    {
      "id": "ccrp:muzzle_silencer_samurai_edge",
      "displayName": "武士之刃 消音器",
      "nameZh": "武士之刃 消音器",
      "nameEn": "Samurai's Edge Silencer",
      "nameKey": "ccrp.attachment.muzzle_silencer_samurai_edge.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_silencer_samurai_edge.json"
    },
    {
      "id": "ccrp:muzzle_sr25_silencer",
      "displayName": "骑士军械 | SR25原厂消音器",
      "nameZh": "骑士军械 | SR25原厂消音器",
      "nameEn": "KAC | SR25 OEM Suppressor",
      "nameKey": "ccrp.attachment.muzzle_sr25_silencer.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_sr25_silencer.json"
    },
    {
      "id": "ccrp:muzzle_sr_3m_silencer",
      "displayName": "SR-3M 9x39消音器",
      "nameZh": "SR-3M 9x39消音器",
      "nameEn": "SR-3M 9x39 Silencer",
      "nameKey": "ccrp.attachment.muzzle_sr_3m_silencer.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_sr_3m_silencer.json"
    },
    {
      "id": "ccrp:muzzle_tacfire_mz1202",
      "displayName": "战术火力 | MZ1202 枪口补偿器",
      "nameZh": "战术火力 | MZ1202 枪口补偿器",
      "nameEn": "TacFire | MZ1202 Compensator",
      "nameKey": "ccrp.attachment.muzzle_tacfire_mz1202.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ccrp/index/attachments/muzzle_tacfire_mz1202.json"
    },
    {
      "id": "ccrp:scope_g28",
      "displayName": "施密特&本德 | 3-20x50 PM II 高精度瞄准镜",
      "nameZh": "施密特&本德 | 3-20x50 PM II 高精度瞄准镜",
      "nameEn": "Schmidt & Bender | 3-20x50 PM II Scope",
      "nameKey": "ccrp.attachment.scope_g28.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/scope_g28.json"
    },
    {
      "id": "ccrp:scope_xm157",
      "displayName": "Vortex | XM-157火控瞄准系统",
      "nameZh": "Vortex | XM-157火控瞄准系统",
      "nameEn": "Vortex | XM-157 scope",
      "nameKey": "ccrp.attachment.scope_xm157.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/scope_xm157.json"
    },
    {
      "id": "ccrp:scope_xm157_black",
      "displayName": "Vortex | XM-157火控瞄准系统",
      "nameZh": "Vortex | XM-157火控瞄准系统",
      "nameEn": "Vortex | XM-157 scope",
      "nameKey": "ccrp.attachment.scope_xm157.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/scope_xm157_black.json"
    },
    {
      "id": "ccrp:sight_camg_frameless",
      "displayName": "齐玛军械 | 空洞 红点瞄具",
      "nameZh": "齐玛军械 | 空洞 红点瞄具",
      "nameEn": "CAMG | Hollow Red Dot",
      "nameKey": "ccrp.attachment.sight_camg_frameless.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/sight_camg_frameless.json"
    },
    {
      "id": "ccrp:sight_gbrs_ogl_viper",
      "displayName": "GBRS | 九头蛇 镭射瞄具套组",
      "nameZh": "GBRS | 九头蛇 镭射瞄具套组",
      "nameEn": "GBRS | Hydra Laser Sight Kit",
      "nameKey": "ccrp.attachment.sight_gbrs_ogl_viper.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/sight_gbrs_ogl_viper.json"
    },
    {
      "id": "ccrp:sight_mgl_scope",
      "displayName": "MGL 瞄准具",
      "nameZh": "MGL 瞄准具",
      "nameEn": "MGL Sight",
      "nameKey": "ccrp.attachment.sight_mgl_scope.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/sight_mgl_scope.json"
    },
    {
      "id": "ccrp:sight_romeo_8t",
      "displayName": "西格绍尔 | Romeo 8T 红点瞄准镜",
      "nameZh": "西格绍尔 | Romeo 8T 红点瞄准镜",
      "nameEn": "SIG Sauer | Romeo 8T",
      "nameKey": "ccrp.attachment.sight_romeo_8t.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/ccrp/index/attachments/sight_romeo_8t.json"
    },
    {
      "id": "ccrp:stock_191_default",
      "displayName": "ccrp.attachment.stock_191_default.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "ccrp.attachment.stock_191_default.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/ccrp/index/attachments/stock_191_default.json"
    },
    {
      "id": "ccrp:stock_192_gasknob",
      "displayName": "19式高速导气调节角度",
      "nameZh": "19式高速导气调节角度",
      "nameEn": "",
      "nameKey": "ccrp.attachment.stock_192_gasknob",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_192_gasknob.json"
    },
    {
      "id": "ccrp:stock_b5_precision_stock",
      "displayName": "B5 Systems | 精确射击枪托",
      "nameZh": "B5 Systems | 精确射击枪托",
      "nameEn": "B5 Systems | Precision Stock",
      "nameKey": "ccrp.attachment.b5_precision_stock.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_b5_precision_stock.json"
    },
    {
      "id": "ccrp:stock_b5_precision_stock_sand",
      "displayName": "B5 Systems | 精确射击枪托 沙色",
      "nameZh": "B5 Systems | 精确射击枪托 沙色",
      "nameEn": "B5 Systems | Precision Stock-Sand",
      "nameKey": "ccrp.attachment.b5_precision_stock_sand.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_b5_precision_stock_sand.json"
    },
    {
      "id": "ccrp:stock_cqr_gen1",
      "displayName": "Hera Arms | 一代CQR后托",
      "nameZh": "Hera Arms | 一代CQR后托",
      "nameEn": "Hera Arms | CQR Buttstock Gen1",
      "nameKey": "ccrp.attachment.stock_cqr_gen1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_cqr_gen1.json"
    },
    {
      "id": "ccrp:stock_dd_stock",
      "displayName": "丹尼尔防务 | DD后托",
      "nameZh": "丹尼尔防务 | DD后托",
      "nameEn": "Daniel Defense | DD Stock",
      "nameKey": "ccrp.attachment.dd_stock.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_dd_stock.json"
    },
    {
      "id": "ccrp:stock_haenel",
      "displayName": "黑内尔 战术后托",
      "nameZh": "黑内尔 战术后托",
      "nameEn": "HAENEL Stock",
      "nameKey": "ccrp.attachment.stock_haenel.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_haenel.json"
    },
    {
      "id": "ccrp:stock_haenel_sand",
      "displayName": "黑内尔 战术后托 | 沙色",
      "nameZh": "黑内尔 战术后托 | 沙色",
      "nameEn": "HAENEL Stock TAN",
      "nameKey": "ccrp.attachment.stock_haenel_sand.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_haenel_sand.json"
    },
    {
      "id": "ccrp:stock_hera_cqr_gen2",
      "displayName": "Hera Arms | 二代CQR后托 ",
      "nameZh": "Hera Arms | 二代CQR后托 ",
      "nameEn": "Hera Arms | CQR Buttstock Gen2",
      "nameKey": "ccrp.attachment.stock_hera_cqr_gen2.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_hera_cqr_gen2.json"
    },
    {
      "id": "ccrp:stock_hk_e1",
      "displayName": "HK | E1重型枪托",
      "nameZh": "HK | E1重型枪托",
      "nameEn": "HK | E1 Heavy Buttstock",
      "nameKey": "ccrp.attachment.stock_hk_e1.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_hk_e1.json"
    },
    {
      "id": "ccrp:stock_sa58_heavy",
      "displayName": "DSA | B.R.S 重型枪托",
      "nameZh": "DSA | B.R.S 重型枪托",
      "nameEn": "DSA | B.R.S Heavy Stock",
      "nameKey": "ccrp.attachment.stock_sa58_heavy",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_sa58_heavy.json"
    },
    {
      "id": "ccrp:stock_scar_sc",
      "displayName": "FN赫斯塔尔 | SCAR SC伸缩枪托（折叠）",
      "nameZh": "FN赫斯塔尔 | SCAR SC伸缩枪托（折叠）",
      "nameEn": "FN | SCAR SC Stock (Fold)",
      "nameKey": "ccrp.attachment.sc_stock.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_scar_sc.json"
    },
    {
      "id": "ccrp:stock_scar_sc_extend",
      "displayName": "FN赫斯塔尔 | SCAR SC伸缩枪托（展开）",
      "nameZh": "FN赫斯塔尔 | SCAR SC伸缩枪托（展开）",
      "nameEn": "FN | SCAR SC Stock (Extend)",
      "nameKey": "ccrp.attachment.sc_stock_extend.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/ccrp/index/attachments/stock_scar_sc_extend.json"
    },
    {
      "id": "ccrp:weapon_m203",
      "displayName": "M203 下挂式榴弹发射器",
      "nameZh": "M203 下挂式榴弹发射器",
      "nameEn": "M203 Underbarrel Grenade Launcher",
      "nameKey": "ccrp.attachment.weapon_m203.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/ccrp/index/attachments/weapon_m203.json"
    },
    {
      "id": "cib:3l",
      "displayName": "三棱刺刀",
      "nameZh": "三棱刺刀",
      "nameEn": "Three-edged bayonet",
      "nameKey": "cib.attachment.3l.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/3l.json"
    },
    {
      "id": "cib:change",
      "displayName": "枪械转换套件",
      "nameZh": "枪械转换套件",
      "nameEn": "Gun Conversion Kit",
      "nameKey": "cib.change.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/cib/index/attachments/change.json"
    },
    {
      "id": "cib:csol2",
      "displayName": "建设工业 CS/OL-2 全息瞄准镜",
      "nameZh": "建设工业 CS/OL-2 全息瞄准镜",
      "nameEn": "CS/OL-2 HCOG",
      "nameKey": "cib.attachment.csol2.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/csol2.json"
    },
    {
      "id": "cib:csol3",
      "displayName": "建设工业 CS/OL-3 多功能照准器",
      "nameZh": "建设工业 CS/OL-3 多功能照准器",
      "nameEn": "CS/OL-3 laser",
      "nameKey": "cib.attachment.csol3.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "laser",
      "indexPath": "data/cib/index/attachments/csol3.json"
    },
    {
      "id": "cib:csos20a",
      "displayName": "建设工业 CS/OS-20A 光学瞄具",
      "nameZh": "建设工业 CS/OS-20A 光学瞄具",
      "nameEn": "CS/OS-20A Scope",
      "nameKey": "cib.attachment.csos20a.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/csos20a.json"
    },
    {
      "id": "cib:dzj08s",
      "displayName": "cib.dzj08s.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "cib.dzj08s.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/dzj08s.json"
    },
    {
      "id": "cib:g3s",
      "displayName": "cib.g3s.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "cib.g3s.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/g3s.json"
    },
    {
      "id": "cib:grip_191",
      "displayName": "建设工业 QBZ-191 垂直握把",
      "nameZh": "建设工业 QBZ-191 垂直握把",
      "nameEn": "QBZ-191 Grip",
      "nameKey": "cib.attachment.grip_191.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "grip",
      "indexPath": "data/cib/index/attachments/grip_191.json"
    },
    {
      "id": "cib:grip_rk2",
      "displayName": "RK-2 垂直握把",
      "nameZh": "RK-2 垂直握把",
      "nameEn": "RK-2 Grip",
      "nameKey": "cib.attachment.grip_rk2.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "grip",
      "indexPath": "data/cib/index/attachments/grip_rk2.json"
    },
    {
      "id": "cib:laser_qsz92",
      "displayName": "建设工业 QSZ-92 小型激光指示器",
      "nameZh": "建设工业 QSZ-92 小型激光指示器",
      "nameEn": "QSZ-92 laser",
      "nameKey": "cib.attachment.laser_qsz92.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "laser",
      "indexPath": "data/cib/index/attachments/laser_qsz92.json"
    },
    {
      "id": "cib:ls321",
      "displayName": "昊阳公司 LS-321 激光指示器",
      "nameZh": "昊阳公司 LS-321 激光指示器",
      "nameEn": "Holosun LS-321 laser",
      "nameKey": "cib.attachment.ls321.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "laser",
      "indexPath": "data/cib/index/attachments/ls321.json"
    },
    {
      "id": "cib:muzzle_191",
      "displayName": "建设工业 QBZ-191 消音器",
      "nameZh": "建设工业 QBZ-191 消音器",
      "nameEn": "QBZ-191 silencer",
      "nameKey": "cib.attachment.muzzle_191.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/muzzle_191.json"
    },
    {
      "id": "cib:muzzle_aspirations",
      "displayName": "建设工业 问鼎 枪口制退器",
      "nameZh": "建设工业 问鼎 枪口制退器",
      "nameEn": "Aspirations Brake",
      "nameKey": "cib.attachment.muzzle_aspirations.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/muzzle_aspirations.json"
    },
    {
      "id": "cib:muzzle_csld3",
      "displayName": "建设工业 CS/LD-3 消音器",
      "nameZh": "建设工业 CS/LD-3 消音器",
      "nameEn": "CS/LD-3 silencer",
      "nameKey": "cib.attachment.muzzle_csld3.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/muzzle_csld3.json"
    },
    {
      "id": "cib:muzzle_firmament",
      "displayName": "建设工业 天穹 枪口制退器",
      "nameZh": "建设工业 天穹 枪口制退器",
      "nameEn": "Sky Brake",
      "nameKey": "cib.attachment.muzzle_firmament.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/muzzle_firmament.json"
    },
    {
      "id": "cib:muzzle_silencer_shotgun",
      "displayName": "霰弹枪消音器",
      "nameZh": "霰弹枪消音器",
      "nameEn": "Shotgun silencer",
      "nameKey": "cib.attachment.muzzle_silencer_shotgun.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/muzzle_silencer_shotgun.json"
    },
    {
      "id": "cib:muzzle_silencer_shotgun_1",
      "displayName": "霰弹枪消音器",
      "nameZh": "霰弹枪消音器",
      "nameEn": "Shotgun silencer",
      "nameKey": "cib.attachment.muzzle_silencer_shotgun_1.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/muzzle_silencer_shotgun_1.json"
    },
    {
      "id": "cib:qbz191_bayonet",
      "displayName": "建设工业 QNL-191 刺刀",
      "nameZh": "建设工业 QNL-191 刺刀",
      "nameEn": "QNL-191 bayonet",
      "nameKey": "cib.attachment.qbz191_bayonet.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/qbz191_bayonet.json"
    },
    {
      "id": "cib:qbz95_bayonet",
      "displayName": "北方工业 QNL-95 刺刀",
      "nameZh": "北方工业 QNL-95 刺刀",
      "nameEn": "QNL-95 bayonet",
      "nameKey": "cib.attachment.qbz95_bayonet.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/qbz95_bayonet.json"
    },
    {
      "id": "cib:qcw05_js9_muffler",
      "displayName": "建设工业 QCW-05/JS-9 专用消音器",
      "nameZh": "建设工业 QCW-05/JS-9 专用消音器",
      "nameEn": "QCW-05/JS-9 silencer",
      "nameKey": "cib.attachment.qcw05_js9_muffler.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/qcw05_js9_muffler.json"
    },
    {
      "id": "cib:qmd131",
      "displayName": "资江机器 QMD-131 光电瞄准镜",
      "nameZh": "资江机器 QMD-131 光电瞄准镜",
      "nameEn": "QMD-131 Photoelectric sight",
      "nameKey": "cib.attachment.qmd131.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/qmd131.json"
    },
    {
      "id": "cib:qmk171",
      "displayName": "建设工业 QMK-171 A型 光学瞄具",
      "nameZh": "建设工业 QMK-171 A型 光学瞄具",
      "nameEn": "QMK-171 A Scope",
      "nameKey": "cib.attachment.qmk171.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/qmk171.json"
    },
    {
      "id": "cib:qmk191",
      "displayName": "建设工业 QMK-191 光学瞄具",
      "nameZh": "建设工业 QMK-191 光学瞄具",
      "nameEn": "QMK-191 Scope",
      "nameKey": "cib.attachment.qmk191.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/qmk191.json"
    },
    {
      "id": "cib:qmk201",
      "displayName": "建设工业 QMK-201 光学瞄具",
      "nameZh": "建设工业 QMK-201 光学瞄具",
      "nameEn": "QMK-201 Scope",
      "nameKey": "cib.attachment.qmk201.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/qmk201.json"
    },
    {
      "id": "cib:qmk204",
      "displayName": "建设工业 QMK-204 光学瞄具",
      "nameZh": "建设工业 QMK-204 光学瞄具",
      "nameEn": "QMK-204 Scope",
      "nameKey": "cib.attachment.qmk204.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/qmk204.json"
    },
    {
      "id": "cib:qmq171",
      "displayName": "建设工业 QMQ-171 全息瞄准镜",
      "nameZh": "建设工业 QMQ-171 全息瞄准镜",
      "nameEn": "QMQ-171 HCOG",
      "nameKey": "cib.attachment.qmq171.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/qmq171.json"
    },
    {
      "id": "cib:sight_coyote_bf4",
      "displayName": "Coyote 红点瞄准镜丨战地风云4",
      "nameZh": "Coyote 红点瞄准镜丨战地风云4",
      "nameEn": "Coyote Sight丨Battlefield 4",
      "nameKey": "cib.attachment.sight_coyote_bf4.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/sight_coyote_bf4.json"
    },
    {
      "id": "cib:sight_hkk_02",
      "displayName": "祺丰海派 02 数字化瞄准镜",
      "nameZh": "祺丰海派 02 数字化瞄准镜",
      "nameEn": "Digital gun sight-02",
      "nameKey": "cib.attachment.sight_hkk_02.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/sight_hkk_02.json"
    },
    {
      "id": "cib:type30",
      "displayName": "明治三十年式刺刀",
      "nameZh": "明治三十年式刺刀",
      "nameEn": "Arisaka Type30 bayonet",
      "nameKey": "cib.attachment.type30.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/type30.json"
    },
    {
      "id": "cib:usp_tactical_silencer",
      "displayName": "HK-USP 战术型手枪消音器",
      "nameZh": "HK-USP 战术型手枪消音器",
      "nameEn": "HK-USP TACTICAL silencer",
      "nameKey": "cib.attachment.usp_tactical_silencer.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cib/index/attachments/usp_tactical_silencer.json"
    },
    {
      "id": "cib:yma88",
      "displayName": "北方工业 YMA QBU-88 微光瞄具",
      "nameZh": "北方工业 YMA QBU-88 微光瞄具",
      "nameEn": "YMA QBU-88 Scope",
      "nameKey": "cib.attachment.yma88.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/yma88.json"
    },
    {
      "id": "cib:yma95_1",
      "displayName": "北方工业 YMA QBZ-95-1 光学瞄具",
      "nameZh": "北方工业 YMA QBZ-95-1 光学瞄具",
      "nameEn": "YMA QBZ-95-1 Scope",
      "nameKey": "cib.attachment.yma95_1.name",
      "source": "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip",
      "sources": [
        "tacz/[Tacz1.1.7+]CIBR_GunsPack_v0.3_1.1.7.zip"
      ],
      "type": "scope",
      "indexPath": "data/cib/index/attachments/yma95_1.json"
    },
    {
      "id": "classicr:ammo_mod_rubber_bullet",
      "displayName": "Detonics丨.45 橡胶弹",
      "nameZh": "Detonics丨.45 橡胶弹",
      "nameEn": "Detonics | .45 Rubber Bullet",
      "nameKey": "classicr.attachment.ammo_mod_rubber_bullet.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/classicr/index/attachments/ammo_mod_rubber_bullet.json"
    },
    {
      "id": "classicr:ammo_mod_snake_shot",
      "displayName": ".357 穿甲燃烧弹",
      "nameZh": ".357 穿甲燃烧弹",
      "nameEn": ".357Magnum Snake Shot",
      "nameKey": "classicr.attachment.ammo_mod_snake_shot.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/classicr/index/attachments/ammo_mod_snake_shot.json"
    },
    {
      "id": "classicr:b5_stock_sand",
      "displayName": "军科 B5 枪托丨沙色",
      "nameZh": "军科 B5 枪托丨沙色",
      "nameEn": "B5 Stock | TAN",
      "nameKey": "classicr.attachment.b5_stock_sand.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/classicr/index/attachments/b5_stock_sand.json"
    },
    {
      "id": "classicr:grip_kac_sand",
      "displayName": "骑士 KAC 握把丨沙色",
      "nameZh": "骑士 KAC 握把丨沙色",
      "nameEn": "KAC Grip | TAN",
      "nameKey": "classicr.attachment.grip_kac_sand.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/classicr/index/attachments/grip_kac_sand.json"
    },
    {
      "id": "classicr:grip_rvg_railless",
      "displayName": "RVG 握把（无导轨夹）",
      "nameZh": "RVG 握把（无导轨夹）",
      "nameEn": "RVG grip(without Rail clamp)",
      "nameKey": "classicr.attachment.grip_rvg_railless.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/classicr/index/attachments/grip_rvg_railless.json"
    },
    {
      "id": "classicr:grip_shift",
      "displayName": "Fortis丨Shift 两用轻型握把",
      "nameZh": "Fortis丨Shift 两用轻型握把",
      "nameEn": "Fortis | Shift Vertical Grip",
      "nameKey": "classicr.attachment.grip_shift.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "grip",
      "indexPath": "data/classicr/index/attachments/grip_shift.json"
    },
    {
      "id": "classicr:muzzle_creaky_silencer",
      "displayName": "破旧简易消音器",
      "nameZh": "破旧简易消音器",
      "nameEn": "Creaky Silencer",
      "nameKey": "classicr.attachment.muzzle_creaky_silencer.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/classicr/index/attachments/muzzle_creaky_silencer.json"
    },
    {
      "id": "classicr:muzzle_silencer_aac",
      "displayName": "SOCOM 消音器丨沙色",
      "nameZh": "SOCOM 消音器丨沙色",
      "nameEn": "SOCOM Silencer | TAN",
      "nameKey": "classicr.attachment.muzzle_silencer_aac.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/classicr/index/attachments/muzzle_silencer_aac.json"
    },
    {
      "id": "classicr:muzzle_silencer_block",
      "displayName": "波奇消音器",
      "nameZh": "波奇消音器",
      "nameEn": "Bocchi Silencer",
      "nameKey": "classicr.attachment.muzzle_silencer_block.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/classicr/index/attachments/muzzle_silencer_block.json"
    },
    {
      "id": "classicr:muzzle_silencer_default",
      "displayName": "一体式消音器",
      "nameZh": "一体式消音器",
      "nameEn": "Integrated muffler",
      "nameKey": "classicr.attachment.muzzle_silencer_default.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/classicr/index/attachments/muzzle_silencer_default.json"
    },
    {
      "id": "classicr:muzzle_silencer_msr",
      "displayName": "雷明顿 | MSR消音器",
      "nameZh": "雷明顿 | MSR消音器",
      "nameEn": "Remington | MSR Silencer",
      "nameKey": "classicr.attachment.muzzle_silencer_msr.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/classicr/index/attachments/muzzle_silencer_msr.json"
    },
    {
      "id": "classicr:sight_canted_laser",
      "displayName": "夜袭者斜置激光",
      "nameZh": "夜袭者斜置激光",
      "nameEn": "Night Raider Canted Laser",
      "nameKey": "classicr.attachment.sight_canted_laser.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/classicr/index/attachments/sight_canted_laser.json"
    },
    {
      "id": "classicr:sight_red_dot",
      "displayName": "ROMEOZero-Pro 1x30mm丨红点瞄准镜",
      "nameZh": "ROMEOZero-Pro 1x30mm丨红点瞄准镜",
      "nameEn": "ROMEOZero-Pro 1x30mm | Red dot",
      "nameKey": "classicr.attachment.sight_red_dot.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "scope",
      "indexPath": "data/classicr/index/attachments/sight_red_dot.json"
    },
    {
      "id": "classicr:xm7_silencer",
      "displayName": "XM7 消音器",
      "nameZh": "XM7 消音器",
      "nameEn": "XM7 Silencer",
      "nameKey": "classicr.attachment.xm7_silencer.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/classicr/index/attachments/xm7_silencer.json"
    },
    {
      "id": "classicr:xm7_stock",
      "displayName": "classicr.attachment.xm7_stock.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "classicr.attachment.xm7_stock.name",
      "source": "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip",
      "sources": [
        "tacz/[TACZ1.1.8+Arcana]ClassicRCCRP 1.1.6_release.zip"
      ],
      "type": "stock",
      "indexPath": "data/classicr/index/attachments/xm7_stock.json"
    },
    {
      "id": "cpse:ammo_mod_speed",
      "displayName": "高速弹",
      "nameZh": "高速弹",
      "nameEn": "Fast Ammo",
      "nameKey": "cpse.attachment.ammo_mod_speed.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/cpse/index/attachments/ammo_mod_speed.json"
    },
    {
      "id": "cpse:ammo_mod_w",
      "displayName": "钨制弹头",
      "nameZh": "钨制弹头",
      "nameEn": "Tungsten Warheads",
      "nameKey": "cpse.attachment.ammo_mod_w.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/cpse/index/attachments/ammo_mod_w.json"
    },
    {
      "id": "cpse:muzzle_silencer_bwind",
      "displayName": "破风.50口径消音器",
      "nameZh": "破风.50口径消音器",
      "nameEn": "",
      "nameKey": "cpse.attachment.muzzle_silencer_bwind.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cpse/index/attachments/muzzle_silencer_bwind.json"
    },
    {
      "id": "cpse:muzzle_silencer_quiet",
      "displayName": "静谧.50口径消音器",
      "nameZh": "静谧.50口径消音器",
      "nameEn": "",
      "nameKey": "cpse.attachment.muzzle_silencer_quiet.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/cpse/index/attachments/muzzle_silencer_quiet.json"
    },
    {
      "id": "cpse:scope_m7xi",
      "displayName": "视得乐 M7Xi 4/28x 光学瞄具",
      "nameZh": "视得乐 M7Xi 4/28x 光学瞄具",
      "nameEn": "STEINER M7xi 4/28x Scope",
      "nameKey": "cpse.attachment.scope_m7xi.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/cpse/index/attachments/scope_m7xi.json"
    },
    {
      "id": "cpse:scope_pm2_25x",
      "displayName": "施密特本德 PM II 5/25x 光学瞄具",
      "nameZh": "施密特本德 PM II 5/25x 光学瞄具",
      "nameEn": "Schmidt&Bender PM II 5/25x Scope",
      "nameKey": "cpse.attachment.scope_pm2_25x.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/cpse/index/attachments/scope_pm2_25x.json"
    },
    {
      "id": "cpse:scope_spyglass",
      "displayName": "焊接望远镜",
      "nameZh": "焊接望远镜",
      "nameEn": "",
      "nameKey": "cpse.attachment.scope_spyglass.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/cpse/index/attachments/scope_spyglass.json"
    },
    {
      "id": "cpse:scope_tango",
      "displayName": "西格绍尔 Tango-MSR 速瞄",
      "nameZh": "西格绍尔 Tango-MSR 速瞄",
      "nameEn": "SIG Tango-MSR Scope",
      "nameKey": "cpse.attachment.scope_tango.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/cpse/index/attachments/scope_tango.json"
    },
    {
      "id": "cpse:scope_vari_13",
      "displayName": "刘波尔德 Vari VIII 6.5/20x 光学瞄具",
      "nameZh": "刘波尔德 Vari VIII 6.5/20x 光学瞄具",
      "nameEn": "Leupold Vari VIII 6.5/20x Scope",
      "nameKey": "cpse.attachment.scope_vari_13.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/cpse/index/attachments/scope_vari_13.json"
    },
    {
      "id": "cpse:stock_aics",
      "displayName": "精密国际 AICS 定制枪托",
      "nameZh": "精密国际 AICS 定制枪托",
      "nameEn": "",
      "nameKey": "cpse.attachment.stock_aics.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/cpse/index/attachments/stock_aics.json"
    },
    {
      "id": "cpse:stock_cdx50_tac",
      "displayName": "CDX-50 TAC 轻便枪托",
      "nameZh": "CDX-50 TAC 轻便枪托",
      "nameEn": "",
      "nameKey": "cpse.attachment.stock_cdx50_tac.name",
      "source": "tacz/Complete Silence 0.0.2b hotfix.zip",
      "sources": [
        "tacz/Complete Silence 0.0.2b hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/cpse/index/attachments/stock_cdx50_tac.json"
    },
    {
      "id": "k16:pas21k",
      "displayName": "PAS-21 K",
      "nameZh": "",
      "nameEn": "PAS-21 K",
      "nameKey": "k16.attachment.pas21k.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/k16/index/attachments/pas21k.json"
    },
    {
      "id": "kpp:k7s",
      "displayName": "kpp.attachment.k7s.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "kpp.attachment.k7s.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/kpp/index/attachments/k7s.json"
    },
    {
      "id": "kpp:pvs11k",
      "displayName": "PVS-11K",
      "nameZh": "PVS-11K",
      "nameEn": "PVS-11K",
      "nameKey": "kpp.attachment.pvs11k.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/kpp/index/attachments/pvs11k.json"
    },
    {
      "id": "ra1k:939_silencer",
      "displayName": "Silencer for 9x39mm",
      "nameZh": "",
      "nameEn": "Silencer for 9x39mm",
      "nameKey": "ra1k.attachment.939.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ra1k/index/attachments/939_silencer.json"
    },
    {
      "id": "ra1k:cobra",
      "displayName": "Cobra sight",
      "nameZh": "",
      "nameEn": "Cobra sight",
      "nameKey": "ra1k.attachment.cobra.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/cobra.json"
    },
    {
      "id": "ra1k:cover",
      "displayName": "Default receiver cover",
      "nameZh": "",
      "nameEn": "Default receiver cover",
      "nameKey": "ra1k.attachment.cover.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/cover.json"
    },
    {
      "id": "ra1k:drum_mag",
      "displayName": "Drum Magazine",
      "nameZh": "",
      "nameEn": "Drum Magazine",
      "nameKey": "ra1k.attachment.drum_mag.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/ra1k/index/attachments/drum_mag.json"
    },
    {
      "id": "ra1k:fast_mag",
      "displayName": "Fast Magazine",
      "nameZh": "",
      "nameEn": "Fast Magazine",
      "nameKey": "ra1k.attachment.fast_mag.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/ra1k/index/attachments/fast_mag.json"
    },
    {
      "id": "ra1k:flip_mag",
      "displayName": "Flip Magazine",
      "nameZh": "",
      "nameEn": "Flip Magazine",
      "nameKey": "ra1k.attachment.flip_mag.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/ra1k/index/attachments/flip_mag.json"
    },
    {
      "id": "ra1k:fpx_sight_552",
      "displayName": "Sight Miltech 552",
      "nameZh": "",
      "nameEn": "Sight Miltech 552",
      "nameKey": "ra1k.attachment.fpx_sight_552.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/fpx_sight_552.json"
    },
    {
      "id": "ra1k:gp25",
      "displayName": "GP-25 underbarrel grenade launcher",
      "nameZh": "",
      "nameEn": "GP-25 underbarrel grenade launcher",
      "nameKey": "ra1k.attachment.gp25.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/ra1k/index/attachments/gp25.json"
    },
    {
      "id": "ra1k:handguard_green",
      "displayName": "Custom green handguard",
      "nameZh": "",
      "nameEn": "Custom green handguard",
      "nameKey": "ra1k.handguard.green.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_green.json"
    },
    {
      "id": "ra1k:handguard_green_laser",
      "displayName": "Custom green handguard with laser",
      "nameZh": "",
      "nameEn": "Custom green handguard with laser",
      "nameKey": "ra1k.handguard_laser.green.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_green_laser.json"
    },
    {
      "id": "ra1k:handguard_grey",
      "displayName": "Custom tactical handguard",
      "nameZh": "",
      "nameEn": "Custom tactical handguard",
      "nameKey": "ra1k.handguard.grey.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_grey.json"
    },
    {
      "id": "ra1k:handguard_grey_laser",
      "displayName": "Custom tactical handguard with laser",
      "nameZh": "",
      "nameEn": "Custom tactical handguard with laser",
      "nameKey": "ra1k.handguard_laser.grey.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_grey_laser.json"
    },
    {
      "id": "ra1k:handguard_red",
      "displayName": "Custom red handguard",
      "nameZh": "",
      "nameEn": "Custom red handguard",
      "nameKey": "ra1k.handguard.red.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_red.json"
    },
    {
      "id": "ra1k:handguard_red_laser",
      "displayName": "Custom red handguard with laser",
      "nameZh": "",
      "nameEn": "Custom red handguard with laser",
      "nameKey": "ra1k.handguard_laser.red.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_red_laser.json"
    },
    {
      "id": "ra1k:handguard_sand",
      "displayName": "Custom sand handguard",
      "nameZh": "",
      "nameEn": "Custom sand handguard",
      "nameKey": "ra1k.handguard.sand.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_sand.json"
    },
    {
      "id": "ra1k:handguard_sand_laser",
      "displayName": "Custom sand handguard with laser",
      "nameZh": "",
      "nameEn": "Custom sand handguard with laser",
      "nameKey": "ra1k.handguard_laser.sand.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_sand_laser.json"
    },
    {
      "id": "ra1k:handguard_tactical",
      "displayName": "Default tactical handguard",
      "nameZh": "",
      "nameEn": "Default tactical handguard",
      "nameKey": "ra1k.attachment.handguard_tactical.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/ra1k/index/attachments/handguard_tactical.json"
    },
    {
      "id": "ra1k:kit_lmg",
      "displayName": "Full auto module",
      "nameZh": "",
      "nameEn": "Full auto module",
      "nameKey": "ra1k.attachment.kit_lmg.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/ra1k/index/attachments/kit_lmg.json"
    },
    {
      "id": "ra1k:m203",
      "displayName": "M203 underbarrel grenade launcher",
      "nameZh": "",
      "nameEn": "M203 underbarrel grenade launcher",
      "nameKey": "ra1k.attachment.m203.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/ra1k/index/attachments/m203.json"
    },
    {
      "id": "ra1k:m590_mag",
      "displayName": "Mossberg 590 Magazine",
      "nameZh": "",
      "nameEn": "Mossberg 590 Magazine",
      "nameKey": "ra1k.attachment.m590_mag.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/ra1k/index/attachments/m590_mag.json"
    },
    {
      "id": "ra1k:okp7",
      "displayName": "OKP-7 sight",
      "nameZh": "",
      "nameEn": "OKP-7 sight",
      "nameKey": "ra1k.attachment.okp7.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/okp7.json"
    },
    {
      "id": "ra1k:scope_pso1",
      "displayName": "PSO-1 sniper scope",
      "nameZh": "",
      "nameEn": "PSO-1 sniper scope",
      "nameKey": "ra1k.attachment.pso1.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/scope_pso1.json"
    },
    {
      "id": "ra1k:sight_magnifier",
      "displayName": "Sight with magnifier",
      "nameZh": "",
      "nameEn": "Sight with magnifier",
      "nameKey": "ra1k.attachment.sight_magnifier.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/sight_magnifier.json"
    },
    {
      "id": "ra1k:sight_magnifier_off",
      "displayName": "Sight with magnifier (Switched)",
      "nameZh": "",
      "nameEn": "Sight with magnifier (Switched)",
      "nameKey": "ra1k.attachment.sight_magnifier_off.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/sight_magnifier_off.json"
    },
    {
      "id": "ra1k:sight_velociraptor",
      "displayName": "Sight for Velociraptor",
      "nameZh": "",
      "nameEn": "Sight for Velociraptor",
      "nameKey": "ra1k.attachment.sight_velociraptor.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/sight_velociraptor.json"
    },
    {
      "id": "ra1k:vudu_2x",
      "displayName": "Vudu 1-6x Scope (Switched)",
      "nameZh": "",
      "nameEn": "Vudu 1-6x Scope (Switched)",
      "nameKey": "ra1k.attachment.vudu_2x.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/vudu_2x.json"
    },
    {
      "id": "ra1k:vudu_6x",
      "displayName": "Vudu 1-6x Scope",
      "nameZh": "",
      "nameEn": "Vudu 1-6x Scope",
      "nameKey": "ra1k.attachment.vudu_6x.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/ra1k/index/attachments/vudu_6x.json"
    },
    {
      "id": "ra1k:xmag",
      "displayName": "Extended Magazine",
      "nameZh": "",
      "nameEn": "Extended Magazine",
      "nameKey": "ra1k.attachment.extended_mag.name",
      "source": "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip",
      "sources": [
        "tacz/Ra1k_gunpack_v2.0.4_hotfix.zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/ra1k/index/attachments/xmag.json"
    },
    {
      "id": "rfp:1p87",
      "displayName": "1P87 瞄准镜",
      "nameZh": "1P87 瞄准镜",
      "nameEn": "1P87 sight",
      "nameKey": "rfp.attachment.1p87.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/1p87.json"
    },
    {
      "id": "rfp:1p8790",
      "displayName": "1P87 1P90 组合瞄准镜",
      "nameZh": "1P87 1P90 组合瞄准镜",
      "nameEn": "1P87 1P90 Compound scope",
      "nameKey": "rfp.attachment.1p8790.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/1p8790.json"
    },
    {
      "id": "rfp:dbala2",
      "displayName": "DBAL-A2 战术激光",
      "nameZh": "DBAL-A2 战术激光",
      "nameEn": "",
      "nameKey": "rfp.attachment.dbala2.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "laser",
      "indexPath": "data/rfp/index/attachments/dbala2.json"
    },
    {
      "id": "rfp:dbala2_1",
      "displayName": "DBAL-A2 战术激光",
      "nameZh": "DBAL-A2 战术激光",
      "nameEn": "",
      "nameKey": "rfp.attachment.dbala2.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "laser",
      "indexPath": "data/rfp/index/attachments/dbala2_1.json"
    },
    {
      "id": "rfp:elcan_c79",
      "displayName": "ELCAN SpecterOS 3.4× C79A2",
      "nameZh": "ELCAN SpecterOS 3.4× C79A2",
      "nameEn": "ELCAN SpecterOS 3.4× C79A2",
      "nameKey": "rfp.attachment.elcan_c79.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/elcan_c79.json"
    },
    {
      "id": "rfp:elcan_c79_1",
      "displayName": "ELCAN SpecterOS 4× C79",
      "nameZh": "ELCAN SpecterOS 4× C79",
      "nameEn": "ELCAN SpecterOS 4× C79",
      "nameKey": "rfp.attachment.elcan_c79_1.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/elcan_c79_1.json"
    },
    {
      "id": "rfp:elcan_c79_2",
      "displayName": "ELCAN SpecterOS 4× C79",
      "nameZh": "ELCAN SpecterOS 4× C79",
      "nameEn": "ELCAN SpecterOS 4× C79",
      "nameKey": "rfp.attachment.elcan_c79_1.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/elcan_c79_2.json"
    },
    {
      "id": "rfp:elcan_c79_3",
      "displayName": "ELCAN SpecterOS 4× C79",
      "nameZh": "ELCAN SpecterOS 4× C79",
      "nameEn": "ELCAN SpecterOS 4× C79",
      "nameKey": "rfp.attachment.elcan_c79_1.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/elcan_c79_3.json"
    },
    {
      "id": "rfp:hd2scope",
      "displayName": "绝地潜兵2支援武器通用瞄准镜",
      "nameZh": "绝地潜兵2支援武器通用瞄准镜",
      "nameEn": "HelldiversⅡ support weapon scope",
      "nameKey": "rfp.attachment.hd2scope.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/hd2scope.json"
    },
    {
      "id": "rfp:mg43scope",
      "displayName": "MG43 专用瞄准镜",
      "nameZh": "MG43 专用瞄准镜",
      "nameEn": "MG43 Scope",
      "nameKey": "rfp.attachment.mg43scope.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/mg43scope.json"
    },
    {
      "id": "rfp:scope_acog_ta648",
      "displayName": "Trijicon ACOG TA648 6X48",
      "nameZh": "Trijicon ACOG TA648 6X48",
      "nameEn": "Trijicon ACOG TA648 6X48",
      "nameKey": "rfp.attachment.scope_acog_ta648.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/scope_acog_ta648.json"
    },
    {
      "id": "rfp:scope_acog_ta648_1",
      "displayName": "Trijicon ACOG TA648 6X48",
      "nameZh": "Trijicon ACOG TA648 6X48",
      "nameEn": "Trijicon ACOG TA648 6X48",
      "nameKey": "rfp.attachment.scope_acog_ta648.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/scope_acog_ta648_1.json"
    },
    {
      "id": "rfp:ta648_rmr",
      "displayName": "Trijicon ACOG TA648 6X48 with RMR",
      "nameZh": "",
      "nameEn": "Trijicon ACOG TA648 6X48 with RMR",
      "nameKey": "rfp.attachment.ta648_rmr.name",
      "source": "tacz/RFP v1.1.0 alpha 5.zip",
      "sources": [
        "tacz/RFP v1.1.0 alpha 5.zip"
      ],
      "type": "scope",
      "indexPath": "data/rfp/index/attachments/ta648_rmr.json"
    },
    {
      "id": "suffuse:ash12_silencer",
      "displayName": "ASh12 消音器",
      "nameZh": "ASh12 消音器",
      "nameEn": "ASh12 Suppressor",
      "nameKey": "suffuse.attachment.ash12_silencer.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/ash12_silencer.json"
    },
    {
      "id": "suffuse:cslr4_flash_hider",
      "displayName": "CS/LR4 枪口消焰器",
      "nameZh": "CS/LR4 枪口消焰器",
      "nameEn": "CS/LR4 Flash Hider",
      "nameKey": "suffuse.attachment.cslr4_flash_hider.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/cslr4_flash_hider.json"
    },
    {
      "id": "suffuse:cslr4_muzzle_brake",
      "displayName": "CS/LR4 枪口制退器",
      "nameZh": "CS/LR4 枪口制退器",
      "nameEn": "CS/LR4 Muzzle Break",
      "nameKey": "suffuse.attachment.cslr4_muzzle_brake.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/cslr4_muzzle_brake.json"
    },
    {
      "id": "suffuse:cslr4_suppressor",
      "displayName": "CS/LR4 枪口抑制器",
      "nameZh": "CS/LR4 枪口抑制器",
      "nameEn": "CS/LR4 Suppressor",
      "nameKey": "suffuse.attachment.cslr4_suppressor.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/cslr4_suppressor.json"
    },
    {
      "id": "suffuse:grip_bt10vbatlas",
      "displayName": "BT10 V8 Atlas 折叠伸缩脚架",
      "nameZh": "BT10 V8 Atlas 折叠伸缩脚架",
      "nameEn": "BT10 V8 Atlas Fold & Retractable Bipod",
      "nameKey": "suffuse.attachment.grip_bt10vbatlas.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_bt10vbatlas.json"
    },
    {
      "id": "suffuse:grip_flashlight",
      "displayName": "CAG Gangster 握把",
      "nameZh": "CAG Gangster 握把",
      "nameEn": "CAG Gangster Grip",
      "nameKey": "suffuse.attachment.grip_flashlight.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_flashlight.json"
    },
    {
      "id": "suffuse:grip_m203",
      "displayName": "M203 下挂垂直前握把",
      "nameZh": "M203 下挂垂直前握把",
      "nameEn": "M203 Vertical Grip",
      "nameKey": "suffuse.attachment.grip_m203.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_m203.json"
    },
    {
      "id": "suffuse:grip_m32a1",
      "displayName": "TD 快拆握把 沙色款",
      "nameZh": "TD 快拆握把 沙色款",
      "nameEn": "TD Quick-Detach Grip",
      "nameKey": "suffuse.attachment.grip_td.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_m32a1.json"
    },
    {
      "id": "suffuse:grip_rvg",
      "displayName": "Magpul RVG 前握把",
      "nameZh": "Magpul RVG 前握把",
      "nameEn": "Magpul RVG Grip",
      "nameKey": "suffuse.attachment.grip_rvg.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_rvg.json"
    },
    {
      "id": "suffuse:grip_td",
      "displayName": "TD 快拆握把 沙色款",
      "nameZh": "TD 快拆握把 沙色款",
      "nameEn": "TD Quick-Detach Grip",
      "nameKey": "suffuse.attachment.grip_td.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_td.json"
    },
    {
      "id": "suffuse:grip_td_black",
      "displayName": "TD 快拆握把 黑色款",
      "nameZh": "TD 快拆握把 黑色款",
      "nameEn": "TD Quick-Detach Grip .ver Black",
      "nameKey": "suffuse.attachment.grip_td_black.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_td_black.json"
    },
    {
      "id": "suffuse:grip_td_blue_grey",
      "displayName": "TD 快拆握把 灰蓝色款",
      "nameZh": "TD 快拆握把 灰蓝色款",
      "nameEn": "TD Quick-Detach Grip .ver Blue Grey",
      "nameKey": "suffuse.attachment.grip_td_blue_grey.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_td_blue_grey.json"
    },
    {
      "id": "suffuse:grip_td_green",
      "displayName": "TD 快拆握把 绿色款",
      "nameZh": "TD 快拆握把 绿色款",
      "nameEn": "TD Quick-Detach Grip .ver Green",
      "nameKey": "suffuse.attachment.grip_td_green.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_td_green.json"
    },
    {
      "id": "suffuse:grip_usgi_m249",
      "displayName": "M249 SAW 战术前握把",
      "nameZh": "M249 SAW 战术前握把",
      "nameEn": "M249 SAW Tactical Grip",
      "nameKey": "suffuse.attachment.grip_usgi_m249.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/grip_usgi_m249.json"
    },
    {
      "id": "suffuse:laser_an_peq_2a",
      "displayName": "AN/PEQ-2 战术镭射",
      "nameZh": "AN/PEQ-2 战术镭射",
      "nameEn": "",
      "nameKey": "suffuse.attachment.laser_an_peq_2a",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/suffuse/index/attachments/laser_an_peq_2a.json"
    },
    {
      "id": "suffuse:laser_dbala2",
      "displayName": "DBAL-A2 战术镭射",
      "nameZh": "DBAL-A2 战术镭射",
      "nameEn": "DBAL-A2 Tactical Laser Device",
      "nameKey": "suffuse.attachment.laser_dbala2.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/suffuse/index/attachments/laser_dbala2.json"
    },
    {
      "id": "suffuse:laser_pistol",
      "displayName": "手枪式紧凑镭射",
      "nameZh": "手枪式紧凑镭射",
      "nameEn": "Pistol Compact Laser Device",
      "nameKey": "suffuse.attachment.laser_pistol",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/suffuse/index/attachments/laser_pistol.json"
    },
    {
      "id": "suffuse:laser_zenitcoperst3",
      "displayName": "泽宁特 Perst-3 战术镭射",
      "nameZh": "泽宁特 Perst-3 战术镭射",
      "nameEn": "Zenit Perst-3 Tactical Laser Device",
      "nameKey": "suffuse.attachment.laser_zenitcoperst3.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "laser",
      "indexPath": "data/suffuse/index/attachments/laser_zenitcoperst3.json"
    },
    {
      "id": "suffuse:m7_silencer",
      "displayName": "M7 消音器",
      "nameZh": "M7 消音器",
      "nameEn": "M7 Suppressor",
      "nameKey": "suffuse.attachment.m7_silencer.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/m7_silencer.json"
    },
    {
      "id": "suffuse:mg338_silencer",
      "displayName": "MG338 消音器",
      "nameZh": "MG338 消音器",
      "nameEn": "MG338 Suppressor",
      "nameKey": "suffuse.attachment.mg338_silencer.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/mg338_silencer.json"
    },
    {
      "id": "suffuse:pistollaser",
      "displayName": "手枪式紧凑镭射",
      "nameZh": "手枪式紧凑镭射",
      "nameEn": "Pistol Laser",
      "nameKey": "suffuse.attachment.pistollaser.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "grip",
      "indexPath": "data/suffuse/index/attachments/pistollaser.json"
    },
    {
      "id": "suffuse:rm277_silencer",
      "displayName": "RM277 消音器",
      "nameZh": "RM277 消音器",
      "nameEn": "RM277 Suppressor",
      "nameKey": "suffuse.attachment.rm277_silencer.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/suffuse/index/attachments/rm277_silencer.json"
    },
    {
      "id": "suffuse:scope_acogta01",
      "displayName": "ACOG TA01 光学瞄准镜",
      "nameZh": "ACOG TA01 光学瞄准镜",
      "nameEn": "ACOG TA01 Optic Scope",
      "nameKey": "suffuse.attachment.scope_acogta01.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_acogta01.json"
    },
    {
      "id": "suffuse:scope_compm4",
      "displayName": "Aimpoint CompM4 红点瞄准镜",
      "nameZh": "Aimpoint CompM4 红点瞄准镜",
      "nameEn": "Aimpoint CompM4 Red Dot Sight",
      "nameKey": "suffuse.attachment.scope_compm4.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_compm4.json"
    },
    {
      "id": "suffuse:scope_ks23m",
      "displayName": "PU 短型光学瞄具",
      "nameZh": "PU 短型光学瞄具",
      "nameEn": "PU Short Optical Sight",
      "nameKey": "suffuse.attachment.scope_ks23m.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_ks23m.json"
    },
    {
      "id": "suffuse:scope_m2a1",
      "displayName": "suffuse.attachment.scope_m2a1.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "suffuse.attachment.scope_m2a1.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_m2a1.json"
    },
    {
      "id": "suffuse:scope_pf98a",
      "displayName": "PF-98A 固定式瞄具",
      "nameZh": "PF-98A 固定式瞄具",
      "nameEn": "PF-98A Fixed Sights",
      "nameKey": "suffuse.attachment.scope_pf98a.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_pf98a.json"
    },
    {
      "id": "suffuse:scope_qlu11s",
      "displayName": "QLU-11 光电瞄具",
      "nameZh": "QLU-11 光电瞄具",
      "nameEn": "QLU-11 Electro-Optic Scope",
      "nameKey": "suffuse.attachment.scope_qlu11s.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_qlu11s.json"
    },
    {
      "id": "suffuse:scope_romeo4t",
      "displayName": "ROMEO 4T 反射式瞄具",
      "nameZh": "ROMEO 4T 反射式瞄具",
      "nameEn": "ROMEO 4T Reflective Sight",
      "nameKey": "suffuse.attachment.scope_romeo4t.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_romeo4t.json"
    },
    {
      "id": "suffuse:scope_sig_tango_msr_1_6",
      "displayName": "SIG TANGO 1-6 速瞄具",
      "nameZh": "SIG TANGO 1-6 速瞄具",
      "nameEn": "SIG TANGO 1-6 LPVO Scope",
      "nameKey": "suffuse.attachment.scope_sig_tango_msr_1_6.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_sig_tango_msr_1_6.json"
    },
    {
      "id": "suffuse:scope_vzor3",
      "displayName": "泽宁特 VZOR-3 反射式瞄具",
      "nameZh": "泽宁特 VZOR-3 反射式瞄具",
      "nameEn": "Zenit VZOR-3 Reflective Sight",
      "nameKey": "suffuse.attachment.scope_vzor3.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/scope_vzor3.json"
    },
    {
      "id": "suffuse:sight_cobra_ekp_818",
      "displayName": "EKP-8-18 反射式瞄具",
      "nameZh": "EKP-8-18 反射式瞄具",
      "nameEn": "EKP-8-18 Reflex Sight",
      "nameKey": "suffuse.attachment.sight_cobra_ekp_818.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/sight_cobra_ekp_818.json"
    },
    {
      "id": "suffuse:sight_dbala2",
      "displayName": "DBAL-A2 战术镭射",
      "nameZh": "DBAL-A2 战术镭射",
      "nameEn": "DBAL-A2 Tactical Laser Device",
      "nameKey": "suffuse.attachment.sight_dbala2.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/sight_dbala2.json"
    },
    {
      "id": "suffuse:sight_rmr",
      "displayName": "RMR 微型反射式瞄具",
      "nameZh": "RMR 微型反射式瞄具",
      "nameEn": "RMR Micro Reflective Sight",
      "nameKey": "suffuse.attachment.sight_rmr.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "scope",
      "indexPath": "data/suffuse/index/attachments/sight_rmr.json"
    },
    {
      "id": "suffuse:stock_bcm_mod2_sopmod",
      "displayName": "BCM MOD2 SOPMOD 战术枪托",
      "nameZh": "BCM MOD2 SOPMOD 战术枪托",
      "nameEn": "BCM MOD2 SOPMOD Stock",
      "nameKey": "suffuse.attachment.stock_bcm_mod2_sopmod.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_bcm_mod2_sopmod.json"
    },
    {
      "id": "suffuse:stock_colt",
      "displayName": "Colt M4标准式 战术枪托",
      "nameZh": "Colt M4标准式 战术枪托",
      "nameEn": "Colt M4 Standard Issue Stock",
      "nameKey": "suffuse.attachment.stock_colt.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_colt.json"
    },
    {
      "id": "suffuse:stock_colt_plus",
      "displayName": "Colt M4增厚托垫型 战术枪托",
      "nameZh": "Colt M4增厚托垫型 战术枪托",
      "nameEn": "Colt M4 Thickened Stock",
      "nameKey": "suffuse.attachment.stock_colt_plus.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_colt_plus.json"
    },
    {
      "id": "suffuse:stock_comb_3dg",
      "displayName": "3DG 95式枪族 托腮板",
      "nameZh": "3DG 95式枪族 托腮板",
      "nameEn": "3DG Type 95 Compat. Cheek Pad",
      "nameKey": "suffuse.attachment.stock_comb_3dg.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_comb_3dg.json"
    },
    {
      "id": "suffuse:stock_comb_jiazhou",
      "displayName": "甲胄 95式枪族 快拆贴腮板",
      "nameZh": "甲胄 95式枪族 快拆贴腮板",
      "nameEn": "First Armor Type 95 Compat. QD Cheek Pad",
      "nameKey": "suffuse.attachment.stock_comb_jiazhou.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_comb_jiazhou.json"
    },
    {
      "id": "suffuse:stock_comb_redsight",
      "displayName": "红准星 95式枪族 托腮板",
      "nameZh": "红准星 95式枪族 托腮板",
      "nameEn": "HongZhunXing Type 95 Compat. Cheek Pad",
      "nameKey": "suffuse.attachment.stock_comb_redsight.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_comb_redsight.json"
    },
    {
      "id": "suffuse:stock_elf_ultralight",
      "displayName": "ELF 轻型枪托",
      "nameZh": "ELF 轻型枪托",
      "nameEn": "ELF Ultralight AR Stock",
      "nameKey": "suffuse.attachment.stock_elf_ultralight.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_elf_ultralight.json"
    },
    {
      "id": "suffuse:stock_m249",
      "displayName": "M249 伞兵型 伸缩枪托",
      "nameZh": "M249 伞兵型 伸缩枪托",
      "nameEn": "M249 Para. Retractable Stock",
      "nameKey": "suffuse.attachment.stock_m249.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_m249.json"
    },
    {
      "id": "suffuse:stock_minimi_mk1",
      "displayName": "Minimi MK1 折叠枪托",
      "nameZh": "Minimi MK1 折叠枪托",
      "nameEn": "Minimi MK1 Foldable Stock",
      "nameKey": "suffuse.attachment.stock_minimi_mk1.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_minimi_mk1.json"
    },
    {
      "id": "suffuse:stock_mk46",
      "displayName": "MK46 伸缩枪托",
      "nameZh": "MK46 伸缩枪托",
      "nameEn": "MK46 Retractable Stock",
      "nameKey": "suffuse.attachment.stock_mk46.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_mk46.json"
    },
    {
      "id": "suffuse:stock_n4",
      "displayName": "Magpul MOE-SL-K 战术枪托",
      "nameZh": "Magpul MOE-SL-K 战术枪托",
      "nameEn": "Magpul MOE-SL-K",
      "nameKey": "suffuse.attachment.stock_n4.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_n4.json"
    },
    {
      "id": "suffuse:stock_pip_m249",
      "displayName": "PIP M249 固定枪托",
      "nameZh": "PIP M249 固定枪托",
      "nameEn": "PIP M249 Fixed Stock",
      "nameKey": "suffuse.attachment.stock_pip_m249.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_pip_m249.json"
    },
    {
      "id": "suffuse:stock_sig_black",
      "displayName": "SIG 战术枪托 黑色款",
      "nameZh": "SIG 战术枪托 黑色款",
      "nameEn": "SIG Stock .ver Black",
      "nameKey": "suffuse.attachment.stock_sig_black.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_sig_black.json"
    },
    {
      "id": "suffuse:stock_sig_blue_grey",
      "displayName": "SIG 战术枪托 灰蓝色款",
      "nameZh": "SIG 战术枪托 灰蓝色款",
      "nameEn": "SIG Stock .ver Blue Grey",
      "nameKey": "suffuse.attachment.stock_sig_blue_grey.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_sig_blue_grey.json"
    },
    {
      "id": "suffuse:stock_sig_desert",
      "displayName": "SIG 战术枪托 沙色款",
      "nameZh": "SIG 战术枪托 沙色款",
      "nameEn": "SIG Stock .ver FDE",
      "nameKey": "suffuse.attachment.stock_sig_desert.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_sig_desert.json"
    },
    {
      "id": "suffuse:stock_vltor_emod_black",
      "displayName": "VLTOR E-MOD 增强型 模块化枪托 黑色款",
      "nameZh": "VLTOR E-MOD 增强型 模块化枪托 黑色款",
      "nameEn": "VLTOR E-MOD Enhanced Modular Stock .ver Black",
      "nameKey": "suffuse.attachment.stock_vltor_emod_black.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_vltor_emod_black.json"
    },
    {
      "id": "suffuse:stock_vltor_emod_desert",
      "displayName": "VLTOR E-MOD 增强型 模块化枪托 沙色款",
      "nameZh": "VLTOR E-MOD 增强型 模块化枪托 沙色款",
      "nameEn": "VLTOR E-MOD Enhanced Modular Stock .ver FDE",
      "nameKey": "suffuse.attachment.stock_vltor_emod_desert.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_vltor_emod_desert.json"
    },
    {
      "id": "suffuse:stock_vltor_emod_green",
      "displayName": "VLTOR E-MOD 增强型 模块化枪托 绿色款",
      "nameZh": "VLTOR E-MOD 增强型 模块化枪托 绿色款",
      "nameEn": "VLTOR E-MOD Enhanced Modular Stock .ver Green",
      "nameKey": "suffuse.attachment.stock_vltor_emod_green.name",
      "source": "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip",
      "sources": [
        "tacz/Suffuse-GunSmoke-Pack1.0.8-hotfix.zip"
      ],
      "type": "stock",
      "indexPath": "data/suffuse/index/attachments/stock_vltor_emod_green.json"
    },
    {
      "id": "tacz:aklys_sight",
      "displayName": "tacz.attachment.aklys_sight.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "tacz.attachment.aklys_sight.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/aklys_sight.json"
    },
    {
      "id": "tacz:ammo_mod_fmj",
      "displayName": "全金属被甲弹",
      "nameZh": "全金属被甲弹",
      "nameEn": "Full Metal Jacket Ammo",
      "nameKey": "tacz.attachment.ammo_mod_fmj.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/ammo_mod_fmj.json"
    },
    {
      "id": "tacz:ammo_mod_he",
      "displayName": "高爆弹",
      "nameZh": "高爆弹",
      "nameEn": "High Explosive Ammo",
      "nameKey": "tacz.attachment.ammo_mod_he.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/ammo_mod_he.json"
    },
    {
      "id": "tacz:ammo_mod_hp",
      "displayName": "空尖弹",
      "nameZh": "空尖弹",
      "nameEn": "Hollow-Point Ammo",
      "nameKey": "tacz.attachment.ammo_mod_hp.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/ammo_mod_hp.json"
    },
    {
      "id": "tacz:ammo_mod_i",
      "displayName": "燃烧弹",
      "nameZh": "燃烧弹",
      "nameEn": "Incendiary Ammo",
      "nameKey": "tacz.attachment.ammo_mod_i.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/ammo_mod_i.json"
    },
    {
      "id": "tacz:ammo_mod_slug",
      "displayName": "独头弹",
      "nameZh": "独头弹",
      "nameEn": "Shotgun Slug",
      "nameKey": "tacz.attachment.ammo_mod_slug.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/ammo_mod_slug.json"
    },
    {
      "id": "tacz:bayonet_6h3",
      "displayName": "6H3 刺刀",
      "nameZh": "6H3 刺刀",
      "nameEn": "6H3 Bayonet",
      "nameKey": "tacz.attachment.bayonet_6h3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/bayonet_6h3.json"
    },
    {
      "id": "tacz:bayonet_m9",
      "displayName": "M9 刺刀",
      "nameZh": "M9 刺刀",
      "nameEn": "M9 Bayonet",
      "nameKey": "tacz.attachment.bayonet_m9.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/bayonet_m9.json"
    },
    {
      "id": "tacz:can",
      "displayName": "Would you hold still, please?",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "§dWould you hold still, please?",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/can.json"
    },
    {
      "id": "tacz:deagle_golden_long_barrel",
      "displayName": ".357 黄金沙漠之鹰加长枪管",
      "nameZh": ".357 黄金沙漠之鹰加长枪管",
      "nameEn": ".357 Golden Deagle Long Barrel",
      "nameKey": "tacz.attachment.deagle_golden_long_barrel.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/deagle_golden_long_barrel.json"
    },
    {
      "id": "tacz:extended_mag_1",
      "displayName": "重型弹药扩容弹匣",
      "nameZh": "重型弹药扩容弹匣",
      "nameEn": "Heavy Ammo Extended Mag",
      "nameKey": "tacz.attachment.extended_mag_1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/extended_mag_1.json"
    },
    {
      "id": "tacz:extended_mag_2",
      "displayName": "重型弹药扩容弹匣",
      "nameZh": "重型弹药扩容弹匣",
      "nameEn": "Heavy Ammo Extended Mag",
      "nameKey": "tacz.attachment.extended_mag_2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/extended_mag_2.json"
    },
    {
      "id": "tacz:extended_mag_3",
      "displayName": "重型弹药扩容弹匣",
      "nameZh": "重型弹药扩容弹匣",
      "nameEn": "Heavy Ammo Extended Mag",
      "nameKey": "tacz.attachment.extended_mag_3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/extended_mag_3.json"
    },
    {
      "id": "tacz:grip_cobra",
      "displayName": "SI 三角阻手",
      "nameZh": "SI 三角阻手",
      "nameEn": "SI Grip",
      "nameKey": "tacz.attachment.grip_cobra.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_cobra.json"
    },
    {
      "id": "tacz:grip_cqr",
      "displayName": "CQR 战术握把",
      "nameZh": "CQR 战术握把",
      "nameEn": "Hera Arms CQR Grip",
      "nameKey": "tacz.attachment.grip_cqr",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_cqr.json"
    },
    {
      "id": "tacz:grip_magpul_afg_2",
      "displayName": "塔伦 AFG2 阻手",
      "nameZh": "塔伦 AFG2 阻手",
      "nameEn": "Talon AFG1 Handstop",
      "nameKey": "tacz.attachment.grip_magpul_afg_2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_magpul_afg_2.json"
    },
    {
      "id": "tacz:grip_osovets_black",
      "displayName": "P-2 战术垂直握把",
      "nameZh": "P-2 战术垂直握把",
      "nameEn": "P-2 Grip",
      "nameKey": "tacz.attachment.grip_osovets_black.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_osovets_black.json"
    },
    {
      "id": "tacz:grip_rk0",
      "displayName": "RK-0 垂直握把",
      "nameZh": "RK-0 垂直握把",
      "nameEn": "RK-0 Grip",
      "nameKey": "tacz.attachment.grip_rk0.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_rk0.json"
    },
    {
      "id": "tacz:grip_rk1_b25u",
      "displayName": "RK-1 斜握把",
      "nameZh": "RK-1 斜握把",
      "nameEn": "RK-1 B25U Grip",
      "nameKey": "tacz.attachment.grip_rk1_b25u.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_rk1_b25u.json"
    },
    {
      "id": "tacz:grip_rk6",
      "displayName": "RK-6 轻型阻手",
      "nameZh": "RK-6 轻型阻手",
      "nameEn": "RK-6 Grip",
      "nameKey": "tacz.attachment.grip_rk6.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_rk6.json"
    },
    {
      "id": "tacz:grip_se_5",
      "displayName": "SE-5 斜握把",
      "nameZh": "SE-5 斜握把",
      "nameEn": "SE-5 Express Grip",
      "nameKey": "tacz.attachment.grip_se_5",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_se_5.json"
    },
    {
      "id": "tacz:grip_td",
      "displayName": "TD 骷髅斜握把",
      "nameZh": "TD 骷髅斜握把",
      "nameEn": "TD Grip",
      "nameKey": "tacz.attachment.grip_td",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_td.json"
    },
    {
      "id": "tacz:grip_vertical_military",
      "displayName": "名驹 军用制式握把",
      "nameZh": "名驹 军用制式握把",
      "nameEn": "Nagoma Military Standard Grip",
      "nameKey": "tacz.attachment.grip_vertical_military.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_vertical_military.json"
    },
    {
      "id": "tacz:grip_vertical_ranger",
      "displayName": "科赫 游骑兵重型握把",
      "nameZh": "科赫 游骑兵重型握把",
      "nameEn": "Koch Ranger Heavy Grip",
      "nameKey": "tacz.attachment.grip_vertical_ranger.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_vertical_ranger.json"
    },
    {
      "id": "tacz:grip_vertical_talon",
      "displayName": "塔伦 SG2 前握把",
      "nameZh": "塔伦 SG2 前握把",
      "nameEn": "Talon SG2 Grip",
      "nameKey": "tacz.attachment.grip_vertical_talon.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/grip_vertical_talon.json"
    },
    {
      "id": "tacz:harris",
      "displayName": "Harris Bipod",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Harris Bipod",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/harris.json"
    },
    {
      "id": "tacz:laser_compact",
      "displayName": "米利泰克 小型激光指示器",
      "nameZh": "米利泰克 小型激光指示器",
      "nameEn": "Militech Compact Laser",
      "nameKey": "tacz.attachment.laser_compact",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_compact.json"
    },
    {
      "id": "tacz:laser_dbala",
      "displayName": "DBAL-A2 Laser",
      "nameZh": "",
      "nameEn": "DBAL-A2 Laser",
      "nameKey": "tacz.attachment.laser_dbala.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_dbala.json"
    },
    {
      "id": "tacz:laser_holosun",
      "displayName": "Holosun Dual Laser Sight",
      "nameZh": "",
      "nameEn": "Holosun Dual Laser Sight",
      "nameKey": "tacz.attachment.laser_holosun.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_holosun.json"
    },
    {
      "id": "tacz:laser_lopro",
      "displayName": "低姿战术激光指示器",
      "nameZh": "低姿战术激光指示器",
      "nameEn": "Lopro Tactical Laser",
      "nameKey": "tacz.attachment.laser_lopro",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_lopro.json"
    },
    {
      "id": "tacz:laser_nightstick",
      "displayName": "夜槌 便携式激光指示器",
      "nameZh": "夜槌 便携式激光指示器",
      "nameEn": "Nightstick Compact laser",
      "nameKey": "tacz.attachment.laser_nightstick",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_nightstick.json"
    },
    {
      "id": "tacz:laser_peq15",
      "displayName": "tacz.attachment.laser_peq15",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "tacz.attachment.laser_peq15",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_peq15.json"
    },
    {
      "id": "tacz:laser_peq6",
      "displayName": "PEQ6 集成激光瞄准模块",
      "nameZh": "PEQ6 集成激光瞄准模块",
      "nameEn": "PEQ6 ILLM",
      "nameKey": "tacz.attachment.laser_peq6",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/laser_peq6.json"
    },
    {
      "id": "tacz:light_extended_mag_1",
      "displayName": "轻型弹药扩容弹匣",
      "nameZh": "轻型弹药扩容弹匣",
      "nameEn": "Light Ammo Extended mag",
      "nameKey": "tacz.attachment.light_extended_mag_1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/light_extended_mag_1.json"
    },
    {
      "id": "tacz:light_extended_mag_2",
      "displayName": "轻型弹药扩容弹匣",
      "nameZh": "轻型弹药扩容弹匣",
      "nameEn": "Light Ammo Extended mag",
      "nameKey": "tacz.attachment.light_extended_mag_2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/light_extended_mag_2.json"
    },
    {
      "id": "tacz:light_extended_mag_3",
      "displayName": "轻型弹药扩容弹匣",
      "nameZh": "轻型弹药扩容弹匣",
      "nameEn": "Light Ammo Extended mag",
      "nameKey": "tacz.attachment.light_extended_mag_3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/light_extended_mag_3.json"
    },
    {
      "id": "tacz:ls321",
      "displayName": "Holosun LS-321 laser",
      "nameZh": "",
      "nameEn": "Holosun LS-321 laser",
      "nameKey": "tacz.attachment.ls321.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/ls321.json"
    },
    {
      "id": "tacz:ls32_black",
      "displayName": "Holosun LS-321 laser Black",
      "nameZh": "",
      "nameEn": "Holosun LS-321 laser Black",
      "nameKey": "tacz.attachment.ls321_black.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "laser",
      "indexPath": "data/tacz/index/attachments/ls32_black.json"
    },
    {
      "id": "tacz:m1918bip",
      "displayName": "M1918 Bipod",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M1918 Bipod",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "grip",
      "indexPath": "data/tacz/index/attachments/m1918bip.json"
    },
    {
      "id": "tacz:m4",
      "displayName": "Comp M4",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Comp M4",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/m4.json"
    },
    {
      "id": "tacz:m82",
      "displayName": "M73B Weaver Scope",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "M73B Weaver Scope",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/m82.json"
    },
    {
      "id": "tacz:muzzle_brake_cthulhu",
      "displayName": "克苏鲁 K7 制退器",
      "nameZh": "克苏鲁 K7 制退器",
      "nameEn": "Cthulhu K7 Brake",
      "nameKey": "tacz.attachment.muzzle_brake_cthulhu.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_cthulhu.json"
    },
    {
      "id": "tacz:muzzle_brake_cyclone_d2",
      "displayName": "气旋 D2 枪口制退器",
      "nameZh": "气旋 D2 枪口制退器",
      "nameEn": "Cyclone D2 Brake",
      "nameKey": "tacz.attachment.muzzle_brake_cyclone_d2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_cyclone_d2.json"
    },
    {
      "id": "tacz:muzzle_brake_mastiff_sg",
      "displayName": "獒犬 制退器",
      "nameZh": "獒犬 制退器",
      "nameEn": "Mastiff Shotgun Muzzle Brake",
      "nameKey": "tacz.attachment.muzzle_brake_mastiff_sg.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_mastiff_sg.json"
    },
    {
      "id": "tacz:muzzle_brake_pioneer",
      "displayName": "先锋 A3 制退器",
      "nameZh": "先锋 A3 制退器",
      "nameEn": "Pioneer A3 Brake",
      "nameKey": "tacz.attachment.muzzle_brake_pioneer.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_pioneer.json"
    },
    {
      "id": "tacz:muzzle_brake_timeless50",
      "displayName": "永恒 .50口径制退器",
      "nameZh": "永恒 .50口径制退器",
      "nameEn": "Timeless .50 Cal Brake",
      "nameKey": "tacz.attachment.muzzle_brake_timeless50.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_timeless50.json"
    },
    {
      "id": "tacz:muzzle_brake_timeless50_black",
      "displayName": "Timeless .50 Cal Brake Black",
      "nameZh": "",
      "nameEn": "Timeless .50 Cal Brake Black",
      "nameKey": "tacz.attachment.muzzle_brake_timeless50_black.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_timeless50_black.json"
    },
    {
      "id": "tacz:muzzle_brake_timeless50_tan",
      "displayName": "Timeless .50 Cal Brake Tan",
      "nameZh": "",
      "nameEn": "Timeless .50 Cal Brake Tan",
      "nameKey": "tacz.attachment.muzzle_brake_timeless50_tan.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_timeless50_tan.json"
    },
    {
      "id": "tacz:muzzle_brake_trex",
      "displayName": "霸王龙 重型制退器",
      "nameZh": "霸王龙 重型制退器",
      "nameEn": "T-Rex Heavy Brake",
      "nameKey": "tacz.attachment.muzzle_brake_trex.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_brake_trex.json"
    },
    {
      "id": "tacz:muzzle_choke_sg",
      "displayName": "扼流器",
      "nameZh": "扼流器",
      "nameEn": "Shotgun Choke",
      "nameKey": "tacz.attachment.muzzle_choke_sg.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_choke_sg.json"
    },
    {
      "id": "tacz:muzzle_compensator_trident",
      "displayName": "风暴 三叉戟 枪口补偿器",
      "nameZh": "风暴 三叉戟 枪口补偿器",
      "nameEn": "Tempest Trident Compensator",
      "nameKey": "tacz.attachment.muzzle_compensator_trident.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_compensator_trident.json"
    },
    {
      "id": "tacz:muzzle_silencer_albert",
      "displayName": "\"Albert-01\" Silencer",
      "nameZh": "",
      "nameEn": "\"Albert-01\" Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_albert.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_albert.json"
    },
    {
      "id": "tacz:muzzle_silencer_knight_qd",
      "displayName": "骑士快拆消音器",
      "nameZh": "骑士快拆消音器",
      "nameEn": "Knight QD Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_knight_qd.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_knight_qd.json"
    },
    {
      "id": "tacz:muzzle_silencer_mirage",
      "displayName": "幻象 手枪消音器",
      "nameZh": "幻象 手枪消音器",
      "nameEn": "Mirage Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_mirage.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_mirage.json"
    },
    {
      "id": "tacz:muzzle_silencer_phantom_s1",
      "displayName": "幽影 S1 消音器",
      "nameZh": "幽影 S1 消音器",
      "nameEn": "Phantom S1 Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_phantom_s1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_phantom_s1.json"
    },
    {
      "id": "tacz:muzzle_silencer_ptilopsis",
      "displayName": "PO-2 \"白面鸮\" 手枪消音器",
      "nameZh": "PO-2 \"白面鸮\" 手枪消音器",
      "nameEn": "PO-2 \"Ptilopsis\" Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_ptilopsis.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_ptilopsis.json"
    },
    {
      "id": "tacz:muzzle_silencer_sg",
      "displayName": "12号口径消音器",
      "nameZh": "12号口径消音器",
      "nameEn": "12 Gauge Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_sg.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_sg.json"
    },
    {
      "id": "tacz:muzzle_silencer_ursus",
      "displayName": "乌萨斯制式消音器",
      "nameZh": "乌萨斯制式消音器",
      "nameEn": "Ursus Military Standard Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_ursus.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_ursus.json"
    },
    {
      "id": "tacz:muzzle_silencer_vulture",
      "displayName": "秃鹫.50口径消音器",
      "nameZh": "秃鹫.50口径消音器",
      "nameEn": "Vulture .50 Cal Suppressor",
      "nameKey": "tacz.attachment.muzzle_silencer_vulture.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_vulture.json"
    },
    {
      "id": "tacz:muzzle_silencer_wraith",
      "displayName": "幽灵 手枪消音器",
      "nameZh": "幽灵 手枪消音器",
      "nameEn": "Wraith Silencer",
      "nameKey": "tacz.attachment.muzzle_silencer_wraith.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/muzzle_silencer_wraith.json"
    },
    {
      "id": "tacz:no32",
      "displayName": "No.32 Rifle Scope",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "No.32 Rifle Scope",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/no32.json"
    },
    {
      "id": "tacz:nxs",
      "displayName": "Nightforce NXS 5.5-22x56",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Nightforce NXS 5.5-22x56",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/nxs.json"
    },
    {
      "id": "tacz:oem_stock_heavy",
      "displayName": "原厂重型枪托",
      "nameZh": "原厂重型枪托",
      "nameEn": "Factory Issued Heavy Stock",
      "nameKey": "tacz.attachment.oem_stock_heavy.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/oem_stock_heavy.json"
    },
    {
      "id": "tacz:oem_stock_light",
      "displayName": "原厂轻型枪托",
      "nameZh": "原厂轻型枪托",
      "nameEn": "Factory Issued light Stock",
      "nameKey": "tacz.attachment.oem_stock_light.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/oem_stock_light.json"
    },
    {
      "id": "tacz:oem_stock_tactical",
      "displayName": "原厂战术枪托",
      "nameZh": "原厂战术枪托",
      "nameEn": "Factory Issued Tactical Stock",
      "nameKey": "tacz.attachment.oem_stock_tactical.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/oem_stock_tactical.json"
    },
    {
      "id": "tacz:pso1",
      "displayName": "PSO-1 Scope",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PSO-1 Scope",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/pso1.json"
    },
    {
      "id": "tacz:pu",
      "displayName": "PU 5x Scope",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PU 5x Scope",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/pu.json"
    },
    {
      "id": "tacz:sawn",
      "displayName": "Sawn off M1887 Conversion",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Sawn off M1887 Conversion",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/sawn.json"
    },
    {
      "id": "tacz:scope_1873_6x",
      "displayName": "老式 春田神射手瞄具",
      "nameZh": "老式 春田神射手瞄具",
      "nameEn": "Vintage Springfield Scope",
      "nameKey": "tacz.attachment.scope_1873_6x.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_1873_6x.json"
    },
    {
      "id": "tacz:scope_98k",
      "displayName": "毛瑟 4x 光学瞄具",
      "nameZh": "毛瑟 4x 光学瞄具",
      "nameEn": "Mauser 4x Light Sight",
      "nameKey": "tacz.attachment.scope_98k.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_98k.json"
    },
    {
      "id": "tacz:scope_acog_ta31",
      "displayName": "TA31 2x 先进战斗光学瞄具",
      "nameZh": "TA31 2x 先进战斗光学瞄具",
      "nameEn": "TA31 2x ACOG",
      "nameKey": "tacz.attachment.scope_acog_ta31.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_acog_ta31.json"
    },
    {
      "id": "tacz:scope_aug_default",
      "displayName": "AUG 内置瞄具",
      "nameZh": "AUG 内置瞄具",
      "nameEn": "AUG Builtin Scope",
      "nameKey": "tacz.attachment.scope_aug_default.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_aug_default.json"
    },
    {
      "id": "tacz:scope_contender",
      "displayName": "竞技者 4x 光学瞄具",
      "nameZh": "竞技者 4x 光学瞄具",
      "nameEn": "Contender 4x Scope",
      "nameKey": "tacz.attachment.scope_contender.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_contender.json"
    },
    {
      "id": "tacz:scope_elcan_4x",
      "displayName": "埃尔坎 4x 光学瞄具",
      "nameZh": "埃尔坎 4x 光学瞄具",
      "nameEn": "Elcan 4x Scope",
      "nameKey": "tacz.attachment.scope_elcan_4x.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_elcan_4x.json"
    },
    {
      "id": "tacz:scope_hamr",
      "displayName": "HAMR 3x 组合式光学瞄具",
      "nameZh": "HAMR 3x 组合式光学瞄具",
      "nameEn": "HAMR 3x Scope",
      "nameKey": "tacz.attachment.scope_hamr.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_hamr.json"
    },
    {
      "id": "tacz:scope_lpvo_1_6",
      "displayName": "LPVO 1-6倍瞄具 ",
      "nameZh": "LPVO 1-6倍瞄具 ",
      "nameEn": "LPVO 1-6x Scope ",
      "nameKey": "tacz.attachment.scope_lpvo_1_6.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_lpvo_1_6.json"
    },
    {
      "id": "tacz:scope_mk5hd",
      "displayName": "Mark 5 HD 5-25x 组合式光学瞄具",
      "nameZh": "Mark 5 HD 5-25x 组合式光学瞄具",
      "nameEn": "Mark 5 HD 5-25x Scope",
      "nameKey": "tacz.attachment.scope_mk5hd.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_mk5hd.json"
    },
    {
      "id": "tacz:scope_psg",
      "displayName": "PSG-1 scope",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "PSG-1 scope",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_psg.json"
    },
    {
      "id": "tacz:scope_qmk152",
      "displayName": "QMK-152 3x 白光瞄准镜",
      "nameZh": "QMK-152 3x 白光瞄准镜",
      "nameEn": "QMK-152 3x White Light Sight",
      "nameKey": "tacz.attachment.scope_qmk152.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_qmk152.json"
    },
    {
      "id": "tacz:scope_retro_2x",
      "displayName": "3x 光学瞄具\"复古\"",
      "nameZh": "3x 光学瞄具\"复古\"",
      "nameEn": "Retro 3x Scope",
      "nameKey": "tacz.attachment.scope_retro_2x.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_retro_2x.json"
    },
    {
      "id": "tacz:scope_standard_8x",
      "displayName": "斥候 4-10x 光学瞄具",
      "nameZh": "斥候 4-10x 光学瞄具",
      "nameEn": "Scout 4-10x Scope",
      "nameKey": "tacz.attachment.scope_standard_8x.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_standard_8x.json"
    },
    {
      "id": "tacz:scope_vudu",
      "displayName": "巫毒 1-6x 组合式精确瞄具",
      "nameZh": "巫毒 1-6x 组合式精确瞄具",
      "nameEn": "Vudu 1-6x Scope",
      "nameKey": "tacz.attachment.scope_vudu.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/scope_vudu.json"
    },
    {
      "id": "tacz:shotgun_extended_mag_1",
      "displayName": "霰弹弹药扩容弹匣",
      "nameZh": "霰弹弹药扩容弹匣",
      "nameEn": "Shotgun Ammo Extended Mag",
      "nameKey": "tacz.attachment.shotgun_extended_mag_1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/shotgun_extended_mag_1.json"
    },
    {
      "id": "tacz:shotgun_extended_mag_2",
      "displayName": "霰弹弹药扩容弹匣",
      "nameZh": "霰弹弹药扩容弹匣",
      "nameEn": "Shotgun Ammo Extended Mag",
      "nameKey": "tacz.attachment.shotgun_extended_mag_2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/shotgun_extended_mag_2.json"
    },
    {
      "id": "tacz:shotgun_extended_mag_3",
      "displayName": "霰弹弹药扩容弹匣",
      "nameZh": "霰弹弹药扩容弹匣",
      "nameEn": "Shotgun Ammo Extended Mag",
      "nameKey": "tacz.attachment.shotgun_extended_mag_3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/shotgun_extended_mag_3.json"
    },
    {
      "id": "tacz:sight_552",
      "displayName": "EOTECH 552全息瞄具 ",
      "nameZh": "EOTECH 552全息瞄具 ",
      "nameEn": "Militech 552 HCOG",
      "nameKey": "tacz.attachment.sight_552.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_552.json"
    },
    {
      "id": "tacz:sight_552_tan",
      "displayName": "Militech 552 HCOG TAN",
      "nameZh": "",
      "nameEn": "Militech 552 HCOG TAN",
      "nameKey": "tacz.attachment.sight_552_tan.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_552_tan.json"
    },
    {
      "id": "tacz:sight_acro_pistol",
      "displayName": "ACRO P-1 反射式瞄具",
      "nameZh": "ACRO P-1 反射式瞄具",
      "nameEn": "Aimpoint ACRO P-1 Sight",
      "nameKey": "tacz.attachment.sight_acro_pistol.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_acro_pistol.json"
    },
    {
      "id": "tacz:sight_acro_rifle",
      "displayName": "ACRO P-1 增高 反射式瞄具",
      "nameZh": "ACRO P-1 增高 反射式瞄具",
      "nameEn": "Aimpoint ACRO P-1 Sight Rised",
      "nameKey": "tacz.attachment.sight_acro_rifle.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_acro_rifle.json"
    },
    {
      "id": "tacz:sight_coyote",
      "displayName": "Coyote 红点瞄准镜",
      "nameZh": "Coyote 红点瞄准镜",
      "nameEn": "Coyote Sight",
      "nameKey": "tacz.attachment.sight_coyote.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_coyote.json"
    },
    {
      "id": "tacz:sight_deltapoint_pistol",
      "displayName": "DeltaPoint 反射式瞄具",
      "nameZh": "DeltaPoint 反射式瞄具",
      "nameEn": "DeltaPoint Sight",
      "nameKey": "tacz.attachment.sight_deltapoint_pistol.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_deltapoint_pistol.json"
    },
    {
      "id": "tacz:sight_deltapoint_rifle",
      "displayName": "DeltaPoint 增高 反射式瞄具",
      "nameZh": "DeltaPoint 增高 反射式瞄具",
      "nameEn": "DeltaPoint Sight Rised",
      "nameKey": "tacz.attachment.sight_deltapoint_rifle.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_deltapoint_rifle.json"
    },
    {
      "id": "tacz:sight_exp3",
      "displayName": "EXP3全息瞄具 ",
      "nameZh": "EXP3全息瞄具 ",
      "nameEn": "EXP3 HCOG",
      "nameKey": "tacz.attachment.sight_exp3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_exp3.json"
    },
    {
      "id": "tacz:sight_exp3_tan",
      "displayName": "EXP3 HCOG TAN",
      "nameZh": "",
      "nameEn": "EXP3 HCOG TAN",
      "nameKey": "tacz.attachment.sight_exp3_tan.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_exp3_tan.json"
    },
    {
      "id": "tacz:sight_fastfire_pistol",
      "displayName": "FastFire 反射式瞄具",
      "nameZh": "FastFire 反射式瞄具",
      "nameEn": "FastFire Sight",
      "nameKey": "tacz.attachment.sight_fastfire_pistol.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_fastfire_pistol.json"
    },
    {
      "id": "tacz:sight_fastfire_rifle",
      "displayName": "FastFire 增高 射式瞄具",
      "nameZh": "FastFire 增高 射式瞄具",
      "nameEn": "FastFire Sight Rised",
      "nameKey": "tacz.attachment.sight_fastfire_rifle.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_fastfire_rifle.json"
    },
    {
      "id": "tacz:sight_okp7",
      "displayName": "OKP-7 反射式瞄具",
      "nameZh": "OKP-7 反射式瞄具",
      "nameEn": "OKP-7 Sight",
      "nameKey": "tacz.attachment.sight_okp7.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_okp7.json"
    },
    {
      "id": "tacz:sight_p90",
      "displayName": "tacz.attachment.sight_p90.name",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "tacz.attachment.sight_p90.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_p90.json"
    },
    {
      "id": "tacz:sight_pk06_pistol",
      "displayName": "PK06 反射式瞄具",
      "nameZh": "PK06 反射式瞄具",
      "nameEn": "PK06 Sight",
      "nameKey": "tacz.attachment.sight_pk06_pistol.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_pk06_pistol.json"
    },
    {
      "id": "tacz:sight_pk06_rifle",
      "displayName": "PK06 增高 反射式瞄具",
      "nameZh": "PK06 增高 反射式瞄具",
      "nameEn": "PK06 Sight Rised",
      "nameKey": "tacz.attachment.sight_pk06_rifle.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_pk06_rifle.json"
    },
    {
      "id": "tacz:sight_rmr_dot",
      "displayName": "RMR迷你红点瞄具 ",
      "nameZh": "RMR迷你红点瞄具 ",
      "nameEn": "RMR Mini Red Dot",
      "nameKey": "tacz.attachment.sight_rmr_dot.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_rmr_dot.json"
    },
    {
      "id": "tacz:sight_sro_dot",
      "displayName": "SRO 微型红点瞄具",
      "nameZh": "SRO 微型红点瞄具",
      "nameEn": "SRO Mini Red Dot",
      "nameKey": "tacz.attachment.sight_sro_dot.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_sro_dot.json"
    },
    {
      "id": "tacz:sight_srs_02",
      "displayName": "SRS-02 反射式瞄具",
      "nameZh": "SRS-02 反射式瞄具",
      "nameEn": "Trijicon SRS-02 Reflex Sight",
      "nameKey": "tacz.attachment.sight_srs_02.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_srs_02.json"
    },
    {
      "id": "tacz:sight_t1",
      "displayName": "T1封闭式红点瞄具 ",
      "nameZh": "T1封闭式红点瞄具 ",
      "nameEn": "T1 red dot",
      "nameKey": "tacz.attachment.sight_t1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_t1.json"
    },
    {
      "id": "tacz:sight_t2",
      "displayName": "T2封闭式红点瞄具 ",
      "nameZh": "T2封闭式红点瞄具 ",
      "nameEn": "T2 red dot",
      "nameKey": "tacz.attachment.sight_t2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_t2.json"
    },
    {
      "id": "tacz:sight_uh1",
      "displayName": "UH-1全息瞄具 ",
      "nameZh": "UH-1全息瞄具 ",
      "nameEn": "UH-1 HCOG",
      "nameKey": "tacz.attachment.sight_uh1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "scope",
      "indexPath": "data/tacz/index/attachments/sight_uh1.json"
    },
    {
      "id": "tacz:silencer_sr",
      "displayName": "9x39mm Detachable Silencer",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "9x39mm Detachable Silencer",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "muzzle",
      "indexPath": "data/tacz/index/attachments/silencer_sr.json"
    },
    {
      "id": "tacz:sniper_extended_mag_1",
      "displayName": "狙击弹药扩容弹匣",
      "nameZh": "狙击弹药扩容弹匣",
      "nameEn": "Sniper Ammo Extended mag",
      "nameKey": "tacz.attachment.sniper_extended_mag_1.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/sniper_extended_mag_1.json"
    },
    {
      "id": "tacz:sniper_extended_mag_2",
      "displayName": "狙击弹药扩容弹匣",
      "nameZh": "狙击弹药扩容弹匣",
      "nameEn": "Sniper Ammo Extended mag",
      "nameKey": "tacz.attachment.sniper_extended_mag_2.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/sniper_extended_mag_2.json"
    },
    {
      "id": "tacz:sniper_extended_mag_3",
      "displayName": "狙击弹药扩容弹匣",
      "nameZh": "狙击弹药扩容弹匣",
      "nameEn": "Sniper Ammo Extended Mag",
      "nameKey": "tacz.attachment.sniper_extended_mag_3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/sniper_extended_mag_3.json"
    },
    {
      "id": "tacz:stock_ak12",
      "displayName": "伊兹玛什 战术枪托",
      "nameZh": "伊兹玛什 战术枪托",
      "nameEn": "AK-12 Regular Stock",
      "nameKey": "tacz.attachment.stock_ak12.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_ak12.json"
    },
    {
      "id": "tacz:stock_carbon_bone_c5",
      "displayName": "碳骨 C5 轻型枪托",
      "nameZh": "碳骨 C5 轻型枪托",
      "nameEn": "Carbon bone C5 Stock",
      "nameKey": "tacz.attachment.stock_carbon_bone_c5.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_carbon_bone_c5.json"
    },
    {
      "id": "tacz:stock_heavy_spas_12",
      "displayName": "弗兰基 重型枪托",
      "nameZh": "弗兰基 重型枪托",
      "nameEn": "Franchi Heavy Stock",
      "nameKey": "tacz.attachment.stock_heavy_spas_12.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_heavy_spas_12.json"
    },
    {
      "id": "tacz:stock_hk_slim_line",
      "displayName": "KS 战术枪托",
      "nameZh": "KS 战术枪托",
      "nameEn": "HK Slim Line Stock",
      "nameKey": "tacz.attachment.stock_hk_slim_line.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_hk_slim_line.json"
    },
    {
      "id": "tacz:stock_hk_slim_line_tan",
      "displayName": "HK Slim Line Stock Tan",
      "nameZh": "",
      "nameEn": "HK Slim Line Stock Tan",
      "nameKey": "tacz.attachment.stock_hk_slim_line_tan.name",
      "source": "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip",
      "sources": [
        "tacz/Tacz1.1.8-Only] Tacz_Plus_v1.6.zip"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_hk_slim_line_tan.json"
    },
    {
      "id": "tacz:stock_m4ss",
      "displayName": "M4SS 战术枪托",
      "nameZh": "M4SS 战术枪托",
      "nameEn": "M4SS Stock",
      "nameKey": "tacz.attachment.stock_m4ss.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_m4ss.json"
    },
    {
      "id": "tacz:stock_militech_b5",
      "displayName": "军科 B5 战术枪托",
      "nameZh": "军科 B5 战术枪托",
      "nameEn": "Militech B5 Stock",
      "nameKey": "tacz.attachment.stock_militech_b5.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_militech_b5.json"
    },
    {
      "id": "tacz:stock_moe",
      "displayName": "MOE 轻型枪托",
      "nameZh": "MOE 轻型枪托",
      "nameEn": "Magpul MOE Stock",
      "nameKey": "tacz.attachment.stock_moe.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_moe.json"
    },
    {
      "id": "tacz:stock_ripstock",
      "displayName": "RS 超轻型枪托",
      "nameZh": "RS 超轻型枪托",
      "nameEn": "CMMG RipStock Stock",
      "nameKey": "tacz.attachment.stock_ripstock.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_ripstock.json"
    },
    {
      "id": "tacz:stock_sba3",
      "displayName": "SBA3 枪托",
      "nameZh": "SBA3 枪托",
      "nameEn": "SBA3 Stock",
      "nameKey": "tacz.attachment.stock_sba3.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_sba3.json"
    },
    {
      "id": "tacz:stock_tactical_ar",
      "displayName": "CTR战术枪托",
      "nameZh": "CTR战术枪托",
      "nameEn": "Magpul CTR stock",
      "nameKey": "tacz.attachment.stock_tactical_ar.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_tactical_ar.json"
    },
    {
      "id": "tacz:stock_tactical_spas_12",
      "displayName": "弗兰基 战术枪托",
      "nameZh": "弗兰基 战术枪托",
      "nameEn": "Franchi Tactical Stock",
      "nameKey": "tacz.attachment.stock_tactical_spas_12.name",
      "source": "tacz/tacz_default_gun",
      "sources": [
        "tacz/tacz_default_gun"
      ],
      "type": "stock",
      "indexPath": "data/tacz/index/attachments/stock_tactical_spas_12.json"
    },
    {
      "id": "tacz:subsonic",
      "displayName": "Subsonic Rounds",
      "nameZh": "",
      "nameEn": "",
      "nameKey": "Subsonic Rounds",
      "source": "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip",
      "sources": [
        "tacz/TaCZ_-Expanded-Arsenal-v3.0 (1).zip"
      ],
      "type": "extended_mag",
      "indexPath": "data/tacz/index/attachments/subsonic.json"
    }
  ],
  "vehicles": [
    {
      "id": "dragonrise_reforge:2s25m",
      "entityId": "dragonrise_reforge:2s25m",
      "displayName": "2s25 章鱼",
      "nameZh": "2s25 章鱼",
      "nameEn": "2S25 Sprut-SDM1",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/2s25m.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/2s25m.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:2s38",
      "entityId": "dragonrise_reforge:2s38",
      "displayName": "2S38",
      "nameZh": "2S38",
      "nameEn": "2S38",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/2s38.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/2s38.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:625e",
      "entityId": "dragonrise_reforge:625e",
      "displayName": "625E",
      "nameZh": "625E",
      "nameEn": "625E",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/625e.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/625e.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:9m133",
      "entityId": "dragonrise_reforge:9m133",
      "displayName": "短号 反坦克导弹",
      "nameZh": "短号 反坦克导弹",
      "nameEn": "9M133 Kornet Anti-Tank Missile",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/9m133.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/9m133.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ac130",
      "entityId": "dragonrise_reforge:ac130",
      "displayName": "AC-130",
      "nameZh": "AC-130",
      "nameEn": "AC-130",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ac130.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ac130.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ah1f",
      "entityId": "dragonrise_reforge:ah1f",
      "displayName": "AH-1F 眼镜蛇",
      "nameZh": "AH-1F 眼镜蛇",
      "nameEn": "AH-1F Cobra",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ah1f.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ah1f.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ah64",
      "entityId": "dragonrise_reforge:ah64",
      "displayName": "AH-64 阿帕奇",
      "nameZh": "AH-64 阿帕奇",
      "nameEn": "AH-64 Apache",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ah64.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ah64.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:akm",
      "entityId": "dragonrise_reforge:akm",
      "displayName": "AKM 黄蜂",
      "nameZh": "AKM 黄蜂",
      "nameEn": "AKM Hornet",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/akm.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/akm.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:amx56",
      "entityId": "dragonrise_reforge:amx56",
      "displayName": "AMX-56",
      "nameZh": "AMX-56",
      "nameEn": "AMX-56 Leclerc",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/amx56.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/amx56.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:av8b",
      "entityId": "dragonrise_reforge:av8b",
      "displayName": "AV-8B",
      "nameZh": "AV-8B",
      "nameEn": "AV-8B Harrier II",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/av8b.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/av8b.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:bmd4m",
      "entityId": "dragonrise_reforge:bmd4m",
      "displayName": "BMD-4M",
      "nameZh": "BMD-4M",
      "nameEn": "BMD-4M",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/bmd4m.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/bmd4m.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:bmp3",
      "entityId": "dragonrise_reforge:bmp3",
      "displayName": "BMP-3",
      "nameZh": "BMP-3",
      "nameEn": "BMP-3",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/bmp3.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/bmp3.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:bmpt72",
      "entityId": "dragonrise_reforge:bmpt72",
      "displayName": "BMPT-72",
      "nameZh": "BMPT-72",
      "nameEn": "BMPT-72",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/bmpt72.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/bmpt72.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:camel",
      "entityId": "dragonrise_reforge:camel",
      "displayName": "Camel",
      "nameZh": "Camel",
      "nameEn": "Camel",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/camel.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/camel.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:churchill_vii",
      "entityId": "dragonrise_reforge:churchill_vii",
      "displayName": "Churchill VII",
      "nameZh": "Churchill VII",
      "nameEn": "Churchill VII",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/churchill_vii.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/churchill_vii.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:cm34",
      "entityId": "dragonrise_reforge:cm34",
      "displayName": "CM-34",
      "nameZh": "CM-34",
      "nameEn": "CM-34",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/cm34.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/cm34.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:comet",
      "entityId": "dragonrise_reforge:comet",
      "displayName": "Comet",
      "nameZh": "Comet",
      "nameEn": "Comet",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/comet.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/comet.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:csk181",
      "entityId": "dragonrise_reforge:csk181",
      "displayName": "CSK-181",
      "nameZh": "CSK-181",
      "nameEn": "CSK-181",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/csk181.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/csk181.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:cv90",
      "entityId": "dragonrise_reforge:cv90",
      "displayName": "CV90",
      "nameZh": "CV90",
      "nameEn": "CV90",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/cv90.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/cv90.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:cyborg_tank",
      "entityId": "dragonrise_reforge:cyborg_tank",
      "displayName": "湮灭坦克",
      "nameZh": "湮灭坦克",
      "nameEn": "Annihilation Tank",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/cyborg_tank.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/cyborg_tank.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:dshk",
      "entityId": "dragonrise_reforge:dshk",
      "displayName": "DShK",
      "nameZh": "DShK",
      "nameEn": "DShK",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/dshk.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/dshk.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ec665",
      "entityId": "dragonrise_reforge:ec665",
      "displayName": "EC665",
      "nameZh": "EC665",
      "nameEn": "EC665 Tiger",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ec665.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ec665.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:f14",
      "entityId": "dragonrise_reforge:f14",
      "displayName": "F-14",
      "nameZh": "F-14",
      "nameEn": "F-14 Tomcat",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/f14.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/f14.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:f16c",
      "entityId": "dragonrise_reforge:f16c",
      "displayName": "F-16C 战隼",
      "nameZh": "F-16C 战隼",
      "nameEn": "F-16C Fighting Falcon",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/f16c.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/f16c.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:f4u",
      "entityId": "dragonrise_reforge:f4u",
      "displayName": "F4U 海盗",
      "nameZh": "F4U 海盗",
      "nameEn": "F4U Corsair",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/f4u.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/f4u.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:fav_a",
      "entityId": "dragonrise_reforge:fav_a",
      "displayName": "FAV-A",
      "nameZh": "FAV-A",
      "nameEn": "FAV-A",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/fav_a.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/fav_a.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:hj8",
      "entityId": "dragonrise_reforge:hj8",
      "displayName": "红箭-8 反坦克导弹",
      "nameZh": "红箭-8 反坦克导弹",
      "nameEn": "HJ-8 Anti-Tank Missile",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/hj8.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/hj8.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:hyr0",
      "entityId": "dragonrise_reforge:hyr0",
      "displayName": "HYR0",
      "nameZh": "HYR0",
      "nameEn": "HYR0",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/hyr0.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/hyr0.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:is2",
      "entityId": "dragonrise_reforge:is2",
      "displayName": "IS-2",
      "nameZh": "IS-2",
      "nameEn": "IS-2",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/is2.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/is2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j10",
      "entityId": "dragonrise_reforge:j10",
      "displayName": "J-10",
      "nameZh": "J-10",
      "nameEn": "J-10",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j10.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j10.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j10c",
      "entityId": "dragonrise_reforge:j10c",
      "displayName": "J-10C",
      "nameZh": "J-10C",
      "nameEn": "J-10C",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j10c.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j10c.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j11",
      "entityId": "dragonrise_reforge:j11",
      "displayName": "J-11B",
      "nameZh": "J-11B",
      "nameEn": "J-11B",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j11.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j11.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j15t",
      "entityId": "dragonrise_reforge:j15t",
      "displayName": "J-15T",
      "nameZh": "J-15T",
      "nameEn": "J-15T",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j15t.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j15t.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j16",
      "entityId": "dragonrise_reforge:j16",
      "displayName": "J-16",
      "nameZh": "J-16",
      "nameEn": "J-16",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j16.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j16.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j20",
      "entityId": "dragonrise_reforge:j20",
      "displayName": "J-20",
      "nameZh": "J-20",
      "nameEn": "J-20",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j20.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j20.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j20vtol",
      "entityId": "dragonrise_reforge:j20vtol",
      "displayName": "J-20C (VTOL)",
      "nameZh": "J-20C (VTOL)",
      "nameEn": "J-20C (VTOL)",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j20vtol.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j20vtol.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j35",
      "entityId": "dragonrise_reforge:j35",
      "displayName": "J-35",
      "nameZh": "J-35",
      "nameEn": "J-35",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j35.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j35.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:j8",
      "entityId": "dragonrise_reforge:j8",
      "displayName": "J-8B",
      "nameZh": "J-8B",
      "nameEn": "J-8B",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j8.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/j8.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:jas39e",
      "entityId": "dragonrise_reforge:jas39e",
      "displayName": "JAS 39E",
      "nameZh": "JAS 39E",
      "nameEn": "JAS 39E Gripen",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/jas39e.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/jas39e.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:jf17",
      "entityId": "dragonrise_reforge:jf17",
      "displayName": "JF-17",
      "nameZh": "JF-17",
      "nameEn": "JF-17 Thunder",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/jf17.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/jf17.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ka50",
      "entityId": "dragonrise_reforge:ka50",
      "displayName": "Ka-50",
      "nameZh": "Ka-50",
      "nameEn": "Ka-50 Hokum",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ka50.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ka50.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:kv1",
      "entityId": "dragonrise_reforge:kv1",
      "displayName": "KV-1",
      "nameZh": "KV-1",
      "nameEn": "KV-1 Heavy Tank",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/kv1.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/kv1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:l1a2",
      "entityId": "dragonrise_reforge:l1a2",
      "displayName": "猎豹 1A2 自行防空炮",
      "nameZh": "猎豹 1A2 自行防空炮",
      "nameEn": "Gepard 1A2 SPAAG",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/l1a2.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/l1a2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:leopard2a4",
      "entityId": "dragonrise_reforge:leopard2a4",
      "displayName": "豹2A4 主战坦克",
      "nameZh": "豹2A4 主战坦克",
      "nameEn": "Leopard 2A4",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/leopard2a4.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/leopard2a4.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:lvt",
      "entityId": "dragonrise_reforge:lvt",
      "displayName": "LVT",
      "nameZh": "LVT",
      "nameEn": "LVT",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/lvt.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/lvt.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m10booker",
      "entityId": "dragonrise_reforge:m10booker",
      "displayName": "M10 布克",
      "nameZh": "M10 布克",
      "nameEn": "M10 Booker",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m10booker.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m10booker.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m113",
      "entityId": "dragonrise_reforge:m113",
      "displayName": "M113",
      "nameZh": "M113",
      "nameEn": "M113",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m113.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m113.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m1a2sepv2",
      "entityId": "dragonrise_reforge:m1a2sepv2",
      "displayName": "M1A2 SEPv2",
      "nameZh": "M1A2 SEPv2",
      "nameEn": "M1A2 SEPv2",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m1a2sepv2.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m1a2sepv2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m2",
      "entityId": "dragonrise_reforge:m2",
      "displayName": "M2HB",
      "nameZh": "M2HB",
      "nameEn": "M2HB",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m2.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m3a3",
      "entityId": "dragonrise_reforge:m3a3",
      "displayName": "M3A3 布雷德利",
      "nameZh": "M3A3 布雷德利",
      "nameEn": "M3A3 Bradley",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m3a3.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m3a3.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m3stuart",
      "entityId": "dragonrise_reforge:m3stuart",
      "displayName": "M3斯图亚特",
      "nameZh": "M3斯图亚特",
      "nameEn": "M3Stuart",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m3stuart.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m3stuart.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m4a2",
      "entityId": "dragonrise_reforge:m4a2",
      "displayName": "M4A2 谢尔曼",
      "nameZh": "M4A2 谢尔曼",
      "nameEn": "M4A2 Sherman",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m4a2.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m4a2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:m4a2_105",
      "entityId": "dragonrise_reforge:m4a2_105",
      "displayName": "M4A2 (105mm)",
      "nameZh": "M4A2 (105mm)",
      "nameEn": "M4A2 (105mm)",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m4a2_105.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/m4a2_105.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:maus",
      "entityId": "dragonrise_reforge:maus",
      "displayName": "Maus",
      "nameZh": "Maus",
      "nameEn": "Maus",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/maus.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/maus.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:mk19",
      "entityId": "dragonrise_reforge:mk19",
      "displayName": "MK-19",
      "nameZh": "MK-19",
      "nameEn": "MK-19",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/mk19.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/mk19.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:motuo",
      "entityId": "dragonrise_reforge:motuo",
      "displayName": "Motorcycle with Sidecar",
      "nameZh": "Motorcycle with Sidecar",
      "nameEn": "Motorcycle with Sidecar",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/motuo.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/motuo.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:nh90",
      "entityId": "dragonrise_reforge:nh90",
      "displayName": "NH90",
      "nameZh": "NH90",
      "nameEn": "NH90",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/nh90.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/nh90.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:npds114",
      "entityId": "dragonrise_reforge:npds114",
      "displayName": "NPDS114 自动炮塔",
      "nameZh": "NPDS114 自动炮塔",
      "nameEn": "NPDS114 Auto Turret",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/npds114.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/npds114.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:npds514",
      "entityId": "dragonrise_reforge:npds514",
      "displayName": "NPDS514 自动炮塔",
      "nameZh": "NPDS514 自动炮塔",
      "nameEn": "NPDS514 Auto Turret",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/npds514.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/npds514.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:npds810",
      "entityId": "dragonrise_reforge:npds810",
      "displayName": "NPDS810 自动炮塔",
      "nameZh": "NPDS810 自动炮塔",
      "nameEn": "NPDS810 Auto Turret",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/npds810.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/npds810.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:panzer4",
      "entityId": "dragonrise_reforge:panzer4",
      "displayName": "Pz.Kpfw. IV",
      "nameZh": "Pz.Kpfw. IV",
      "nameEn": "Panzer IV",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/panzer4.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/panzer4.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:pershing",
      "entityId": "dragonrise_reforge:pershing",
      "displayName": "M26 Pershing",
      "nameZh": "M26 Pershing",
      "nameEn": "M26 Pershing",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/pershing.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/pershing.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:project640",
      "entityId": "dragonrise_reforge:project640",
      "displayName": "Project 640",
      "nameZh": "Project 640",
      "nameEn": "Object 640",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/project640.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/project640.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:pzbjy",
      "entityId": "dragonrise_reforge:pzbjy",
      "displayName": "破障北金鹰",
      "nameZh": "破障北金鹰",
      "nameEn": "Obstacle Clearing Northern Golden Eagle",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/pzbjy.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/pzbjy.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:q5",
      "entityId": "dragonrise_reforge:q5",
      "displayName": "Q-5",
      "nameZh": "Q-5",
      "nameEn": "Q-5",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/q5.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/q5.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:qjz89",
      "entityId": "dragonrise_reforge:qjz89",
      "displayName": "QJZ-89",
      "nameZh": "QJZ-89",
      "nameEn": "QJZ-89",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/qjz89.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/qjz89.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:refale",
      "entityId": "dragonrise_reforge:refale",
      "displayName": "Rafale",
      "nameZh": "Rafale",
      "nameEn": "Rafale",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/refale.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/refale.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:refaleaa",
      "entityId": "dragonrise_reforge:refaleaa",
      "displayName": "Rafale",
      "nameZh": "Rafale",
      "nameEn": "Rafale (Naval)",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/refaleaa.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/refaleaa.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:sd905",
      "entityId": "dragonrise_reforge:sd905",
      "displayName": "SD 905",
      "nameZh": "SD 905",
      "nameEn": "SD 905",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/sd905.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/sd905.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:shield",
      "entityId": "dragonrise_reforge:shield",
      "displayName": "便携式护盾",
      "nameZh": "便携式护盾",
      "nameEn": "Portable Shield",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/shield.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/shield.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:spacebag",
      "entityId": "dragonrise_reforge:spacebag",
      "displayName": "太空包",
      "nameZh": "太空包",
      "nameEn": "Space Bag",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/spacebag.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/spacebag.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:strv103",
      "entityId": "dragonrise_reforge:strv103",
      "displayName": "Strv 103",
      "nameZh": "Strv 103",
      "nameEn": "Strv 103",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/strv103.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/strv103.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:sx1",
      "entityId": "dragonrise_reforge:sx1",
      "displayName": "SX-1",
      "nameZh": "SX-1",
      "nameEn": "SX-1",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/sx1.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/sx1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:syy651",
      "entityId": "dragonrise_reforge:syy651",
      "displayName": "SYY-651",
      "nameZh": "SYY-651",
      "nameEn": "SYY-651",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/syy651.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/syy651.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:t3476",
      "entityId": "dragonrise_reforge:t3476",
      "displayName": "T-34/76",
      "nameZh": "T-34/76",
      "nameEn": "T-34/76",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t3476.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t3476.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:t3485",
      "entityId": "dragonrise_reforge:t3485",
      "displayName": "T-34-85",
      "nameZh": "T-34-85",
      "nameEn": "T-34-85",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t3485.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t3485.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:t80",
      "entityId": "dragonrise_reforge:t80",
      "displayName": "T-80BVM",
      "nameZh": "T-80BVM",
      "nameEn": "T-80BVM",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t80.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t80.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:t80b",
      "entityId": "dragonrise_reforge:t80b",
      "displayName": "T-80B",
      "nameZh": "T-80B",
      "nameEn": "T-80B",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t80b.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t80b.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:t90mh",
      "entityId": "dragonrise_reforge:t90mh",
      "displayName": "T-90M",
      "nameZh": "T-90M",
      "nameEn": "T-90M",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t90mh.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t90mh.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:t90mhigh",
      "entityId": "dragonrise_reforge:t90mhigh",
      "displayName": "T-90M (高架)",
      "nameZh": "T-90M (高架)",
      "nameEn": "T-90M (Elevated)",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t90mhigh.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/t90mhigh.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:test",
      "entityId": "dragonrise_reforge:test",
      "displayName": "测试实体",
      "nameZh": "测试实体",
      "nameEn": "Test Entity",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/test.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/test.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:test_ship",
      "entityId": "dragonrise_reforge:test_ship",
      "displayName": "测试舰船",
      "nameZh": "测试舰船",
      "nameEn": "Test Ship",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/test_ship.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/test_ship.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:tiger",
      "entityId": "dragonrise_reforge:tiger",
      "displayName": "Tiger I",
      "nameZh": "Tiger I",
      "nameEn": "Tiger I",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/tiger.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/tiger.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:tjgc",
      "entityId": "dragonrise_reforge:tjgc",
      "displayName": "\"TJP-F01“天机”\"",
      "nameZh": "\"TJP-F01“天机”\"",
      "nameEn": "\"TJP-F01 Tianji\"",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/tjgc.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/tjgc.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:toyota_seiki",
      "entityId": "dragonrise_reforge:toyota_seiki",
      "displayName": "Toyota Century",
      "nameZh": "Toyota Century",
      "nameEn": "Toyota Century",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/toyota_seiki.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/toyota_seiki.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:tunguska",
      "entityId": "dragonrise_reforge:tunguska",
      "displayName": "2K22通古斯卡",
      "nameZh": "2K22通古斯卡",
      "nameEn": "2K22 Tunguska",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/tunguska.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/tunguska.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:type100",
      "entityId": "dragonrise_reforge:type100",
      "displayName": "Type 100",
      "nameZh": "Type 100",
      "nameEn": "Type 100",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type100.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type100.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:type3",
      "entityId": "dragonrise_reforge:type3",
      "displayName": "特三式内火艇",
      "nameZh": "特三式内火艇",
      "nameEn": "Type 3 Ka-Chi",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type3.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type3.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:type97",
      "entityId": "dragonrise_reforge:type97",
      "displayName": "九五式战车",
      "nameZh": "九五式战车",
      "nameEn": "Type 95 Ha-Go",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type97.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type97.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:type97q",
      "entityId": "dragonrise_reforge:type97q",
      "displayName": "九七式中战车",
      "nameZh": "九七式中战车",
      "nameEn": "Type 97 Chi-Ha",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type97q.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/type97q.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:uh60",
      "entityId": "dragonrise_reforge:uh60",
      "displayName": "UH-60",
      "nameZh": "UH-60",
      "nameEn": "UH-60 Black Hawk",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/uh60.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/uh60.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:vt4a1",
      "entityId": "dragonrise_reforge:vt4a1",
      "displayName": "VT4A1",
      "nameZh": "VT4A1",
      "nameEn": "VT4A1",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/vt4a1.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/vt4a1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:vt4b",
      "entityId": "dragonrise_reforge:vt4b",
      "displayName": "VT4B",
      "nameZh": "VT4B",
      "nameEn": "VT4B",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/vt4b.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/vt4b.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:wlhgzu23",
      "entityId": "dragonrise_reforge:wlhgzu23",
      "displayName": "五菱宏光ZU-23",
      "nameZh": "五菱宏光ZU-23",
      "nameEn": "Wuling Hongguang ZU-23",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/wlhgzu23.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/wlhgzu23.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:wlsc",
      "entityId": "dragonrise_reforge:wlsc",
      "displayName": "五菱宏光",
      "nameZh": "五菱宏光",
      "nameEn": "Wuling Hongguang",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/wlsc.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/wlsc.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:z10a",
      "entityId": "dragonrise_reforge:z10a",
      "displayName": "Z-10A",
      "nameZh": "Z-10A",
      "nameEn": "Z-10A",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z10a.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z10a.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:z10me",
      "entityId": "dragonrise_reforge:z10me",
      "displayName": "Z-10ME",
      "nameZh": "Z-10ME",
      "nameEn": "Z-10ME",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z10me.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z10me.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:z20",
      "entityId": "dragonrise_reforge:z20",
      "displayName": "Z-20",
      "nameZh": "Z-20",
      "nameEn": "Z-20",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z20.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z20.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:z9",
      "entityId": "dragonrise_reforge:z9",
      "displayName": "Z-9",
      "nameZh": "Z-9",
      "nameEn": "Z-9",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z9.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/z9.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:zbd04a",
      "entityId": "dragonrise_reforge:zbd04a",
      "displayName": "ZBD-04A",
      "nameZh": "ZBD-04A",
      "nameEn": "ZBD-04A",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zbd04a.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zbd04a.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:zbl08",
      "entityId": "dragonrise_reforge:zbl08",
      "displayName": "ZBL-08",
      "nameZh": "ZBL-08",
      "nameEn": "ZBL-08",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zbl08.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zbl08.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:zlt11",
      "entityId": "dragonrise_reforge:zlt11",
      "displayName": "ZLT-11",
      "nameZh": "ZLT-11",
      "nameEn": "ZLT-11",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zlt11.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zlt11.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:zsu234",
      "entityId": "dragonrise_reforge:zsu234",
      "displayName": "ZSU-23-4M",
      "nameZh": "ZSU-23-4M",
      "nameEn": "ZSU-23-4M",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zsu234.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zsu234.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ztq15",
      "entityId": "dragonrise_reforge:ztq15",
      "displayName": "ZTQ-15",
      "nameZh": "ZTQ-15",
      "nameEn": "ZTQ-15",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztq15.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztq15.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ztz59a",
      "entityId": "dragonrise_reforge:ztz59a",
      "displayName": "ZTZ-59A",
      "nameZh": "ZTZ-59A",
      "nameEn": "ZTZ-59A",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztz59a.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztz59a.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:ztz99a",
      "entityId": "dragonrise_reforge:ztz99a",
      "displayName": "ZTZ-99A",
      "nameZh": "ZTZ-99A",
      "nameEn": "ZTZ-99A",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztz99a.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztz99a.json",
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/ztz99bh.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "dragonrise_reforge:zu23",
      "entityId": "dragonrise_reforge:zu23",
      "displayName": "ZU-23",
      "nameZh": "ZU-23",
      "nameEn": "ZU-23",
      "source": "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zu23.json",
      "sources": [
        "mods/[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar!data/dragonrise_reforge/sbw/vehicles/zu23.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:aavp",
      "entityId": "fcp:aavp",
      "displayName": "AAVP",
      "nameZh": "",
      "nameEn": "AAVP",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/aavp/aavp.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/aavp/aavp.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/aavp/aavp.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bigbird",
      "entityId": "fcp:bigbird",
      "displayName": "fcp:bigbird",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/bigbird.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/bigbird.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp1",
      "entityId": "fcp:bmp1",
      "displayName": "BMP-1",
      "nameZh": "",
      "nameEn": "BMP-1",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp1am",
      "entityId": "fcp:bmp1am",
      "displayName": "BMP-1AM",
      "nameZh": "",
      "nameEn": "BMP-1AM",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1am.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1am.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp1am.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp1p",
      "entityId": "fcp:bmp1p",
      "displayName": "BMP-1P",
      "nameZh": "",
      "nameEn": "BMP-1P",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1p.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1p.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp1p.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp1u",
      "entityId": "fcp:bmp1u",
      "displayName": "BMP-1U",
      "nameZh": "",
      "nameEn": "BMP-1U",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1u.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp1u.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp1u.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp2",
      "entityId": "fcp:bmp2",
      "displayName": "BMP-2",
      "nameZh": "",
      "nameEn": "BMP-2",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp2_noatgm",
      "entityId": "fcp:bmp2_noatgm",
      "displayName": "BMP-2",
      "nameZh": "",
      "nameEn": "BMP-2",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2_noatgm.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2_noatgm.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp2_noatgm.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp2d",
      "entityId": "fcp:bmp2d",
      "displayName": "BMP-2D",
      "nameZh": "",
      "nameEn": "BMP-2D",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2d.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2d.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp2d.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp2m",
      "entityId": "fcp:bmp2m",
      "displayName": "BMP-2M",
      "nameZh": "",
      "nameEn": "BMP-2M",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2m.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2m.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp2m.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:bmp2md",
      "entityId": "fcp:bmp2md",
      "displayName": "BMP-2MD",
      "nameZh": "",
      "nameEn": "BMP-2MD",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2md.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/bmp/bmp2md.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/bmp/bmp2md.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:brdm2",
      "entityId": "fcp:brdm2",
      "displayName": "BRDM-2",
      "nameZh": "",
      "nameEn": "BRDM-2",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/brdm/brdm2.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/brdm/brdm2.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/brdm/brdm2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr3e",
      "entityId": "fcp:btr3e",
      "displayName": "BTR-3E",
      "nameZh": "",
      "nameEn": "BTR-3E",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr3e.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr3e.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr3e.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr4mv1",
      "entityId": "fcp:btr4mv1",
      "displayName": "BTR-4MV1",
      "nameZh": "",
      "nameEn": "BTR-4MV1",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr4mv1.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr4mv1.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr4mv1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr80",
      "entityId": "fcp:btr80",
      "displayName": "BTR-80",
      "nameZh": "",
      "nameEn": "BTR-80",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr80.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr80.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr80.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr80_cope",
      "entityId": "fcp:btr80_cope",
      "displayName": "BTR-80 (Cope Cage)",
      "nameZh": "",
      "nameEn": "BTR-80 (Cope Cage)",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr80_cope.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr80_cope.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr80_cope.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr82",
      "entityId": "fcp:btr82",
      "displayName": "BTR82A",
      "nameZh": "",
      "nameEn": "BTR82A",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr82.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr82.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr82.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr82_cope",
      "entityId": "fcp:btr82_cope",
      "displayName": "BTR-82A (Cope Cage)",
      "nameZh": "",
      "nameEn": "BTR-82A (Cope Cage)",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr82_cope.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr82_cope.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr82_cope.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:btr82at",
      "entityId": "fcp:btr82at",
      "displayName": "BTR-82AT",
      "nameZh": "",
      "nameEn": "BTR-82AT",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr82at.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/btr/btr82at.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/btr/btr82at.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:combine",
      "entityId": "fcp:combine",
      "displayName": "John Deere Combine",
      "nameZh": "",
      "nameEn": "John Deere Combine",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/farm/combine.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/farm/combine.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:cultivator",
      "entityId": "fcp:cultivator",
      "displayName": "Cultivator",
      "nameZh": "",
      "nameEn": "Cultivator",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/trailers/cultivator.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/trailers/cultivator.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:dpv_m240",
      "entityId": "fcp:dpv_m240",
      "displayName": "DPV with M240",
      "nameZh": "",
      "nameEn": "DPV with M240",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/dpv_m240.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/dpv_m240.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:dpv_minigun",
      "entityId": "fcp:dpv_minigun",
      "displayName": "DPV with Minigun",
      "nameZh": "",
      "nameEn": "DPV with Minigun",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/dpv_minigun.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/dpv_minigun.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_ags17",
      "entityId": "fcp:empl_ags17",
      "displayName": "AGS-17 Emplacement",
      "nameZh": "",
      "nameEn": "AGS-17 Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_ags17.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_ags17.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_dshk",
      "entityId": "fcp:empl_dshk",
      "displayName": "DShK Emplacement",
      "nameZh": "",
      "nameEn": "DShK Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_dshk.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_dshk.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_kornet",
      "entityId": "fcp:empl_kornet",
      "displayName": "Kornet Emplacement",
      "nameZh": "",
      "nameEn": "Kornet Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_kornet.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_kornet.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_m2",
      "entityId": "fcp:empl_m2",
      "displayName": "M2 Emplacement",
      "nameZh": "",
      "nameEn": "M2 Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_m2.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_m2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_mg3",
      "entityId": "fcp:empl_mg3",
      "displayName": "MG3 Emplacement",
      "nameZh": "",
      "nameEn": "MG3 Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_mg3.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_mg3.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_mk19",
      "entityId": "fcp:empl_mk19",
      "displayName": "Mk19 Emplacement",
      "nameZh": "",
      "nameEn": "Mk19 Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_mk19.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_mk19.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_tow",
      "entityId": "fcp:empl_tow",
      "displayName": "TOW Emplacement",
      "nameZh": "",
      "nameEn": "TOW Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_tow.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_tow.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:empl_zis3",
      "entityId": "fcp:empl_zis3",
      "displayName": "ZiS-3 Emplacement",
      "nameZh": "",
      "nameEn": "ZiS-3 Emplacement",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_zis3.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/emplacements/empl_zis3.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:example_trailer",
      "entityId": "fcp:example_trailer",
      "displayName": "fcp:example_trailer",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/trailers/example_trailer.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/trailers/example_trailer.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:fmtv",
      "entityId": "fcp:fmtv",
      "displayName": "FMTV",
      "nameZh": "",
      "nameEn": "FMTV",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/fmtv.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/fmtv.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:gaz_tigr",
      "entityId": "fcp:gaz_tigr",
      "displayName": "Gaz Tigr",
      "nameZh": "",
      "nameEn": "Gaz Tigr",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:gaz_tigr_gl",
      "entityId": "fcp:gaz_tigr_gl",
      "displayName": "Gaz Tigr With MK19",
      "nameZh": "",
      "nameEn": "Gaz Tigr With MK19",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr_gl.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr_gl.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:gaz_tigr_mg",
      "entityId": "fcp:gaz_tigr_mg",
      "displayName": "Gaz Tigr With Machine Gun",
      "nameZh": "",
      "nameEn": "Gaz Tigr With Machine Gun",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr_mg.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr_mg.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:gaz_tigr_rws",
      "entityId": "fcp:gaz_tigr_rws",
      "displayName": "Gaz Tigr RWS",
      "nameZh": "",
      "nameEn": "Gaz Tigr RWS",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr_rws.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/gaz_tigr/gaz_tigr_rws.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_ambulance",
      "entityId": "fcp:hmmwv_ambulance",
      "displayName": "HMMWV Ambulance",
      "nameZh": "",
      "nameEn": "HMMWV Ambulance",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_ambulance.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_ambulance.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_armored_m2",
      "entityId": "fcp:hmmwv_armored_m2",
      "displayName": "HMMWV Armored M2",
      "nameZh": "",
      "nameEn": "HMMWV Armored M2",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_armored_m2.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_armored_m2.json"
      ],
      "evidence": "vehicle JSON ID",
      "currentFormationNames": [
        "悍马 M2"
      ],
      "usedInCurrentFormations": true
    },
    {
      "id": "fcp:hmmwv_armored_mk19",
      "entityId": "fcp:hmmwv_armored_mk19",
      "displayName": "HMMWV Armored MK19",
      "nameZh": "",
      "nameEn": "HMMWV Armored MK19",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_armored_mk19.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_armored_mk19.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_armored_unarmed",
      "entityId": "fcp:hmmwv_armored_unarmed",
      "displayName": "HMMWV Armored Unarmed",
      "nameZh": "",
      "nameEn": "HMMWV Armored Unarmed",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_armored_unarmed.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_armored_unarmed.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_asrad",
      "entityId": "fcp:hmmwv_asrad",
      "displayName": "HMMWV ASRAD",
      "nameZh": "",
      "nameEn": "HMMWV ASRAD",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_asrad.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_asrad.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_avenger",
      "entityId": "fcp:hmmwv_avenger",
      "displayName": "HMMWV Avenger",
      "nameZh": "",
      "nameEn": "HMMWV Avenger",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_avenger.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_avenger.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_cargo",
      "entityId": "fcp:hmmwv_cargo",
      "displayName": "HMMWV Cargo",
      "nameZh": "",
      "nameEn": "HMMWV Cargo",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_cargo.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_cargo.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_shelter",
      "entityId": "fcp:hmmwv_shelter",
      "displayName": "HMMWV Shelter",
      "nameZh": "",
      "nameEn": "HMMWV Shelter",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_shelter.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_shelter.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_soft_top",
      "entityId": "fcp:hmmwv_soft_top",
      "displayName": "HMMWV Soft Top",
      "nameZh": "",
      "nameEn": "HMMWV Soft Top",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_soft_top.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_soft_top.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_soft_top_no_doors",
      "entityId": "fcp:hmmwv_soft_top_no_doors",
      "displayName": "fcp:hmmwv_soft_top_no_doors",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_soft_top_no_doors.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_soft_top_no_doors.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_unarmored_m2",
      "entityId": "fcp:hmmwv_unarmored_m2",
      "displayName": "HMMWV Unarmored M2",
      "nameZh": "",
      "nameEn": "HMMWV Unarmored M2",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_m2.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_m2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_unarmored_m2_shield",
      "entityId": "fcp:hmmwv_unarmored_m2_shield",
      "displayName": "HMMWV Unarmored M2 Shield",
      "nameZh": "",
      "nameEn": "HMMWV Unarmored M2 Shield",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_m2_shield.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_m2_shield.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_unarmored_m2_turret",
      "entityId": "fcp:hmmwv_unarmored_m2_turret",
      "displayName": "HMMWV Unarmored M2 Turret",
      "nameZh": "",
      "nameEn": "HMMWV Unarmored M2 Turret",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_m2_turret.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_m2_turret.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_unarmored_tow",
      "entityId": "fcp:hmmwv_unarmored_tow",
      "displayName": "HMMWV Unarmored TOW",
      "nameZh": "",
      "nameEn": "HMMWV Unarmored TOW",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_tow.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_tow.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_unarmored_tow_turret",
      "entityId": "fcp:hmmwv_unarmored_tow_turret",
      "displayName": "HMMWV Unarmored TOW Turret",
      "nameZh": "",
      "nameEn": "HMMWV Unarmored TOW Turret",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_tow_turret.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_tow_turret.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:hmmwv_unarmored_unarmed",
      "entityId": "fcp:hmmwv_unarmored_unarmed",
      "displayName": "HMMWV Unarmored Unarmed",
      "nameZh": "",
      "nameEn": "HMMWV Unarmored Unarmed",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_unarmed.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/hummwv/hmmwv_unarmored_unarmed.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:huey",
      "entityId": "fcp:huey",
      "displayName": "Huey",
      "nameZh": "",
      "nameEn": "Huey",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:huey_door_gunner_m134",
      "entityId": "fcp:huey_door_gunner_m134",
      "displayName": "Huey With M134",
      "nameZh": "",
      "nameEn": "Huey With M134",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey_door_gunner_m134.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey_door_gunner_m134.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:huey_door_gunner_m60",
      "entityId": "fcp:huey_door_gunner_m60",
      "displayName": "Huey With M60",
      "nameZh": "",
      "nameEn": "Huey With M60",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey_door_gunner_m60.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey_door_gunner_m60.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:huey_rockets",
      "entityId": "fcp:huey_rockets",
      "displayName": "Huey With Rockets",
      "nameZh": "",
      "nameEn": "Huey With Rockets",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey_rockets.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/huey_rockets.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:john_deere",
      "entityId": "fcp:john_deere",
      "displayName": "John Deere Tractor",
      "nameZh": "",
      "nameEn": "John Deere Tractor",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/farm/john_deere.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/farm/john_deere.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:kamaz",
      "entityId": "fcp:kamaz",
      "displayName": "Kamaz",
      "nameZh": "",
      "nameEn": "Kamaz",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kamaz.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kamaz.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:kamaz_kung",
      "entityId": "fcp:kamaz_kung",
      "displayName": "Kamaz Kung",
      "nameZh": "",
      "nameEn": "Kamaz Kung",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kamaz_kung.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kamaz_kung.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:kamaz_long",
      "entityId": "fcp:kamaz_long",
      "displayName": "Kamaz Long",
      "nameZh": "",
      "nameEn": "Kamaz Long",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kamaz_long.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kamaz_long.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:kozak2m1",
      "entityId": "fcp:kozak2m1",
      "displayName": "Kozak-2M1",
      "nameZh": "",
      "nameEn": "Kozak-2M1",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kozak2m1.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kozak2m1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:kozak5",
      "entityId": "fcp:kozak5",
      "displayName": "Kozak-5",
      "nameZh": "",
      "nameEn": "Kozak-5",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kozak5.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kozak5.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:kozak_ambulance",
      "entityId": "fcp:kozak_ambulance",
      "displayName": "Kozak Ambulance",
      "nameZh": "",
      "nameEn": "Kozak Ambulance",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kozak_ambulance.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/kozak_ambulance.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:lav25",
      "entityId": "fcp:lav25",
      "displayName": "LAV-25",
      "nameZh": "",
      "nameEn": "LAV-25",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/lav/lav25.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/lav/lav25.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/lav/lav25.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:littlebird",
      "entityId": "fcp:littlebird",
      "displayName": "MH-6 Littlebird",
      "nameZh": "",
      "nameEn": "MH-6 Littlebird",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/littlebird.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/littlebird.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:littlebird_armed",
      "entityId": "fcp:littlebird_armed",
      "displayName": "AH-6 Littlebird",
      "nameZh": "",
      "nameEn": "AH-6 Littlebird",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/littlebird_armed.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/littlebird_armed.json"
      ],
      "evidence": "vehicle JSON ID",
      "currentFormationNames": [
        "小鸟 机枪版"
      ],
      "usedInCurrentFormations": true
    },
    {
      "id": "fcp:m109",
      "entityId": "fcp:m109",
      "displayName": "M109 Paladin",
      "nameZh": "",
      "nameEn": "M109 Paladin",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/m109.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/m109.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:m939",
      "entityId": "fcp:m939",
      "displayName": "M939 Truck",
      "nameZh": "",
      "nameEn": "M939 Truck",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/m939.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/m939.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:matv",
      "entityId": "fcp:matv",
      "displayName": "MATV",
      "nameZh": "",
      "nameEn": "MATV",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:matv_9in1",
      "entityId": "fcp:matv_9in1",
      "displayName": "fcp:matv_9in1",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv_9in1.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv_9in1.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:matv_crow",
      "entityId": "fcp:matv_crow",
      "displayName": "MATV With CROWS",
      "nameZh": "",
      "nameEn": "MATV With CROWS",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv_crow.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv_crow.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:matv_tow",
      "entityId": "fcp:matv_tow",
      "displayName": "MATV With TOW",
      "nameZh": "",
      "nameEn": "MATV With TOW",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv_tow.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/matv/matv_tow.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:mi17",
      "entityId": "fcp:mi17",
      "displayName": "MI-17",
      "nameZh": "",
      "nameEn": "MI-17",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/mi17.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/mi17.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:msta",
      "entityId": "fcp:msta",
      "displayName": "2S19 Msta-S",
      "nameZh": "",
      "nameEn": "2S19 Msta-S",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/msta.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/msta.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:novator",
      "entityId": "fcp:novator",
      "displayName": "Novator",
      "nameZh": "",
      "nameEn": "Novator",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/novator.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/novator.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:novator_unarmed",
      "entityId": "fcp:novator_unarmed",
      "displayName": "Novator (Unarmed)",
      "nameZh": "",
      "nameEn": "Novator (Unarmed)",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/novator_unarmed.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/novator_unarmed.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:pantsir",
      "entityId": "fcp:pantsir",
      "displayName": "Pantsir-S1",
      "nameZh": "",
      "nameEn": "Pantsir-S1",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/pantsir.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/pantsir.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:seeder",
      "entityId": "fcp:seeder",
      "displayName": "Seeder",
      "nameZh": "",
      "nameEn": "Seeder",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/trailers/seeder.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/trailers/seeder.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:stryker_dragoon",
      "entityId": "fcp:stryker_dragoon",
      "displayName": "Stryker Dragoon",
      "nameZh": "",
      "nameEn": "Stryker Dragoon",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_dragoon.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_dragoon.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/stryker/stryker_dragoon.json"
      ],
      "evidence": "vehicle JSON ID",
      "currentFormationNames": [
        "M1296 龙骑兵"
      ],
      "usedInCurrentFormations": true
    },
    {
      "id": "fcp:stryker_m2",
      "entityId": "fcp:stryker_m2",
      "displayName": "Stryker M2",
      "nameZh": "",
      "nameEn": "Stryker M2",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_m2.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_m2.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/stryker/stryker_m2.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:stryker_mgs",
      "entityId": "fcp:stryker_mgs",
      "displayName": "Stryker MGS",
      "nameZh": "",
      "nameEn": "Stryker MGS",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_mgs.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_mgs.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/stryker/stryker_mgs.json"
      ],
      "evidence": "vehicle JSON ID",
      "currentFormationNames": [
        "M1128 MGS"
      ],
      "usedInCurrentFormations": true
    },
    {
      "id": "fcp:stryker_mk19",
      "entityId": "fcp:stryker_mk19",
      "displayName": "Stryker Mk-19",
      "nameZh": "",
      "nameEn": "Stryker Mk-19",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_mk19.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_mk19.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/stryker/stryker_mk19.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:stryker_mortar",
      "entityId": "fcp:stryker_mortar",
      "displayName": "Stryker Mortar",
      "nameZh": "",
      "nameEn": "Stryker Mortar",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/stryker_mortar.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/stryker_mortar.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:stryker_tow",
      "entityId": "fcp:stryker_tow",
      "displayName": "Stryker TOW",
      "nameZh": "",
      "nameEn": "Stryker TOW",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_tow.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/stryker/stryker_tow.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/stryker/stryker_tow.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:t14_armata",
      "entityId": "fcp:t14_armata",
      "displayName": "fcp:t14_armata",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/t14_armata.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/t14_armata.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:t72av",
      "entityId": "fcp:t72av",
      "displayName": "T72-AV",
      "nameZh": "",
      "nameEn": "T72-AV",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/t72/t72av.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/multicrew/t72/t72av.json",
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/single_crew/t72/t72av.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:toyota_hilux",
      "entityId": "fcp:toyota_hilux",
      "displayName": "Toyota Hilux",
      "nameZh": "",
      "nameEn": "Toyota Hilux",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:toyota_hilux_bmp",
      "entityId": "fcp:toyota_hilux_bmp",
      "displayName": "Toyota Hilux With BMP",
      "nameZh": "",
      "nameEn": "Toyota Hilux With BMP",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_bmp.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_bmp.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:toyota_hilux_mortar",
      "entityId": "fcp:toyota_hilux_mortar",
      "displayName": "Toyota Hilux Mortar",
      "nameZh": "",
      "nameEn": "Toyota Hilux Mortar",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_mortar.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_mortar.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:toyota_hilux_rocket_pod",
      "entityId": "fcp:toyota_hilux_rocket_pod",
      "displayName": "Toyota Hilux With Rocket Pod",
      "nameZh": "",
      "nameEn": "Toyota Hilux With Rocket Pod",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_rocket_pod.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_rocket_pod.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:toyota_hilux_spg9",
      "entityId": "fcp:toyota_hilux_spg9",
      "displayName": "Toyota Hilux With SPG-9",
      "nameZh": "",
      "nameEn": "Toyota Hilux With SPG-9",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_spg9.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_spg9.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:toyota_hilux_zu23",
      "entityId": "fcp:toyota_hilux_zu23",
      "displayName": "fcp:toyota_hilux_zu23",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_zu23.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/toyota/toyota_hilux_zu23.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:uaz",
      "entityId": "fcp:uaz",
      "displayName": "UAZ",
      "nameZh": "",
      "nameEn": "UAZ",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/uaz.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/uaz.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:uaz_dshka",
      "entityId": "fcp:uaz_dshka",
      "displayName": "UAZ With DSHKA",
      "nameZh": "",
      "nameEn": "UAZ With DSHKA",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/uaz_dshka.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/uaz_dshka.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:uaz_spg9",
      "entityId": "fcp:uaz_spg9",
      "displayName": "fcp:uaz_spg9",
      "nameZh": "",
      "nameEn": "",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/uaz_spg9.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/uaz_spg9.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:ural",
      "entityId": "fcp:ural",
      "displayName": "Ural",
      "nameZh": "",
      "nameEn": "Ural",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:ural_fuel",
      "entityId": "fcp:ural_fuel",
      "displayName": "Ural Fuel Truck",
      "nameZh": "",
      "nameEn": "Ural Fuel Truck",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural_fuel.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural_fuel.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:ural_grad",
      "entityId": "fcp:ural_grad",
      "displayName": "Ural With Grad",
      "nameZh": "",
      "nameEn": "Ural With Grad",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural_grad.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural_grad.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:ural_kung",
      "entityId": "fcp:ural_kung",
      "displayName": "Ural Kung",
      "nameZh": "",
      "nameEn": "Ural Kung",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural_kung.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/unchanged/ural/ural_kung.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:venom",
      "entityId": "fcp:venom",
      "displayName": "Venom",
      "nameZh": "",
      "nameEn": "Venom",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/venom.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/venom.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "fcp:viper",
      "entityId": "fcp:viper",
      "displayName": "Viper",
      "nameZh": "",
      "nameEn": "Viper",
      "source": "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/viper.json",
      "sources": [
        "mods/fcp-1.2.1.jar!data/fcp/sbw/vehicles/helicopter/viper.json"
      ],
      "evidence": "vehicle JSON ID"
    },
    {
      "id": "superbwarfare:a_10a",
      "entityId": "superbwarfare:a_10a",
      "displayName": "A-10 “雷电II” 攻击机",
      "nameZh": "A-10 “雷电II” 攻击机",
      "nameEn": "A-10 Thunderbolt II",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/a_10a.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/a_10a.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:ah_6",
      "entityId": "superbwarfare:ah_6",
      "displayName": "AH-6 小鸟直升机",
      "nameZh": "AH-6 小鸟直升机",
      "nameEn": "AH-6 Little Bird",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/ah_6.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/ah_6.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:annihilator",
      "entityId": "superbwarfare:annihilator",
      "displayName": "歼灭者能量炮",
      "nameZh": "歼灭者能量炮",
      "nameEn": "Annihilator Energy Cannon",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/annihilator.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/annihilator.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:bl_132",
      "entityId": "superbwarfare:bl_132",
      "displayName": "130mm/58 BL-132",
      "nameZh": "130mm/58 BL-132",
      "nameEn": "130mm/58 BL-132",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/bl_132.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/bl_132.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:bmp_2",
      "entityId": "superbwarfare:bmp_2",
      "displayName": "BMP-2 履带式步兵战车",
      "nameZh": "BMP-2 履带式步兵战车",
      "nameEn": "BMP-2",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/bmp_2.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/bmp_2.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:bradley",
      "entityId": "superbwarfare:bradley",
      "displayName": "M2 “布拉德利”履带式步兵战车",
      "nameZh": "M2 “布拉德利”履带式步兵战车",
      "nameEn": "M2 Bradley",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/bradley.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/bradley.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:drone",
      "entityId": "superbwarfare:drone",
      "displayName": "无人机",
      "nameZh": "无人机",
      "nameEn": "Drone",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/drone.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/drone.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:hpj_11",
      "entityId": "superbwarfare:hpj_11",
      "displayName": "H/PJ-11近防炮",
      "nameZh": "H/PJ-11近防炮",
      "nameEn": "H/PJ-11 CIWS",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/hpj_11.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/hpj_11.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:ju_87",
      "entityId": "superbwarfare:ju_87",
      "displayName": "Ju-87 斯图卡轰炸机",
      "nameZh": "Ju-87 斯图卡轰炸机",
      "nameEn": "Ju-87 Stuka Bomber",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/ju_87.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/ju_87.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:kv_16",
      "entityId": "superbwarfare:kv_16",
      "displayName": "KV-16 “幽灵” 战斗机",
      "nameZh": "KV-16 “幽灵” 战斗机",
      "nameEn": "KV-16 Ghost Fighter",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/kv_16.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/kv_16.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:laser_tower",
      "entityId": "superbwarfare:laser_tower",
      "displayName": "激光防御塔",
      "nameZh": "激光防御塔",
      "nameEn": "Laser Defense Tower",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/laser_tower.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/laser_tower.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:lav_150",
      "entityId": "superbwarfare:lav_150",
      "displayName": "LAV-150 轮式步兵战车",
      "nameZh": "LAV-150 轮式步兵战车",
      "nameEn": "LAV-150 Commando",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/lav_150.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/lav_150.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:lav_25",
      "entityId": "superbwarfare:lav_25",
      "displayName": "LAV-25 轮式步兵战车",
      "nameZh": "LAV-25 轮式步兵战车",
      "nameEn": "LAV-25",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/lav_25.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/lav_25.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:lav_ad",
      "entityId": "superbwarfare:lav_ad",
      "displayName": "LAV-AD 防空车",
      "nameZh": "LAV-AD 防空车",
      "nameEn": "LAV-AD AAV",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/lav_ad.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/lav_ad.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:m_1a_2",
      "entityId": "superbwarfare:m_1a_2",
      "displayName": "M1A2主战坦克",
      "nameZh": "M1A2主战坦克",
      "nameEn": "M1A2 MBT",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/m_1a_2.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/m_1a_2.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:mi_28",
      "entityId": "superbwarfare:mi_28",
      "displayName": "Mi-28 武装直升机",
      "nameZh": "Mi-28 武装直升机",
      "nameEn": "Mi-28 Attack helicopter",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mi_28.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mi_28.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:mk_42",
      "entityId": "superbwarfare:mk_42",
      "displayName": "5''/54 Mk42",
      "nameZh": "5''/54 Mk42",
      "nameEn": "5''/54 Mk42",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mk_42.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mk_42.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:mle_1934",
      "entityId": "superbwarfare:mle_1934",
      "displayName": "138.6mm50 Mle1934 R1938",
      "nameZh": "138.6mm50 Mle1934 R1938",
      "nameEn": "138.6mm50 Mle1934 R1938",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mle_1934.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mle_1934.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:mortar",
      "entityId": "superbwarfare:mortar",
      "displayName": "迫击炮",
      "nameZh": "迫击炮",
      "nameEn": "Mortar",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mortar.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/mortar.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:plz_05",
      "entityId": "superbwarfare:plz_05",
      "displayName": "PLZ-05自行火炮",
      "nameZh": "PLZ-05自行火炮",
      "nameEn": "PLZ-05 Self-Propelled Artillery",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/plz_05.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/plz_05.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:prism_tank",
      "entityId": "superbwarfare:prism_tank",
      "displayName": "光棱坦克",
      "nameZh": "光棱坦克",
      "nameEn": "Prism Tank",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/prism_tank.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/prism_tank.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:sodayo_pick_up",
      "entityId": "superbwarfare:sodayo_pick_up",
      "displayName": "速德优 TenEven9型 皮卡车",
      "nameZh": "速德优 TenEven9型 皮卡车",
      "nameEn": "Sodayo TenEven9 Pickup",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:sodayo_pick_up_hmg",
      "entityId": "superbwarfare:sodayo_pick_up_hmg",
      "displayName": "速德优 TenEven9型 皮卡车(重机枪)",
      "nameZh": "速德优 TenEven9型 皮卡车(重机枪)",
      "nameEn": "Sodayo TenEven9 Pickup(HMG)",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up_hmg.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up_hmg.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:sodayo_pick_up_rocket",
      "entityId": "superbwarfare:sodayo_pick_up_rocket",
      "displayName": "速德优 TenEven9型 皮卡车(火箭炮)",
      "nameZh": "速德优 TenEven9型 皮卡车(火箭炮)",
      "nameEn": "Sodayo TenEven9 Pickup(Rocket)",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up_rocket.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up_rocket.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:sodayo_pick_up_tow",
      "entityId": "superbwarfare:sodayo_pick_up_tow",
      "displayName": "速德优 TenEven9型 皮卡车(陶氏)",
      "nameZh": "速德优 TenEven9型 皮卡车(陶氏)",
      "nameEn": "Sodayo TenEven9 Pickup(Tow)",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up_tow.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/sodayo_pick_up_tow.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:speedboat",
      "entityId": "superbwarfare:speedboat",
      "displayName": "快艇",
      "nameZh": "快艇",
      "nameEn": "Speedboat",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/speedboat.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/speedboat.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:t_90a",
      "entityId": "superbwarfare:t_90a",
      "displayName": "T-90A主战坦克",
      "nameZh": "T-90A主战坦克",
      "nameEn": "T-90A MBT",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/t_90a.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/t_90a.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:tiny_speedboat",
      "entityId": "superbwarfare:tiny_speedboat",
      "displayName": "迷你快艇",
      "nameZh": "迷你快艇",
      "nameEn": "Tiny Speedboat",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/tiny_speedboat.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/tiny_speedboat.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:tom_6",
      "entityId": "superbwarfare:tom_6",
      "displayName": "汤姆 F6F",
      "nameZh": "汤姆 F6F",
      "nameEn": "Tom F6F",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/tom_6.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/tom_6.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:tow",
      "entityId": "superbwarfare:tow",
      "displayName": "陶式反坦克导弹发射器",
      "nameZh": "陶式反坦克导弹发射器",
      "nameEn": "TOW Anti-tank Missile Launcher",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/tow.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/tow.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:truck",
      "entityId": "superbwarfare:truck",
      "displayName": "泥头车",
      "nameZh": "泥头车",
      "nameEn": "Truck",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/truck.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/truck.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:type_63",
      "entityId": "superbwarfare:type_63",
      "displayName": "63式107mm多管火箭炮",
      "nameZh": "63式107mm多管火箭炮",
      "nameEn": "Type-63 107mm MLRS",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/type_63.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/type_63.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:vehicle_assembling_table",
      "entityId": "superbwarfare:vehicle_assembling_table",
      "displayName": "载具装配台",
      "nameZh": "载具装配台",
      "nameEn": "Vehicle Assembling Table",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/vehicle_assembling_table.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/vehicle_assembling_table.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:waveforce_tower",
      "entityId": "superbwarfare:waveforce_tower",
      "displayName": "波能塔",
      "nameZh": "波能塔",
      "nameEn": "Wave-Force Tower",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/waveforce_tower.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/waveforce_tower.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:wheel_chair",
      "entityId": "superbwarfare:wheel_chair",
      "displayName": "轮椅",
      "nameZh": "轮椅",
      "nameEn": "WheelChair",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/wheel_chair.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/wheel_chair.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:yx_100",
      "entityId": "superbwarfare:yx_100",
      "displayName": "YX-100 主战坦克",
      "nameZh": "YX-100 主战坦克",
      "nameEn": "YX-100 MBT",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/yx_100.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/yx_100.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    },
    {
      "id": "superbwarfare:ztz_99a",
      "entityId": "superbwarfare:ztz_99a",
      "displayName": "ZTZ-99A主战坦克",
      "nameZh": "ZTZ-99A主战坦克",
      "nameEn": "ZTZ-99A MBT",
      "source": "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/ztz_99a.json",
      "sources": [
        "mods/superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar!data/superbwarfare/sbw/vehicles/ztz_99a.json"
      ],
      "evidence": "vehicle data resource ID + matching entity translation"
    }
  ],
  "scanWarnings": []
};
