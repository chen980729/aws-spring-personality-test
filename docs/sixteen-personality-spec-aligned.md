# Four-Dimension Personality Assessment Specification

> **File:** `docs/domain/sixteen-personality-spec.md`  
> **Status:** MVP Baseline / Accepted  
> **Last updated:** 2026-09-19  
> **Scope:** Personality assessment domain rules, scoring, AI clarification, finalization, result composition, and content/legal boundaries.

---

## 1. Purpose

本项目提供一个基于四个 personality dimensions 的人格偏好测试，并最终生成类似 `INFJ`、`ENTP` 的四字母 Personality Type。

本功能在当前 Portfolio Project 中的首要目标不是构建专业心理测量产品，而是提供一个足够完整、可解释、可测试的业务场景，用于展示：

- React questionnaire flow
- Spring Boot domain modeling
- deterministic scoring
- ambiguity handling
- AI-assisted clarification
- structured AI output
- persistence and audit trail
- result visualization
- automated testing
- API contract design
- versioning
- AWS deployment / observability integration

因此，MVP 优先保证：

1. 业务规则明确；
2. Scoring 可重复、可测试；
3. AI 与 deterministic business logic 的职责边界清晰；
4. 原始回答、初始结果、AI clarification、最终结果均可追踪；
5. 实现复杂度与 Portfolio 目标相匹配。

**Professional psychometric validation 不属于 MVP 目标。**

---

## 2. Product Positioning

本项目实现的是一个：

> **Original four-dimension personality preference assessment**

它不是官方 MBTI® assessment，也不是 16Personalities / NERIS Type Explorer® 的复刻。

当前文件名 `sixteen-personality-spec.md` 只是项目内部既有命名。产品 UI、README marketing copy 和 API-facing product name 不应将本测试称为：

- `MBTI Test`
- `Free MBTI`
- `AI MBTI`
- `16Personalities`
- `Sixteen Personality Test`

推荐使用独立工作名称，例如：

- `Personality Type Explorer`
- `Four-Dimension Personality Explorer`

最终品牌名称可以在 UI polish 阶段另行决定。

---

## 3. Assessment Model

测试包含四个 dimensions：

| Dimension | Pole A | Pole B |
|---|---|---|
| `EI` | Extraversion (`E`) | Introversion (`I`) |
| `SN` | Sensing (`S`) | Intuition (`N`) |
| `TF` | Thinking (`T`) | Feeling (`F`) |
| `JP` | Judging (`J`) | Perceiving (`P`) |

统一规定 **Pole A 为 positive direction**：

```text
EI: E = positive, I = negative
SN: S = positive, N = negative
TF: T = positive, F = negative
JP: J = positive, P = negative
```

四个 finalized dimensions 按固定顺序组合：

```text
EI + SN + TF + JP
```

例如：

```text
I + N + F + J = INFJ
```

最终 Personality Type 必须由四个 finalized dimensions deterministic 地生成。

**AI 不直接生成或重写最终四字母 type。**

---

## 4. Questionnaire Structure

### 4.1 Number of Questions

MVP 固定为：

```text
48 questions
```

每个 dimension：

```text
12 questions
```

因此：

```text
4 dimensions × 12 questions = 48 questions
```

这在测试长度与 dimension-level signal 之间提供了足够合理的工程平衡。

### 4.2 Question Distribution

四个 dimension 的题目交错出现，而不是连续完成同一 dimension。

当前 baseline 顺序：

```text
Q1  → EI
Q2  → SN
Q3  → TF
Q4  → JP
Q5  → EI
...
```

因此：

```text
Q1 belongs to EI
```

MVP 不要求 dynamic item selection。

题目 randomization、larger item pool 等能力属于 Future Improvement。

---

## 5. Answer Scale

采用：

```text
5-point Likert Scale
```

用户语义：

| Stored Value | UI Meaning |
|---:|---|
| `1` | Strongly Disagree |
| `2` | Disagree |
| `3` | Neither Agree nor Disagree |
| `4` | Agree |
| `5` | Strongly Agree |

Frontend 不应只显示裸数字；数字必须具有明确的语义标签。

Backend 保存 canonical integer：

```text
1 / 2 / 3 / 4 / 5
```

---

## 6. Balanced Keying

每个 dimension 的 12 道题采用：

```text
6 questions keyed toward Pole A
6 questions keyed toward Pole B
```

例如 `EI`：

```text
6 E-keyed questions
6 I-keyed questions
```

MVP 尽量避免使用复杂否定结构来实现传统意义上的 reverse wording，例如：

```text
I do not dislike ...
```

这类 wording 容易增加理解成本。

Domain model 应表达题目的真实语义：

```text
dimension
keyedPole
```

例如：

```text
dimension = EI
keyedPole = I
```

而不是只保存：

