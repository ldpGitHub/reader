# 70-Book Batch Audit

Batch source report:

```text
algorithm-test/build/phone-reports/batch-1779454227451-70/
```

This document records manual text inspection. A report is not considered an
algorithm truth case until the actual fetched chapter text has been read.

## Batch Summary

- Targets: 70
- Complete reports: 61
- Failed reports: 9
- Complete serial/ongoing-oriented reports: 41 / 50
- Complete completed/baseline reports: 20 / 20
- OK books with suggestions: 22
- Suggestion rows: 185

## Current Audit Status

### 61-Book Seed Benchmark

All 61 complete phone-side reports have been promoted into the offline seed
benchmark under:

```text
algorithm-test/src/test/resources/batch70-seed-cases/
```

The seed benchmark is intentionally broader than the smaller exact-audited
`batch70-cases` set. It gives the algorithm a fixed 60+ book red/green base to
start from:

- Books: 61
- Real sampled chapter text files: 2926
- `POLLUTED` labels: 197
- `CLEAN` labels: 2645
- `NON_STORY` labels: 84

Generation rule:

```text
mustSuggestIndexes =
  report suggestions
  + manually read true-pollution additions
  - manually read false positives
```

Every other sampled chapter is a seed no-suggest expectation, not a claim that
all 2729 green labels were read line by line. If a future replay suggests one of
those chapters, the text must be inspected before deciding whether the algorithm
misfired or the seed missed a polluted chapter. This keeps the benchmark usable
as a 60+ book starting base without pretending it is a finished gold corpus.

The fixture presence check is green:

```text
.\gradlew.bat :algorithm-test:testDebugUnitTest --offline --no-daemon --tests com.ldp.reader.algorithmtest.core.Batch70SeedReplayTest.fullBatch70SeedFixturesArePresent
```

The full seed replay is currently red with three remaining false positives:

```text
.\gradlew.bat -Dbatch70SeedProbe=true :algorithm-test:testDebugUnitTest --offline --no-daemon --tests com.ldp.reader.algorithmtest.core.Batch70SeedReplayTest.replayFullBatch70SeedCases
```

Current red lights:

| Item | Novel | Fixture Index | Manual Judgment | Algorithm Output |
| --- | --- | ---: | --- | --- |
| item-59 | 长夜余火 / 爱潜水的乌贼 | 960 | Normal same-book content around `商见曜`, `真理`, `蒋白棉`, and the research-area scene. | False `POLLUTED_SUFFIX` at offset 1300. |
| item-63 | 修真四万年 / 卧牛真人 | 5570 | Normal same-book finale material around `洪潮`, `超体文明`, `元始文明`, and `李耀`. | False `POLLUTED_SUFFIX` at offset 1300. |
| item-70 | 斗破苍穹 / 天蚕土豆 | 1693 | Normal same-book content around `纳兰嫣然`, `斗皇`, `地灵门`, and `通灵白狐`. | False `POLLUTED_RUN` from offset 0. |

These three are not label escapes. They remain no-suggest seed cases and should
drive the next algorithm iteration.

### Confirmed True Pollution, Representative Chapters Read

These books have sampled suggestion chapters that clearly switch from the
target novel into unrelated novels. They are true-pollution candidates, but only
the explicitly read chapters below are audited so far.

