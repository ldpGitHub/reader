# Source Engine UX Test Matrix

Date: 2026-05-30

## Purpose

Verify the user-visible source-engine reader experience after V8 catalog marks
are enabled.

The pass covers:

```text
search relevance
cover selection
detail load
shelf identity
catalog ordering
V8 wrong-chapter marks
reading content
foreground request priority
```

## Acceptance Criteria

- Search: intended book appears first for exact title, common partial title,
  title marks, alias/current-name query, and author query.
- Result list: unrelated or derivative works do not outrank the intended book.
- Cover: first result/detail/shelf use a real cover when a usable cover exists.
- Detail: opens without crash and shows title, author, intro, latest readable
  chapter, and actions.
- Shelf: adding the same canonical book through different source rows creates
  one shelf item.
- Catalog head: first entries are in main-catalog order.
- Catalog middle: non-story rows are marked or hidden according to the catalog
  filter.
- Catalog tail: V8 marks polluted rows. With `显示错章` on, marked rows remain
  visible; with it off, bad rows are filtered from the adapter view.
- Reading: first chapter, middle chapter, and last visible chapter render body
  text without unrelated-book tails.
- Recovery: a saved position inside a filtered bad tail clamps to the nearest
  readable visible chapter without mutating the stored catalog.

## Shelf V8 Verification Set

```text
青山
清光宝鉴
叩问仙道
仙都
元始法则
苟在武道世界成圣
仙人消失之后
灵源仙途：我养的灵兽太懂感恩了
苟在两界修仙
我在修仙界万古长青
```

For each book:

1. Open from shelf.
2. Open catalog and trigger V8 validation.
3. Wait for V8 logs or persisted V8 cache.
4. Pull V8 cache from device.
5. Check schema and content digest.
6. If any wrong mark exists, inspect first wrong chapter and one or two
   neighboring chapters before and after it.
7. If a wrong mark begins before the final tail, inspect later gaps for skipped
   chapters.

## Search Coverage Set

| ID | Query | Expected Top Result | Author |
| --- | --- | --- | --- |
| S01 | 斗破 | 斗破苍穹 | 天蚕土豆 |
| S02 | 诡秘 | 诡秘之主 | 爱潜水的乌贼 |
| S03 | 凡人 | 凡人修仙传 | 忘语 |
| S04 | 剑来 | 剑来 | 烽火戏诸侯 |
| S05 | 叩问仙道 | 叩问仙道 | 雨打青石 |
| S06 | 我在修仙界万古长青 | 我在修仙界万古长青 | 快餐店 |
| S07 | 灵源仙路 | 灵源仙途：我养的灵兽太懂感恩了 | 春雾煮茶 |

## Full Journey Set

Each full journey case runs:

```text
search
open detail
add to shelf
open reader
open catalog
verify catalog head
verify V8 marks near tail
open last visible readable chapter
```

Required cases:

```text
斗破苍穹
诡秘之主
凡人修仙传
剑来
叩问仙道
我在修仙界万古长青
灵源仙途：我养的灵兽太懂感恩了
```

## Runtime Evidence

Device verification must use AI Bridge. Accepted evidence includes:

```text
uia tree showing expected book/catalog state
logcat V8 scheduler/cache events
pulled V8 cache files
manual text snippets around first wrong chapters
absence of foreground chapter loading starvation
```