```text
reverseScored = true
```

`keyedPole` 是更稳定、更可读的 domain representation。

---

## 7. Scoring

### 7.1 Centered Item Score

原始回答：

```text
1  2  3  4  5
```

转换为：

```text
-2 -1 0 +1 +2
```

基础公式：

```text
centeredScore = answer - 3
```

### 7.2 Keyed Direction

如果题目 keyed toward Pole A：

```text
itemScore = answer - 3
```

如果题目 keyed toward Pole B：

```text
itemScore = -(answer - 3)
```

### 7.3 Dimension Raw Score

每个 dimension 有 12 道题，每题：

```text
-2 ... +2
```

因此：

```text
rawScore ∈ [-24, +24]
```

判定方向：

```text
rawScore > 0 → Pole A
rawScore < 0 → Pole B
rawScore = 0 → exact tie
```

例如 `EI`：

```text
rawScore = +8 → E
rawScore = -8 → I
```

### 7.4 Reference Pseudocode

```text
scoreDimension(dimensionAnswers):
    rawScore = 0

    for each answer:
        centered = answer.value - 3

        if answer.question.keyedPole == dimension.poleA:
            rawScore += centered
        else:
            rawScore -= centered

    return rawScore
```

Scoring 必须是 deterministic application/domain logic，不调用 AI。

---

## 8. Preference Percentage

Percentage 主要用于结果页展示，不作为核心 business rule 的 source of truth。

Pole A display percentage：

```text
poleAPercentage = 50 + (rawScore / 24) * 50
```

Pole B：

```text
poleBPercentage = 100 - poleAPercentage
```

示例：

| Raw Score | Approx. Split |
|---:|---|
| `0` | `50.00 / 50.00` |
| `+1` | `52.08 / 47.92` |
| `+2` | `54.17 / 45.83` |
| `+3` | `56.25 / 43.75` |
| `-2` | `45.83 / 54.17` |

Frontend 可以进行合理 rounding。

**Backend ambiguity / finalization logic 必须使用 raw score，而不是 rounded percentage。**

---

## 9. Ambiguity Policy

产品概念上使用约：

```text
45 / 55 ambiguity band
```

实际 deterministic rule：

```text
ambiguous = abs(rawScore) <= 2
```

因此：

```text
50 / 50
52 / 48
54 / 46
```

均属于 ambiguous。

约：

```text
56 / 44
```

开始属于 non-ambiguous。

该 threshold 是本项目的 **product rule**，不是经过专业心理测量 validation 后得出的科学阈值。

---

## 10. Result Stages

为与项目的 Assessment Domain Model 保持一致，本文件统一使用以下 Ubiquitous Language：

```text
InitialAssessmentResult
ClarificationResult
FinalAssessmentResult
```

其中：

```text
InitialAssessmentResult
└── InitialDimensionResult[]

FinalAssessmentResult
└── FinalDimensionConclusion[]
```

不能只保存一个可被后续流程覆盖的通用 `result`。

### 10.1 InitialAssessmentResult

由 deterministic questionnaire scoring 产生，并作为 AI clarification 之前的不可变 baseline evidence。

例如某个 `InitialDimensionResult`：

```text
dimension = EI
rawScore = -2
questionnairePreference = I
ambiguous = true
```

`InitialAssessmentResult` 一旦产生即 immutable。AI clarification 不得修改或覆盖它。

### 10.2 ClarificationResult

只在 ambiguous dimension 上、当 clarification 真正完成并被业务接受时产生。

例如：

```text
dimension = EI
suggestedPole = E
resolution = RESOLVED
confidence = HIGH
```

`confidence` 可以保存用于 observability / future policy evolution，但 MVP 不要求根据 confidence 再增加一套复杂 threshold。

**Intermediate AI turn output 不等于 `ClarificationResult`。**

多轮 clarification 可以经历若干中间交互；只有当当前 dimension 的 clarification 最终得到可接受业务结论时，才形成并持久化 `ClarificationResult`。

### 10.3 FinalAssessmentResult

由 deterministic `FinalizationPolicy` 基于：

```text
InitialAssessmentResult
+
ClarificationResult / explicit skip / user tie-break facts
```

生成。

每个 dimension 最终形成 `FinalDimensionConclusion`，四个 finalized dimensions 再 deterministic 地组成最终 Personality Type。

AI 不直接持久化最终 Personality Type。

---

## 11. AI Clarification

### 11.1 Trigger

只有：

```text
ambiguous == true
```

的 dimension 才允许进入 AI clarification。

例如：

```text
EI = 54% I → clarification eligible
EI = 67% I → no clarification
```

### 11.2 AI Business Output

AI 的职责：

```text
ambiguous InitialDimensionResult
        ↓
contextual clarification interaction
        ↓
structured business output
```