| Item | Novel | Suggestions | Read Evidence |
| --- | --- | ---: | --- |
| item-22 | 从赘婿开始建立长生家族 / 天弦画柱 | 40 | Read chapter 69, 136, 1114, 1121. The text starts with `陆长生` / `陆妙歌` / `碧湖山`, then switches into unrelated fragments such as `丁雨`, `慕容香香`, `陈浩`, `瓦洛兰大陆`, `段秋`, `宋亮`, `林碧霄`, `卢玄`. |
| item-23 | 旧域怪诞 / 狐尾的笔 | 25 | Read chapter 274, 289, 444, 466. The text starts with `张文达` / `胡毛毛`, then switches into unrelated fragments such as `戚景通`, `加里安`, `何元英`, `米诺托`, `赵原`, `奥妮克希亚`, `张苍穹`, `李世民`. |
| item-17 | 苟在武道世界成圣 / 在水中的纸老虎 | 17 | Read chapter 680, 681, 687, 696, 701. The text starts with `陈庆` / `七大福地` / `大罗天`, then switches into unrelated sports, court, Naruto, historical, and urban snippets. |
| item-18 | 苟在两界修仙 / 文抄公 | 17 | Read chapter 448, 449, 455, 464, 467. The text starts with `方青` / `云州大陆` / `血煞岛`, then switches into unrelated modern, court, fantasy, game, and urban snippets. |
| item-49 | 大不列颠之影 / 趋时 | 17 | Read chapter 185, 35, 36, 55. The text starts with `亚瑟·黑斯廷斯` / `海军部` / `苏格兰场`, then switches into unrelated myth, modern, game-card, fantasy, and xianxia snippets. |
| item-27 | 以神通之名 / 猪心虾仁 | 15 | Read chapter 538, 539, 548, 556. The text starts with `陆昭` / `林知宴` / `武德殿`, then switches into unrelated xianxia, palace, urban, Naruto, game, and military snippets. |
| item-16 | 叩问仙道 / 雨打青石 | 4 | Read all four suggested chapters: 2689, 2692, 2693, 2695. Each begins with `秦桑` / `罗络魔君` context and then switches to unrelated modern, court, school, game, or other xianxia snippets. This item has been promoted into `batch70-cases` as the first audited 70-book fixture. |
| item-06 | 仙工开物 / 蛊真人 | 7 | Read all seven suggested chapters: 583, 584, 585, 586, 587, 588, and 589. Each begins with `宁拙` / `万象宗` / current arc context, then switches into unrelated fragments such as `奥西莉亚`, `安杰希`, corporate tax, funeral/submarine, `苻宝`, school/police, and palace snippets. Neighboring chapters 579-582 were coherent, and the skipped `今天请假` chapter is non-story. |
| item-19 | 没钱修什么仙？ / 熊狼狗 | 8 | Read all seven original suggested chapters: 888, 889, 890, 891, 892, 893, and 894. Each begins with `张羽` / `太月白` / `法流源` / current Kunxu arc context, then switches into unrelated fragments such as `荀宽`, `叶云`, `萧黎`, `慕斯`, `柳七`, `孙立`, zombies, Naruto, and urban snippets. A later replay exposed fixture index 634, title `今天第二章晚一点`; manual reading showed it is also random mixed-source text. Neighboring chapters 885-887 were coherent, and two skipped leave chapters are non-story. |

### Exact Red/Green Fixtures

The following items are now copied into
`algorithm-test/src/test/resources/batch70-cases/`. The replay suite uses the
human-audited expectation, not the current report output.

| Item | Status | Human Expectation |
| --- | --- | --- |
| item-16 | Green | Current algorithm reports exactly the four manually confirmed polluted chapters. |
| item-06 | Green | Current algorithm reports exactly the seven manually confirmed polluted chapters and does not report the checked neighboring/non-story chapters. |
| item-19 | Green | Current algorithm reports exactly the eight manually confirmed polluted chapters and does not report the checked neighboring/non-story chapters. |
| item-50 | Green | Tail chapters 2010-2017 are true pollution; chapter 306 is normal same-book content from the titled `妇人书生` scene and is not reported after the title-absorption fix. |
| item-24 | Green | Chapters 768-775 are true pollution, including the two previously missed chapters 768 and 769. Chapters 766-767 were read as coherent same-book content. |

Replay status:

```text
.\gradlew.bat :algorithm-test:testDebugUnitTest --offline --no-daemon --tests com.ldp.reader.algorithmtest.core.Batch70CaseReplayTest
```

passed for fixture presence.

```text
.\gradlew.bat -Dbatch70CaseProbe=true :algorithm-test:testDebugUnitTest --offline --no-daemon --tests com.ldp.reader.algorithmtest.core.Batch70CaseReplayTest.replayAuditedBatch70Cases
```

