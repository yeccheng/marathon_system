ALTER TABLE marathon_event ADD COLUMN guide_json text NULL;
ALTER TABLE marathon_event MODIFY COLUMN cancel_rule varchar(800) NULL;
ALTER TABLE marathon_event MODIFY COLUMN signup_rule varchar(800) NULL;
ALTER TABLE marathon_event MODIFY COLUMN pickup_info varchar(500) NULL;

UPDATE marathon_event SET
  cancel_rule='比赛日前 7 天（不含比赛日）可在线退赛并原路退回报名费；此后仅支持比赛日现场医务退赛，报名费按组委会现场规定处理。退款一般 3–10 个工作日到账。',
  signup_rule='年满 16 周岁；报名时须如实填写身份信息；领物时出示身份证件与电子确认函。完赛后可在个人中心下载成绩证明。',
  pickup_info='赛前 2–3 天于西安国际会展中心领物；请携带身份证件与报名确认函；参赛服按报名尺码发放。'
WHERE id=1;

UPDATE marathon_event SET
  cancel_rule='比赛日前 7 天（不含比赛日）可在线退赛并原路退回报名费；逾期请联系组委会人工处理。退款一般 3–10 个工作日到账。',
  signup_rule='年满 16 周岁；半程项目建议有一定跑步基础；领物时核对照片与证件。',
  pickup_info='赛前于城墙景区指定点领物；请携带身份证件与确认函。'
WHERE id=2;

UPDATE marathon_event SET
  cancel_rule='比赛日前 7 天可在线退赛退费；迷你跑名额紧张，请尽早确认行程。',
  signup_rule='适合家庭与大众跑者；儿童需监护人陪同报名。',
  pickup_info='比赛日当天现场领物；请提前到达曲江池南门集合区。'
WHERE id=3;

UPDATE marathon_event SET guide_json='{
  "handbook":"一、参赛资格：年满16周岁，身体健康，适合长跑。\\n二、体检证明：建议赛前3个月内在二级及以上医院完成体检，重点关注心电图与血压；组委会有权抽查。\\n三、参赛包：号码布、计时芯片、参赛服、物品寄存袋。\\n四、成绩证明：完赛后可在「个人中心」查看枪声/净成绩，并下载成绩证明图；官方证书于赛后15个工作日内开放。",
  "medical":"请如实申报既往病史。如有心脏病、高血压未控制等情况，请勿勉强参赛。比赛日现场设医疗点与AED。",
  "insurance":"报名费已含基础赛事意外险。如需提高保额，可在支付成功后于个人中心「保险说明」查看合作渠道自行加购，费用自理。退赛成功后退还报名费，保险按保险公司规则办理。",
  "transport":"地铁：大雁塔站 / 大唐不夜城站步行可达起点区。自驾：建议停「大唐不夜城地下停车场」或曲江池周边停车场，赛日部分道路临时管制，请提前查封路公告。",
  "hotel":"附近可选：西安凯悦酒店、曲江宾馆、大唐芙蓉园周边民宿。建议赛前一晚入住起点 3 公里内，避免早高峰。",
  "contact":"组委会热线：029-8888-2026（工作日 9:00–18:00）\\n赛事邮箱：race@gunstart.xian\\n现场服务台：领物处与起点医疗服务点"
}' WHERE id=1;

UPDATE marathon_event SET guide_json='{
  "handbook":"一、半程马拉松，建议具备连续跑完 15 公里能力。\\n二、号码布须佩戴胸前，严禁遮挡芯片。\\n三、成绩与证书：完赛后个人中心可查分段与总成绩。",
  "medical":"城墙段落差与转弯较多，注意补给与热身。现场医务站分布于各补给点。",
  "insurance":"报名费含基础意外险。加购方式见支付成功后的站内通知。",
  "transport":"地铁：永宁门 / 钟楼站；城墙附近道路赛日管制，建议公共交通。",
  "hotel":"推荐南门、钟楼、回民街周边酒店，步行或打车至集合点。",
  "contact":"组委会热线：029-8888-2026\\n邮箱：race@gunstart.xian"
}' WHERE id=2;

UPDATE marathon_event SET guide_json='{
  "handbook":"迷你欢乐跑以体验为主，无严格关门成绩要求；完赛均可领取纪念奖牌（数量有限，领完为止）。",
  "medical":"家庭组请自行照顾儿童安全，注意防晒补水。",
  "insurance":"含基础意外险；儿童投保须监护人同意。",
  "transport":"曲江池南门临停位有限，建议地铁大唐不夜城站。",
  "hotel":"曲江池、不夜城沿线酒店民宿均可。",
  "contact":"组委会热线：029-8888-2026\\n现场问询：南门服务帐篷"
}' WHERE id=3;