当一次 clarification 已经形成候选业务结论时，structured output 至少包含：

```text
dimension
resolution
suggestedPole?
confidence
```

其中：

```text
resolution = RESOLVED | UNCLEAR

RESOLVED -> suggestedPole must be non-null
UNCLEAR  -> suggestedPole must be null
```

可选业务内容：

```text
reasoningSummary
evidence
```

`reasoningSummary` 应是面向 audit / product explanation 的简短摘要，不应依赖或保存模型私有 chain-of-thought。

AI structured output **不负责报告技术执行状态**。例如 timeout、provider error、malformed response 等不应由模型返回：

```text
FAILED
```

这些属于 Backend / AI Integration 的 technical execution outcome。

### 11.3 DimensionClarification Lifecycle vs AI Output

`DimensionClarification` Entity 的 lifecycle 与 AI structured output 是两个不同概念。

Domain lifecycle：

```text
PENDING
IN_PROGRESS
CLARIFIED
SKIPPED
FAILED_RETRYABLE
```

典型映射：

```text
AI resolution = RESOLVED
→ accepted ClarificationResult exists
→ DimensionClarification = CLARIFIED

AI resolution = UNCLEAR
→ accepted ClarificationResult exists with `suggestedPole = null`
→ DimensionClarification = CLARIFIED
→ FinalizationPolicy decides fallback / tie-break path

technical provider failure
→ no accepted ClarificationResult
→ DimensionClarification = FAILED_RETRYABLE
```

技术失败不能被伪装成一个正常的 `UNCLEAR` 业务结论。

### 11.4 AI Provenance

模型标识、prompt / clarification policy revision 等 provenance 不由 LLM 自己“声明”。

Backend / AI Integration 必须从实际执行上下文记录：

```text
AIProvenance
├── modelIdentifier
└── clarificationPolicyRevision
```

MVP `AIProvenance` 表示：

> **最终产生并被业务接受的 `ClarificationResult` 的有效 AI execution provenance。**

它不是：

```text
all LLM calls
all retry attempts
all models used in every intermediate turn
raw provider request / response history
```

如果未来需要完整 AI execution audit，再考虑引入 `ClarificationAttempt[]`；MVP 不建立该模型。

### 11.5 Conversation Runtime State

完整 AI conversation transcript 不是 authoritative Assessment history。

Active clarification 可以依赖 temporary / ephemeral runtime context，以支持多轮交互，但 MVP 不保证：

```text
browser/server restart
→ resume from exact previous AI message
```

如果 temporary context 丢失：

- 已持久化的 `AssessmentSession` 不得损坏；
- `InitialAssessmentResult` 不得变化；
- 当前 dimension 可以重新开始 clarification；
- 最终仅长期保存 accepted `ClarificationResult`、必要的 `AIProvenance` 和可选 summary。

### 11.6 AI Boundary

AI 不允许直接输出并决定：

```text
finalType = INFJ
```

也不允许直接修改：

```text
InitialAssessmentResult
```

正确的职责链是：

```text
Questionnaire Scoring
        ↓
InitialAssessmentResult
        ↓
Ambiguity Evaluation
        ↓
DimensionClarification
        ↓
FinalizationPolicy
        ↓
FinalDimensionConclusion
        ↓
PersonalityTypeComposer
```

---

## 12. FinalizationPolicy

`FinalizationPolicy` 是 deterministic business logic。

它消费已经持久化或已被业务接受的 facts，而不是重新让 AI 决定最终类型。

### 12.1 Case A — Non-Ambiguous

```text
Questionnaire = 67% I
```

结果：

```text
Final = I
source = QUESTIONNAIRE
```

AI 不参与，也无权 override。

### 12.2 Case B — Ambiguous + Clarification Agrees

```text
Questionnaire = 54% I
Clarification = I
```

结果：

```text
Final = I
source = QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION
```

### 12.3 Case C — Ambiguous + Clarification Flips

```text
Questionnaire = 54% I
Clarification = E
```

允许：

```text
Final = E
source = AI_CLARIFICATION
overrodeBaseline = true
```

但必须同时保留：

```text
questionnairePreference = I
finalPreference = E
```

AI clarification 不能覆盖原始 `InitialDimensionResult`。

### 12.4 Case D — Clarification Unclear, Non-Zero Baseline

```text
Questionnaire = 52% E
Clarification = UNCLEAR
```

如果 questionnaire baseline 不是 exact tie，则允许 deterministic fallback：

```text
Final = E
source = QUESTIONNAIRE_FALLBACK
```

这表示 AI 没有提供足够证据改变 baseline，而不是 AI “决定了 E”。

### 12.5 Case E — Technical AI Failure

如果 AI provider timeout、调用失败、structured output invalid 等技术问题发生：

```text
DimensionClarification
→ FAILED_RETRYABLE
```