previously failed with:

```text
item-50: extra suggested chapters [306]
item-50: missing suggested chapters [2013, 2016]
item-50: chapter 306 must not be suggested
item-24: missing suggested chapters [768, 769]
```

After the V3 candidate-generation and structural evidence changes, the same
command passes. The current replay assertions include the newly read
`item-19` fixture index `634`.

### No-Suggestion Tail Checks

These books currently have no suggestions and sampled tail chapters looked
coherent. This is not yet a full no-leak proof across every fetched chapter.

| Item | Novel | Read Evidence |
| --- | --- | --- |
| item-05 | 神话之后 / 鹅是老五 | Read the last five fetched pages. They continuously follow `丁欢`, `莫无忌`, `蓝小布`, `叶玄`, and the `金色大道之河` event. No obvious mixed-source switch was seen. |
| item-08 | 大道之上 / 宅猪 | Read the last five fetched chapters. They continuously follow `陈实`, `元虫`, `万寿帝君`, `宁真子`, and the black-dark sea arc. No obvious mixed-source switch was seen. |

## 61-Book Seed Replay Status

The 61 complete phone-side reports have now been promoted into a seed replay
benchmark under `algorithm-test/src/test/resources/batch70-seed-cases/`.
The current seed contains 2926 labelled sampled chapters: 197 polluted, 2645
clean, and 84 non-story.

The previous seed replay red lights were three false positives:

| Item | Novel | Human Read Result |
| --- | --- | --- |
| item-59 | `长夜余火 / 爱潜水的乌贼` | Index `960` is same-book content. The tail is `蒋白棉` reading brain-area notes in the ordinary research area, not mixed-source fiction. |
| item-63 | `修真四万年 / 卧牛真人` | Index `5570` is same-book finale material around `李耀`, `洪潮`, `超体文明`, and `元始文明`. |
| item-70 | `斗破苍穹 / 天蚕土豆` | Index `1693` is same-world continuation around `纳兰嫣然`, `斗皇`, `地灵门`, and `通灵白狐`. |

The non-model V3/V4 decision-layer fix keeps candidate generation broad but
narrows final pollution acceptance to independent structural evidence. Same-book
concept drift and explanatory/reference material can now reduce confidence, but
they do not remove true pollution unless the segment also has strong same-book
world consistency and weak external identity evidence.

Replay status after the fix:

```text
.\gradlew.bat -Dbatch70SeedProbe=true :algorithm-test:testDebugUnitTest --offline --no-daemon --tests com.ldp.reader.algorithmtest.core.Batch70SeedReplayTest.replayFullBatch70SeedCases
```

passed in 5m04s. The JUnit XML reported `tests=1 skipped=0 failures=0
errors=0`. Spot-check reports for `item-59`, `item-63`, and `item-70` all show
`Suggestions: 0`.

The broader module check also passed:

```text
.\gradlew.bat :algorithm-test:testDebugUnitTest --offline --no-daemon
```

## Working Conclusions

- The highest-suggestion books in this 70-book run are not random threshold
  failures in the inspected chapters. They show real multi-source text
  pollution.
- Some top fingerprint features still include generic-looking terms such as
  `连忙`, `通常`, or fragments like `文达跟`. They did not invalidate the
  inspected positive detections, but they remain a cleanup target for the
  feature-promotion layer.
- The 61-book seed replay is now a green regression suite for the current
  labelled sample set. It is still a seed benchmark: the 197 polluted labels
  were manually text-checked, while many no-suggest labels are inherited from
  the complete reports plus targeted manual false-positive reads.
- The next algorithm work should continue with the same model-free Novel State
  Memory direction before any device model is introduced: chunk data, book
  prototypes, graph absorption, world/style profiles, OOD/energy, and sequence
  boundary evidence.

## Next Audit Queue

1. Expand manual clean-label audit coverage for no-suggestion tail chapters.
2. Re-run the 6 OOM serial books with lower batch parallelism or single-book
   mode after memory pressure is addressed.
