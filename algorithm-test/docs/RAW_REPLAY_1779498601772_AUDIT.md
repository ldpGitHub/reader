# Raw replay 1779498601772 manual audit

Date: 2026-05-23

Snapshot:

- Local path: `algorithm-test/build/raw-corpus-replay-1779498601772`
- Completed books in snapshot: 33
- Audit rows: 276
- Suggestion rows: 72
- Books with suggestions: 21
- Total suggestions: 83

This audit is based on direct reading of the pulled `audit-extracts/*.txt`
files. It verifies the current sampled replay output only; it does not certify
the full 100-book corpus.

## Confirmed true positives

These reported chapters contain obvious mixed-source or wrong-book text. The
algorithm was right to reject them as story pollution.

| Book | Chapter | Manual result | Evidence summary |
| --- | ---: | --- | --- |
| `玄鉴仙族` | 1636 | true positive | Starts with valid `陈迹/山长/陆阳` style content, then switches into unrelated `沐秋/叶逸/吴主任/林安县/赵雨菲` fragments. |
| `玄鉴仙族` | 1640 | true positive | Same tail-source corruption pattern, with unrelated modern/war/other-fantasy fragments. |
| `道爷要飞升` | 617 | true positive | Mixed `木汐子/未来世界/姜圣皇/精灵国/岳风/清军/神宫悠` fragments. |
| `从水猴子开始成神` | 1521 | true positive | Mixed `天辰大殿/龙象王/李知尘/戴维森/华仔/崔呈秀` fragments. |
| `仙工开物` | 1034 | true positive | Valid `宁拙/青簧子` prefix, then unrelated `白龙马/姜寒/刘维/李晴瑶/香雪公司/夏侯惇` fragments. |
| `阵问长生` | 1481 | true positive | Valid `墨画` prefix, then unrelated `魔法驱动四轮车/赵灵儿/金陵众/裂魂刃` fragments. |
| `御兽从零分开始` | 1687 | true positive | Valid `乔桑/检引梭梭` prefix, then unrelated `秦烨/朝佚千名/谢尔盖/苏雨蝶` fragments. |
| `长生从炼丹宗师开始` | 1353 | true positive | Valid-looking opening is quickly replaced by unrelated modern/company/other-cultivation fragments such as `费良言/师意/公司/刘恒/乔苑林`。 |
| `夜无疆` | 726 | true positive | Valid `秦铭` prefix, then unrelated court/modern/game/spy fragments. |
| `叩问仙道` | 2784 | true positive | Valid `秦桑/罗络魔君` prefix, then unrelated `枫华集团/商羽/宋简意/侯焱` fragments. |
| `苟在武道世界成圣` | 706 | true positive | Valid `混元无极金身/景阳福地` prefix, then unrelated horror/game/modern fragments. |
| `苟在两界修仙` | 408 | true positive | Valid `方青/金丹客位` prefix, then unrelated `凤王/双蛋瓦斯/萧燕/深海` fragments. |
| `没钱修什么仙？` | 789 | true positive | Chapter title is status-like, but extracted text is a mixed-source body with many unrelated names and settings. |
| `从赘婿开始建立长生家族` | 103 | true positive | Starts with a matching蛊虫 paragraph, then unrelated `动力装甲/公交车/叶梵天/钢铁军团` fragments. |
| `旧域怪诞` | 227 | true positive | Starts with a matching镇中官兵 paragraph, then unrelated school/苏律/苏礼联盟/萧炎 fragments. |
| `异度旅社` | 768 | true positive | Valid `于生/小女孩` prefix, then unrelated `合欢宗/傅慎行/苏天雄/陈七水` fragments. |
| `以神通之名` | 510 | true positive | Valid军武演 prefix, then unrelated `谢风眠/毗沙门/楚欢/林枫` fragments. |

## Confirmed or likely false positives

These were reported as `POLLUTED_RUN` or `POLLUTED_SUFFIX`, but manual reading
does not support deleting them as external novel pollution.