此时 **不得直接完成 Finalization**。

用户可以：

```text
Retry Current Clarification
```

或显式：

```text
Skip Current / Remaining Clarification
```

只有用户选择 Skip 后，`FinalizationPolicy` 才根据 questionnaire baseline 或 exact-tie rule 继续。

因此：

```text
technical failure
≠
UNCLEAR business result
```

### 12.6 Case F — Clarification Skipped / AI Declined

如果用户跳过某个 ambiguous dimension，或选择 `Skip Remaining Clarifications / Decline AI Clarification`：

#### Non-zero questionnaire baseline

```text
Questionnaire = 52% E
Clarification = SKIPPED
```

结果：

```text
Final = E
source = QUESTIONNAIRE_FALLBACK
```

#### Exact tie

```text
Questionnaire = 50 / 50
Clarification = SKIPPED
```

不能使用固定 pole，也不能伪造 questionnaire preference。

必须进入：

```text
USER_TIE_BREAK_REQUIRED
```

### 12.7 Case G — Exact Tie + Clarification Unclear / Skipped

```text
Questionnaire = 50 / 50
Clarification = UNCLEAR | SKIPPED
```

系统不得偷偷使用固定 tie-break，例如：

```text
E always wins
```

应要求用户完成一次明确的 final preference selection：

```text
Final = user selected pole
source = USER_TIE_BREAK
```

User tie-break 是必须持久表达的业务事实，因为它直接参与 `FinalDimensionConclusion` 的形成。

---

## 13. Final Type Composition

只有四个 `FinalDimensionConclusion` 都已经形成后，才能生成最终 Personality Type。

例如：

```text
EI = I
SN = N
TF = F
JP = J
```

得到：

```text
FinalType = INFJ
```

`PersonalityTypeComposer` 只负责组合：

```text
compose(EI, SN, TF, JP)
```

它不调用 AI，也不重新解释原始回答。

不允许出现：

```text
initial questionnaire outcome = ISTJ
AI says "sounds more like INTJ"
final = INTJ
```

四个 finalized dimensions 是 final type 的唯一 source of truth。

---

## 14. Result Presentation

结果页至少包含三层信息。

### 14.1 Final Type

例如：

```text
INFJ
```

### 14.2 Dimension Evidence and Final Conclusion

Questionnaire percentage 始终表示：

> **Questionnaire evidence derived from deterministic raw score.**

它不是 AI 调整后的“新百分比”。

例如：

```text
Questionnaire evidence:
Introversion 54%
Extraversion 46%

Final preference:
E
Decision source:
AI_CLARIFICATION
```

如果 AI clarification 翻转了 baseline，Frontend 必须把：

```text
questionnaire evidence
```

和：

```text
final categorical conclusion
```

明确区分。

禁止生成：

```text
AI-adjusted percentage
```

对于边界附近的 questionnaire evidence，文案应使用中性的 preference language，例如：

```text
slight preference
```

而不是过度确定的人格判断。

### 14.3 Type Description

描述文案推荐使用：

```text
You may tend to...
You are likely to prefer...
In some situations...
```

避免：

```text
You always...
People of this type are...
You are naturally...
```

产品定位是 self-reflection / personality preference exploration，不是心理诊断。

建议结果页包含类似 disclaimer：

> This result describes preference patterns reflected in your answers. It is intended for self-reflection and is not a clinical or professional psychological assessment.

---

## 15. Assessment Versioning

人格测试未来可能调整：

- question wording
- question set
- keyed pole
- ambiguity threshold
- scoring rules
- clarification policy / prompt
- finalization rules

历史测试结果必须绑定具体的 `AssessmentDefinitionVersion`。

### 15.1 Authoritative Version Snapshot

MVP 的 authoritative specification identity 是：

```text
AssessmentDefinitionVersion = "1.0"
```

它是完整 specification snapshot 的 source of truth，并包含：

```text
QuestionnaireDefinition
DimensionDefinition[]
ScoringPolicy
AmbiguityPolicy
FinalizationPolicy
expected/default ClarificationPolicy revision
```

`ScoringPolicy`、`AmbiguityPolicy`、`FinalizationPolicy` 在 Domain Model 中是 DefinitionVersion 内部的 VO，不建立独立的业务 version lifecycle。

内部 revision 只作为 snapshot metadata，不能成为与 `AssessmentDefinitionVersion` 竞争的独立 source of truth。

### 15.2 Clarification Policy Provenance

由于 AI clarification 的实际执行可能受 runtime provider / policy 变化影响：

- `AssessmentDefinitionVersion` 可以绑定 expected/default `ClarificationPolicy` revision；
- 每个实际完成的 `DimensionClarification` 必须记录实际使用的 `AIProvenance`；
- 历史解释以 DefinitionVersion specification + actual clarification provenance 共同完成。

### 15.3 Immutability and Traceability

一旦某个 `AssessmentDefinitionVersion` 对用户 executable / available：

> **其 specification 不得原地修改。**

影响业务语义的变化必须创建新的 `AssessmentDefinitionVersion`。

历史 Assessment 需要具备：

> **traceability and explainability, with reproducibility for deterministic assessment components.**

LLM clarification 不保证在未来重新执行时得到完全相同的输出，因此不宣称 strict end-to-end reproducibility。

---

## 16. Persistence / Audit Requirements

为了提供足够的 assessment traceability，至少应能够追踪：

```text
AssessmentSession
  ├─ AssessmentDefinitionVersion
  ├─ QuestionnaireResponse / answers
  ├─ InitialAssessmentResult
  │    └─ InitialDimensionResult[]
  ├─ DimensionClarification[]
  │    ├─ ClarificationResult (when accepted)
  │    └─ AIProvenance
  ├─ User tie-break fact (when required)
  └─ FinalAssessmentResult
       └─ FinalDimensionConclusion[]
```

关键要求：

1. 在 retained Assessment record 的生命周期内，submitted answers 必须保留且不能被后续 AI 或新版本规则覆盖；
2. `InitialAssessmentResult` 必须保留，并保持 immutable；
3. AI clarification 不覆盖 baseline evidence；
4. accepted `ClarificationResult` 与必要 `AIProvenance` 必须保存；
5. technical retry history / raw provider request-response / complete conversation transcript 不属于 MVP core domain persistence requirement；
6. `FinalAssessmentResult` 必须记录每个 dimension 的 decision source；
7. exact tie 由用户解决时，user-selected pole / `USER_TIE_BREAK` source 必须持久表达；
8. 历史 assessment 结果不能因为新版本规则被自动重算覆盖；
9. 删除历史 Assessment 时，是否 hard delete / soft delete 由统一 deletion policy 决定；immutability 约束修改，不意味着永远不可删除。

具体 DB schema 在独立 database/backend detailed design 中定义，本文件只定义 domain requirement。

---

## 17. Question Content Policy

MVP 使用：

```text
Original Questions Only
```

不直接复制：

- official MBTI instrument items
- 16Personalities questions
- commercial personality assessment question banks
- 未明确允许复用的第三方测试题目

也不通过简单翻译已有题目来当作原创题。

可以参考公开的人格理论和 questionnaire design methodology，但最终 wording 应由本项目自行设计。

---

## 18. Naming / Copyright / Trademark Boundary

### 18.1 MBTI

`Myers-Briggs Type Indicator`、`Myers-Briggs`、`MBTI` 等名称属于相关权利人的 trademarks / registered trademarks。

官方 Myers & Briggs Foundation 说明，MBTI instrument 不能在未经书面许可的情况下复制，其 instrument items 的 reprint / modification / adaptation / translation 需要许可。

因此本项目：

- 不宣称自己是官方 MBTI assessment；
- 不使用 `MBTI Test` 等名称推广本产品；
- 不复制官方 instrument items；
- 如在 documentation 中提及 MBTI，仅用于背景说明和技术/产品边界说明。

Official reference:

- https://www.myersbriggs.org/using-type-as-a-professional/mbti-permission-trademarks/

### 18.2 16Personalities

16Personalities 的公开资料将其体系描述为 `16Personalities / NERIS Type Explorer® framework`，相关网站内容和产品材料受其 terms / intellectual-property rules 约束。

因此本项目：

- 不复制 16Personalities question wording；
- 不复制其 type profile 文案或 nickname；
- 不将本项目包装为 16Personalities 产品或兼容版本。

Official references:

- https://www.16personalities.com/for-ai
- https://www.16personalities.com/terms

### 18.3 General Copyright Principle

Copyright 一般保护具体 expression，而不是 idea、procedure、method、system 本身。

这意味着我们可以独立实现自己的四维 scoring / assessment approach，但不应因此复制第三方受保护的具体题目或说明文案。

Reference:

- https://www.copyright.gov/what-is-copyright/
- https://www.copyright.gov/help/faq/faq-protect.html

> 本节用于定义项目实现边界，不构成法律意见。若未来将项目商业化，应进行单独的 trademark / licensing review。

---

## 19. MVP Scope

### In Scope

- 48 original questions
- 12 questions per dimension
- 5-point Likert scale
- balanced keying
- deterministic scoring
- raw-score-based ambiguity detection
- AI clarification for ambiguous dimensions
- deterministic FinalizationPolicy
- final type composition
- result page
- result history
- assessment versioning
- automated tests for scoring / finalization

### Out of Scope

以下不属于 MVP：