| Book | Chapter | Manual result | Why it is wrong |
| --- | ---: | --- | --- |
| `大道之上` | 309 | false positive / should be `NON_STORY` | It is an author thanks/meta chapter, not mixed story pollution. |
| `苟在初圣魔门当人材` | 1577 | false positive | `番外十一_仙道盛世` stays coherent around `司祟/初圣/祖龙/光海/吕阳`; the reported alien cluster is mostly generic cultivation wording such as `筑基真人` and variants. |
| `吞噬星空2起源大陆` | 453 | false positive | Text remains in the `罗峰/元/星芒/渊象殿主/雨澜城` setting. It is a later/side plot, not wrong-book pollution. |
| `北宋穿越指南` | 596 | likely false positive | Text is coherent historical-politics content with `朱铭/西夏/刘正彦/苗傅/王渊`; it looks like a normal side/political line judged against an insufficient sparse memory. |
| `北宋穿越指南` | 1398 | likely false positive / needs source-title cross-check | Text is coherent late-arc/epilogue-like alt-history academy content around `谢衍/朱棠溪/皇家学会`; it is not mixed-source garbage. Sparse memory did not include this line. |

## Confirmed misses and non-story gaps

These no-suggestion samples are not clean story chapters. They show that the
current replay output cannot yet be treated as production-grade.

| Book | Chapter | Manual result | Evidence summary |
| --- | ---: | --- | --- |
| `仙人消失之后` | 2923 | missed bad extraction / page chrome | The file begins with JavaScript/page code, then正文, then subscription/recommendation/footer text. It was not reported. |
| `仙人消失之后` | 2924 | missed bad extraction / page chrome | Same JavaScript/page-code failure. |
| `这个武圣血条太厚` | 790 | no pollution, but `NON_STORY` | Lottery/Q群 notice, not正文. |
| `剑出衡山` | 260 | no pollution, but `NON_STORY` | New-book promotion, not正文. |
| `太一道果` | 1218 | no pollution, but `NON_STORY` | 完结感言 / new-book promotion. |
| `法力无边高大仙` | 1245 | no pollution, but `NON_STORY` | New-book promotion. |
| `混在末日，独自成仙` | 684 | no pollution, but `NON_STORY` | 完本/休假 note. |

## Root causes visible from this audit

1. Page chrome and recommendation text can enter Book Memory.
   - `仙人消失之后` report shows core features such as `全文免费`, `潇湘书院`,
     `傅总娇妻`, `孽婚门`, etc. These are source chrome/recommendations, not
     novel identity.
   - Once these appear across sampled chapters, they become "trusted" identity
     features and mask bad extraction.

2. Entity candidates are still too broad.
   - Reports still classify generic words as identity features: `平静`, `许久`,
     `全部`, `施展`, `居住`, `安排`.
   - `筑基真人` and its partial variants became an alien cluster in a coherent
     `苟在初圣魔门当人材`番外. This is not a threshold problem; it is candidate
     quality and type gating.

3. Sparse sampling can starve late-arc Book Memory.
   - `吞噬星空2起源大陆` and `北宋穿越指南` show valid late/side arcs being judged
     against prototypes that do not contain enough nearby plot state.
   - The current sampling strategy is efficient, but first-pass memory cannot
     be treated as complete enough for high-confidence delete decisions.

4. The output still conflates story pollution and non-story/source failure.
   - `大道之上` is a meta chapter reported as pollution.
   - Several no-suggestion samples are meta/footer/page-code content that should
     be separately classified or rejected before pollution scoring.

## Current conclusion

The sampled replay is good at catching obvious mixed-source story pollution, but
the current result is not yet production-grade. It has both false positives and
misses on the pulled text.

The next correction should not be another small threshold tweak. The minimum
next design step is a stricter front gate before Book Memory construction:

1. extract and validate正文 before any fingerprint/prototype work;
2. classify `NON_STORY` and `BAD_EXTRACTION` separately from `POLLUTED`;
3. block page chrome/recommendations from both seed memory and target chunks;
4. require entity candidates to pass stronger boundary/type/repetition support
   before they can form alien clusters;
5. treat sparse-sample detections as first-pass suspects unless nearby context
   confirms the target's late-arc state.