- formal psychometric validation
- large-scale norming
- Cronbach's alpha optimization
- exploratory / confirmatory factor analysis
- Item Response Theory (IRT)
- demographic norm calibration
- clinical interpretation
- hiring / employment assessment
- professional psychological diagnosis
- sophisticated Anti-Gaming system
- adaptive item selection

---

## 20. Future Improvement — Assessment Quality

未来有额外时间时，可以逐题 review construct validity，重点检查：

- EI item 是否实际测到 sociability / social anxiety；
- SN item 是否混入 openness；
- TF item 是否主要测 empathy / agreeableness；
- JP item 是否混入 conscientiousness；
- 是否存在明显 social desirability bias；
- 是否存在 wording bias；
- 是否存在 cultural bias；
- 同一 dimension 的 items 是否过度重复。

如果有足够真实、匿名且合规的数据，还可以进一步研究 reliability / factor structure。

---

## 21. Future Improvement — Anti-Gaming

未来的重要目标之一：

> 即使用户熟悉传统 personality test，也不应该能够非常容易地通过“选择看起来像目标人格的答案”指定最终类型。

例如用户希望故意得到 `INFJ` 时，不应让每一道题都明显暴露：

```text
this answer → I
this answer → N
this answer → F
this answer → J
```

### 21.1 Design Direction

未来可以探索：

#### Indirect Items

避免直接询问人格标签明显对应的行为，改为具体情境中的 preference。

#### Contextual Questions

在不同 context 中测量同一个 construct，例如：

- work
- learning
- social interaction
- decision making
- planning
- stress

#### Cross-Checking

使用不同 wording / context 对同一 underlying preference 进行交叉验证。

如果用户为了目标 type 刻意作答但相关回答产生明显 contradiction，可以降低该 dimension 的 confidence。

#### Larger Item Pool

从：

```text
48 fixed questions
```

演进到：

```text
80–120 candidate items
        ↓
select 48 per assessment
```

这样既降低固定答案模板的价值，也为后续 item analysis 提供空间。

#### Question Randomization

未来可以随机：

```text
question order
presentation direction
```

但必须保持 stable question ID 和 scoring definition。

#### Contextual AI Clarification

AI clarification 不直接问：

```text
Are you more introverted or extroverted?
```

而使用具体场景追问，从而降低目标 pole 的明显程度。

### 21.2 Anti-Gaming Principle

目标是：

```text
Question meaning
    = clear to the user

Scoring implication
    ≠ obvious to the user
```

而不是：

```text
Question meaning
    = intentionally confusing
```

Anti-Gaming 不应以降低可理解性、制造双重否定或欺骗用户为代价。

---

## 22. Engineering Value of Future Anti-Gaming

Anti-Gaming 也可以成为后续 Portfolio 的技术扩展点：

```text
Question Bank
        ↓
Assessment Version
        ↓
Question Selection Strategy
        ↓
Response Analysis
        ↓
Consistency / Confidence
        ↓
AI Clarification
        ↓
Deterministic Finalization
        ↓
Final Type
```

可能引入的 domain concepts：

- `QuestionBank`
- `QuestionSelectionStrategy`
- `ConsistencyScore`
- `DimensionConfidence`
- `AssessmentVersion`
- `ClarificationTrigger`

这些均为 Future Enhancement，不影响当前 MVP。

---

## 23. MVP Acceptance Criteria

### Questionnaire

- [ ] 一个完整 assessment 恰好包含 48 道题。
- [ ] EI / SN / TF / JP 各 12 道题。
- [ ] 每维包含 6 个 Pole A keyed items 和 6 个 Pole B keyed items。
- [ ] 每题只接受 `1..5`。
- [ ] 未完成全部必答题前不能完成 questionnaire。

### Scoring

- [ ] 每题转换后的 score 只能为 `-2..+2`。
- [ ] 每维 raw score 只能位于 `-24..+24`。
- [ ] 相同 answers 必须始终得到相同 `InitialAssessmentResult`。
- [ ] `abs(rawScore) <= 2` 时 `ambiguous = true`。
- [ ] percentage rounding 不影响 ambiguity decision。

### AI Clarification

- [ ] Non-ambiguous dimension 不允许进入 clarification。
- [ ] AI structured business output 不直接包含 technical failure state。
- [ ] `RESOLVED` output 可以形成 accepted `ClarificationResult`。
- [ ] `UNCLEAR` 形成 accepted `ClarificationResult`，但 `suggestedPole = null`，不得伪装成 resolved pole。
- [ ] Technical AI failure 进入 `FAILED_RETRYABLE`，不会直接 finalize。
- [ ] AI 不能覆盖 `InitialAssessmentResult`。
- [ ] `AIProvenance` 由 Backend / AI Integration 从实际执行上下文记录。
- [ ] Intermediate AI turn output 不等于最终 `ClarificationResult`。
- [ ] MVP 不要求持久化完整 AI conversation transcript。

### Finalization

- [ ] Non-ambiguous dimension 使用 questionnaire baseline。
- [ ] Ambiguous dimension 可以被 accepted clarification 确认或翻转。
- [ ] Flip 时 baseline 与 final preference 同时保留。
- [ ] `UNCLEAR` + non-zero questionnaire baseline 使用 deterministic questionnaire fallback。
- [ ] Technical AI failure 不自动转换为 questionnaire fallback；用户需 Retry 或显式 Skip。
- [ ] Skip / Decline AI + non-zero baseline 使用 questionnaire fallback。
- [ ] Exact tie + `UNCLEAR` / `SKIPPED` 进入 explicit user tie-break。
- [ ] User tie-break result 被持久保存，并记录 `source = USER_TIE_BREAK`。
- [ ] Final type 只能由四个 `FinalDimensionConclusion` 组合。

### Result Presentation

- [ ] Questionnaire percentage 始终来自 deterministic raw score。
- [ ] AI clarification 不生成或修改 percentage。
- [ ] 当 final preference 与 questionnaire baseline 不同时，UI 明确区分 questionnaire evidence 与 final categorical conclusion。

### Versioning / History

- [ ] 每次 AssessmentSession 绑定具体 `AssessmentDefinitionVersion`。
- [ ] executable / available DefinitionVersion 不允许原地修改。
- [ ] 历史 answers、InitialResult、ClarificationResult、FinalResult 不被未来版本覆盖。
- [ ] deterministic assessment components 可以基于 retained specification 重现；LLM clarification 只要求 traceable / explainable，不要求 strict reproducibility。
- [ ] 用户可以查看历史完成的测试结果。
- [ ] 删除测试历史时，按项目统一 delete policy 处理该 Assessment 及其 owned child records。

---

# Appendix A — MVP Question Bank v1.0

> 当前题库用于实现 MVP。它满足工程需求，但没有经过专业 psychometric validation。  
> 后续允许通过新的 `assessmentVersion` 修改 wording 或替换 items。

| # | Dimension | Keyed Pole | Statement |
|---:|---|---|---|
| 1 | EI | E | I often understand my thoughts better by discussing them with other people. |
| 2 | SN | S | When learning something new, I prefer concrete examples before abstract explanations. |
| 3 | TF | T | When making a difficult decision, I first look for the most logically consistent option. |
| 4 | JP | J | I feel more comfortable when important plans are settled in advance. |
| 5 | EI | I | After spending a lot of time with people, I usually want some time alone to recharge. |
| 6 | SN | N | I enjoy looking for patterns and meanings that are not immediately obvious. |
| 7 | TF | F | I naturally consider how a decision will affect the people involved. |
| 8 | JP | P | I like leaving room to change my plans when new possibilities appear. |
| 9 | EI | E | Talking through an idea often helps me develop it. |
| 10 | SN | S | Clear instructions and specific examples help me feel confident about a task. |
| 11 | TF | T | In a disagreement, I tend to examine whether each argument is internally consistent. |
| 12 | JP | J | I usually prefer finishing important tasks before turning to less urgent ones. |
| 13 | EI | I | In a new group, I usually take some time to observe before becoming actively involved. |
| 14 | SN | N | I often find myself thinking about how things could develop in the future. |
| 15 | TF | F | I adjust the way I communicate when I know someone may be emotionally affected. |
| 16 | JP | P | I enjoy keeping several options open until a decision is really necessary. |
| 17 | EI | E | I enjoy being actively involved in conversations with several people. |
| 18 | SN | S | When solving a problem, I usually start with the facts that are directly available. |
| 19 | TF | T | I prefer feedback to be clear and precise, even when it may be uncomfortable to hear. |
| 20 | JP | J | Before starting a complicated project, I like to decide what the next steps will be. |
| 21 | EI | I | I usually prefer a few substantial conversations to many brief interactions. |
| 22 | SN | N | Abstract ideas can interest me even before I know whether they have a practical use. |
| 23 | TF | F | Preserving trust between people can matter as much to me as finding the most efficient solution. |
| 24 | JP | P | I adapt easily when a schedule changes unexpectedly. |
| 25 | EI | E | When I have free time, I often look for activities I can share with other people. |
| 26 | SN | S | I tend to notice practical details that affect whether an idea will actually work. |
| 27 | TF | T | I can usually separate my personal feelings from my evaluation of an argument. |
| 28 | JP | J | Having a clear endpoint for a task helps me work more comfortably. |
| 29 | EI | I | I often need private thinking time before I know what I really think about something. |
| 30 | SN | N | I enjoy making connections between ideas from very different subjects. |
| 31 | TF | F | When resolving a disagreement, maintaining the relationship is an important part of the solution for me. |
| 32 | JP | P | I enjoy discovering the direction of a project while I am working on it. |
| 33 | EI | E | In group discussions, contributing my thoughts aloud feels natural to me. |
| 34 | SN | S | When a familiar method works well, I usually see value in using it again. |
| 35 | TF | T | When several choices seem reasonable, comparing their advantages and disadvantages helps me decide. |
| 36 | JP | J | Trips or events feel easier to enjoy when the important details have been arranged beforehand. |
| 37 | EI | I | Even enjoyable social activities can leave me wanting a period of quiet afterward. |
| 38 | SN | N | I am often interested in trying a new approach simply because it might reveal a better possibility. |
| 39 | TF | F | When judging a decision, I pay close attention to whether the people involved are being treated fairly. |
| 40 | JP | P | I prefer adjusting my approach as I go rather than following a detailed plan from beginning to end. |
| 41 | EI | E | Sharing an experience with someone often helps me make sense of it. |
| 42 | SN | S | I usually want to understand how an idea works in practice before relying on it. |
| 43 | TF | T | When a problem becomes stressful, my first instinct is usually to identify what can be fixed. |
| 44 | JP | J | Unresolved important decisions tend to stay on my mind until I settle them. |
| 45 | EI | I | I am comfortable spending long stretches of time working independently. |
| 46 | SN | N | Possibilities and underlying concepts often capture my attention more than concrete details do. |
| 47 | TF | F | When someone tells me about a problem, my first instinct is often to understand how they feel about it. |
| 48 | JP | P | I feel most comfortable when plans leave some room for spontaneity. |

---

# Appendix B — Final MVP Decisions

1. 使用 48 道原创题。
2. 每个 dimension 12 道题。
3. 使用 5-point Likert scale。
4. Q1 属于 EI；四个 dimensions 交错出现。
5. 每维采用 6 + 6 balanced keying。
6. Backend 使用 centered raw scoring。
7. 每维 raw score 范围为 `-24..+24`。
8. `abs(rawScore) <= 2` 定义为 ambiguous。
9. 只有 ambiguous dimensions 允许进入 AI clarification。
10. `InitialAssessmentResult` 是 deterministic、immutable baseline evidence。
11. AI 只提供 bounded structured clarification business output，不直接决定 final type。
12. AI structured business output 使用 `RESOLVED | UNCLEAR`；technical failure 由 Backend 映射为 `FAILED_RETRYABLE`。
13. Intermediate AI turns 不直接形成持久化 `ClarificationResult`。
14. `AIProvenance` 由 Backend / AI Integration 记录，表示 accepted ClarificationResult 的有效执行 provenance。
15. `FinalizationPolicy` 是 deterministic business logic。
16. AI clarification 可以在 ambiguous case 中翻转 questionnaire preference。
17. 原始 `InitialAssessmentResult` 永远保留，不被 AI 覆盖。
18. Technical AI failure 不直接 finalize；用户必须 Retry 或显式 Skip。
19. Skip / Decline AI 时，non-zero baseline 使用 questionnaire fallback。
20. Exact tie 且 clarification `UNCLEAR` / `SKIPPED` 时，由用户进行 explicit tie-break。
21. User tie-break 作为持久业务事实参与 `FinalDimensionConclusion`。
22. 四个 `FinalDimensionConclusion` deterministic 地组成最终四字母 type。
23. Questionnaire percentage 永远来自 deterministic raw score；AI 不生成“调整后百分比”。
24. `AssessmentDefinitionVersion` 是 authoritative specification snapshot；内部 Policy 为 VO。
25. executable / available DefinitionVersion 一旦对用户可用即 immutable。
26. 历史解释强调 traceability / explainability；只有 deterministic components 要求 reproducibility。
27. MVP 不持久化完整 AI conversation transcript 或完整 LLM retry history。
28. MVP 不使用第三方测试题库。
29. 产品不以 MBTI / 16Personalities 等第三方品牌命名。
30. 当前 question accuracy optimization 不是 MVP blocker。
31. Professional psychometric validation 不属于当前 scope。
32. Advanced Anti-Gaming 作为 Future Improvement。

---

## 24. Design Summary

当前方案可以概括为：

```text
Original Questionnaire
        +
Deterministic Scoring
        +
Explicit Ambiguity
        +
Bounded AI Assistance
        +
Deterministic Finalization
        +
Assessment Versioning
        +
Traceable Decision History
```

对于本 Portfolio Project，核心展示价值是：

```text
Domain Modeling
Business Rules
AI Boundary Design
Explainability
Persistence
Testing
Versioning
Frontend / Backend Contract
```

因此 MVP 的原则是：

> **测试内容做到合理、完整、可实现即可；系统设计、工程质量、可解释性和可测试性是当前主要目标。**

未来有额外时间时，再继续提升：

> **Assessment Quality + Anti-Gaming Capability**
