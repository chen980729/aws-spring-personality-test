# Four-Dimension Personality Assessment Specification

> **DefinitionVersion 1.1 status (ADR-0018):** Compatibility, Frontend dual-version support, and Flyway V8 activation are complete. DefinitionVersion 1.1 is the sole `AVAILABLE` version for new Sessions; 1.0 is `RETIRED` for new binding but remains immutable and executable for retained Sessions.

> **File:** `docs/sixteen-personality-spec-aligned.md`\
> **Status:** Immutable 1.0 baseline / Active 1.1 specification\
> **Last updated:** 2026-10-06\
> **Scope:** Personality assessment domain rules, scoring, AI clarification, finalization, result composition, and content/legal boundaries.

---

## 1. Purpose

This project provides an original four-dimension personality preference assessment that produces a four-letter Personality Type such as INFJ or ENTP.

The MVP is not intended to be a professionally validated psychometric instrument. Its purpose is to provide a complete, explainable, and testable business domain for demonstrating:

- React questionnaire flow;
- Spring Boot domain modeling;
- deterministic scoring;
- ambiguity handling;
- AI-assisted clarification;
- structured AI output;
- persistence and auditability;
- result visualization;
- automated testing;
- API contract design;
- versioning;
- AWS deployment and observability integration.

The MVP therefore prioritizes clear business rules, repeatable scoring, a strict boundary between AI and deterministic business logic, traceable evidence/results, and implementation complexity appropriate for the portfolio.

**Professional psychometric validation is outside the MVP scope.**

---

## 2. Product Positioning

The product is an:

> **Original four-dimension personality preference assessment**

It is not the official MBTI® assessment and is not a reproduction of 16Personalities / NERIS Type Explorer®.

The historical repository filename is an internal name only. Product UI, README marketing copy, and API-facing product names should not present the assessment as:

- MBTI Test;
- Free MBTI;
- AI MBTI;
- 16Personalities;
- Sixteen Personality Test.

Independent working names such as “Personality Type Explorer” or “Four-Dimension Personality Explorer” are preferred.

---

## 3. Assessment Model

The assessment contains four dimensions:

| Dimension | Pole A | Pole B |
|---|---|---|
| EI | Extraversion (E) | Introversion (I) |
| SN | Sensing (S) | Intuition (N) |
| TF | Thinking (T) | Feeling (F) |
| JP | Judging (J) | Perceiving (P) |

Pole A is the positive scoring direction:

~~~text
EI: E = positive, I = negative
SN: S = positive, N = negative
TF: T = positive, F = negative
JP: J = positive, P = negative
~~~

Finalized dimensions are composed in the fixed order EI + SN + TF + JP. For example, I + N + F + J produces INFJ.

The final Personality Type must be composed deterministically from the four finalized dimensions. **AI never directly generates or rewrites the four-letter type.**

---

## 4. Questionnaire Structure

### 4.1 Number of Questions

The MVP contains exactly 48 questions: 12 per dimension.

~~~text
4 dimensions × 12 questions = 48 questions
~~~

This is a deliberate engineering balance between assessment length and dimension-level signal.

### 4.2 Question Distribution

Questions from the four dimensions are interleaved rather than grouped:

~~~text
Q1  -> EI
Q2  -> SN
Q3  -> TF
Q4  -> JP
Q5  -> EI
...
~~~

The MVP does not require dynamic item selection. Question randomization and a larger item pool remain future improvements.

---

## 5. Answer Scale

The questionnaire uses a five-point Likert scale:

| Stored Value | UI Meaning |
|---:|---|
| 1 | Strongly Disagree |
| 2 | Disagree |
| 3 | Neither Agree nor Disagree |
| 4 | Agree |
| 5 | Strongly Agree |

The Frontend must present semantic labels rather than bare numbers. The Backend stores the canonical integer value 1..5.

---

## 6. Balanced Keying

Each dimension has:

~~~text
6 questions keyed toward Pole A
6 questions keyed toward Pole B
~~~

The MVP avoids unnecessarily complex negative wording. The Domain model expresses the real meaning of an item with dimension + keyedPole rather than relying only on a generic reverseScored flag.

---

## 7. Scoring

### 7.1 Centered Item Score

Answers are centered as follows:

~~~text
1  2  3  4  5
-2 -1  0 +1 +2
~~~

~~~text
centeredScore = answer - 3
~~~

### 7.2 Keyed Direction

For a Pole A keyed item:

~~~text
itemScore = answer - 3
~~~

For a Pole B keyed item:

~~~text
itemScore = -(answer - 3)
~~~

### 7.3 Dimension Raw Score

Each dimension contains 12 items, each contributing -2..+2:

~~~text
rawScore ∈ [-24, +24]

rawScore > 0 -> Pole A
rawScore < 0 -> Pole B
rawScore = 0 -> exact tie
~~~

### 7.4 Reference Pseudocode

~~~text
scoreDimension(dimensionAnswers):
    rawScore = 0

    for each answer:
        centered = answer.value - 3

        if answer.question.keyedPole == dimension.poleA:
            rawScore += centered
        else:
            rawScore -= centered

    return rawScore
~~~

Scoring is deterministic Application/Domain logic and never calls AI.

---

## 8. Preference Percentage

Percentages exist for presentation, not as the source of truth for business decisions.

~~~text
poleAPercentage = 50 + (rawScore / 24) * 50
poleBPercentage = 100 - poleAPercentage
~~~

| Raw Score | Approx. Split |
|---:|---|
| 0 | 50.00 / 50.00 |
| +1 | 52.08 / 47.92 |
| +2 | 54.17 / 45.83 |
| +3 | 56.25 / 43.75 |
| -2 | 45.83 / 54.17 |

Frontend rounding is allowed. **Ambiguity and finalization always use raw score, never the rounded percentage.**

---

## 9. Ambiguity Policy

The product concept is approximately a 45/55 ambiguity band. The executable deterministic rule is:

~~~text
ambiguous = abs(rawScore) <= 2
~~~

Therefore 50/50, approximately 52/48, and approximately 54/46 are ambiguous; approximately 56/44 is non-ambiguous.

This threshold is a product rule, not a scientifically validated psychometric threshold.

---

## 10. Result Stages

The specification uses the same Ubiquitous Language as the Assessment Domain:

~~~text
InitialAssessmentResult
ClarificationResult
FinalAssessmentResult
~~~

InitialAssessmentResult contains InitialDimensionResult values. FinalAssessmentResult contains FinalDimensionConclusion values. The system must not use one generic mutable result that later workflow steps overwrite.

### 10.1 InitialAssessmentResult

Deterministic questionnaire scoring creates the immutable baseline evidence. AI clarification never changes or overwrites it.

### 10.2 ClarificationResult

A ClarificationResult exists only for an ambiguous dimension when a clarification has completed and its business outcome is accepted.

A representative result contains:

~~~text
dimension
resolution = RESOLVED | UNCLEAR
suggestedPole?
confidence
reasoningSummary? / evidence?
~~~

RESOLVED requires a suggested pole. UNCLEAR requires suggestedPole = null.

Intermediate AI turns are not ClarificationResult values. Only the accepted business outcome is persisted as the clarification result.

### 10.3 FinalAssessmentResult

FinalizationPolicy deterministically combines accepted facts:

~~~text
InitialAssessmentResult
+
ClarificationResult / explicit skip / DimensionTieBreak facts
~~~

Each dimension becomes one FinalDimensionConclusion, and the four conclusions are composed into the final Personality Type. AI does not persist the final type directly.

---

## 11. AI Clarification

### 11.1 Trigger

Only dimensions where ambiguous == true are eligible for AI clarification.

### 11.2 AI Business Output

AI receives contextual information for one ambiguous dimension and returns structured business output. At minimum:

~~~text
dimension
resolution
suggestedPole?
confidence
~~~

Optional business-facing evidence or a short reasoningSummary may be retained. The summary is for product explanation/audit and must not depend on storing private chain-of-thought.

Technical execution outcomes such as timeout, provider error, or malformed response are not model-reported business states. They belong to the Backend / AI Integration execution boundary.

### 11.3 DimensionClarification Lifecycle vs AI Output

DimensionClarification lifecycle:

~~~text
PENDING
IN_PROGRESS
CLARIFIED
SKIPPED
FAILED_RETRYABLE
~~~

Typical mapping:

~~~text
AI RESOLVED
-> accepted ClarificationResult
-> DimensionClarification = CLARIFIED

AI UNCLEAR
-> accepted ClarificationResult with suggestedPole = null
-> DimensionClarification = CLARIFIED
-> FinalizationPolicy selects fallback/tie-break behavior

technical provider failure
-> no accepted ClarificationResult
-> DimensionClarification = FAILED_RETRYABLE
~~~

A technical failure must never be disguised as a valid UNCLEAR business outcome.

### 11.4 AI Provenance

Model identifier and clarification-policy revision are captured by Backend / AI Integration from the real execution context, not trusted as self-declared model output.

~~~text
AIProvenance
├── modelIdentifier
└── clarificationPolicyRevision
~~~

For MVP, AIProvenance describes the accepted execution that produced the accepted ClarificationResult. It is not a complete log of every call, retry, intermediate model, or raw provider request/response. A future ClarificationAttempt[] model may be introduced only if full execution audit becomes a concrete requirement.

### 11.5 Conversation Runtime State

The complete AI conversation transcript is not authoritative Assessment history. Active clarification may use temporary/ephemeral multi-turn context, but MVP does not guarantee exact-message resume after runtime loss.

If temporary context is lost:

- the persisted AssessmentSession remains valid;
- InitialAssessmentResult remains unchanged;
- the current dimension may restart clarification;
- only the accepted ClarificationResult, required AIProvenance, and optional short summary become durable business history.

### 11.6 AI Boundary

AI may not directly decide finalType and may not modify InitialAssessmentResult.

The responsibility chain remains:

~~~text
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
~~~

---

## 12. FinalizationPolicy

FinalizationPolicy is deterministic business logic over persisted/accepted facts.

### 12.1 Non-Ambiguous

A non-ambiguous questionnaire preference becomes the final preference with source QUESTIONNAIRE. AI does not participate and cannot override it.

### 12.2 Ambiguous + Clarification Agrees

When clarification confirms the questionnaire preference:

~~~text
source = QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION
~~~

### 12.3 Ambiguous + Clarification Flips

An accepted clarification may flip an ambiguous questionnaire preference:

~~~text
source = AI_CLARIFICATION
overrodeBaseline = true
~~~

Both questionnairePreference and finalPreference remain visible; the InitialDimensionResult is never overwritten.

### 12.4 UNCLEAR + Non-Zero Baseline

If clarification is UNCLEAR and the questionnaire is not an exact tie, FinalizationPolicy falls back deterministically to the questionnaire preference:

~~~text
source = QUESTIONNAIRE_FALLBACK
~~~

This means AI supplied insufficient evidence to change the baseline; it does not mean AI selected the fallback pole.

### 12.5 Technical AI Failure

Provider timeout, call failure, or invalid structured output transitions the clarification to FAILED_RETRYABLE. Finalization does not proceed automatically. The user may Retry or explicitly Skip; only an explicit Skip allows FinalizationPolicy to continue with questionnaire/tie-break rules.

~~~text
technical failure != UNCLEAR business result
~~~

### 12.6 Clarification Skipped / AI Declined

For a non-zero baseline, Skip/Decline uses QUESTIONNAIRE_FALLBACK.

For an exact 50/50 tie, no questionnaire preference may be invented. A version-specific explicit tie-break is required.

### 12.7 Exact Tie + UNCLEAR / SKIPPED

A fixed hidden winner such as “E always wins” is forbidden.

Behavior is determined by the Session-bound immutable DefinitionVersion:

- **1.0 legacy:** direct selection of one legal pole; source = USER_TIE_BREAK.
- **1.1 / FinalizationPolicy v2:** one binary contextual forced-choice question per unresolved exact-tie dimension; source = TIE_BREAK_QUESTION.

Both paths persist DimensionTieBreak and preserve the questionnaire raw score/evidence at 50/50. A RESOLVED clarification that already supplies a legal pole does not require tie-break. Technical failure alone does not trigger tie-break.

The Frontend must not receive or display option-to-pole mappings. It may reorder presentation, but must submit stable option IDs rather than interpreting A/B display position.

### 12.8 Accepted Contextual Questions — DefinitionVersion 1.1

Shared instruction:

> **Both options may describe you in different situations. If you had to choose, select the one that feels more natural to you most of the time.**

#### EI Tie-Break

**Question**

> When you are trying to make sense of an important issue, which approach more often helps your thoughts become clear?

**Option A — maps to E**

> I start discussing it with someone and often discover what I think while talking.

**Option B — maps to I**

> I first spend some time thinking it through privately, then share my thoughts once they have taken shape.

#### SN Tie-Break

**Question**

> When you face an unfamiliar problem with incomplete instructions, which starting point feels more natural?

**Option A — maps to S**

> I first gather concrete facts, examples, and what has worked before, then build the solution from there.

**Option B — maps to N**

> I first form an overall picture of the patterns and possibilities, then use specific details to test or refine it.

#### TF Tie-Break

**Question**

> When two solutions are both reasonable but you must choose one, which consideration is more likely to guide your final decision?

**Option A — maps to T**

> I prefer the option supported by the most consistent criteria and defensible trade-offs, even if not everyone likes the outcome.

**Option B — maps to F**

> I prefer the option that best accounts for the people involved and preserves trust, even if the rule is applied less uniformly.

#### JP Tie-Break

**Question**

> You begin an important project whose requirements may still change. Which approach feels more comfortable?

**Option A — maps to J**

> I establish a provisional plan, milestones, and next steps early, then revise them if new information appears.

**Option B — maps to P**

> I keep the structure relatively open at first, gather more information, and commit to details when they become necessary.

The A/B labels above describe documentation order only. Stable identifiers and Backend-only resolution are:

| Dimension | questionId | Option A optionId | Backend resolvedPole | Option B optionId | Backend resolvedPole |
|---|---|---|---|---|---|
| EI | TB-EI-1 | TB-EI-01 | E | TB-EI-02 | I |
| SN | TB-SN-1 | TB-SN-01 | S | TB-SN-02 | N |
| TF | TB-TF-1 | TB-TF-01 | T | TB-TF-02 | F |
| JP | TB-JP-1 | TB-JP-01 | J | TB-JP-02 | P |

### 12.9 Specification Validation and Persisted Decision

DefinitionVersion 1.1 adds TieBreakQuestionDefinition[] to AssessmentSpecification. Each definition contains questionId, dimension, instruction, prompt, and exactly two options; each option contains optionId, text, and Backend-only resolvedPole.

Validation requires:

- the referenced dimension exists;
- FinalizationPolicy v2 has exactly one tie-break question per dimension;
- question IDs are unique within the specification;
- each question has exactly two options;
- option IDs are unique within the question;
- the two options resolve to the dimension’s two different legal poles.

DimensionTieBreak remains the persisted business fact; no separate TieBreakResponse domain object is introduced. Conceptual fields are sessionId, dimension, questionId, selectedOptionId, resolvedPole, and decidedAt.

For 1.0, questionId/selectedOptionId remain null because no contextual question existed. For 1.1, the real identifiers must be stored. Backend always validates and resolves the submitted option against the Session-bound immutable DefinitionVersion.

---

## 13. Final Type Composition

Only after all four FinalDimensionConclusion values exist may PersonalityTypeComposer combine EI, SN, TF, and JP into the final four-letter type.

PersonalityTypeComposer performs composition only. It does not call AI or reinterpret questionnaire answers.

---

## 14. Result Presentation

The result page should expose at least three layers:

1. final four-letter type;
2. questionnaire evidence plus final per-dimension conclusion/source;
3. non-clinical type description.

Questionnaire percentages always represent deterministic raw-score evidence. They are never “AI-adjusted percentages”.

If clarification flips a baseline, the UI must distinguish questionnaire evidence from the final categorical conclusion.

Descriptions should use preference language such as “may tend to” or “is likely to prefer” rather than absolute personality claims. The product is for self-reflection and not clinical/professional psychological assessment.

---

## 15. Assessment Versioning

Semantic changes may affect question wording/set, keyed poles, ambiguity/scoring policy, clarification policy, or finalization policy. Historical Sessions must remain bound to their exact AssessmentDefinitionVersion.

### 15.1 Authoritative Version Snapshot

DefinitionVersion 1.0 is the immutable legacy specification with direct-pole exact-tie semantics.

DefinitionVersion 1.1 reuses the same 48 questionnaire questions, wording, order, keyed poles, ScoringPolicy, AmbiguityPolicy, and clarification-policy revision. It changes FinalizationPolicy to revision v2 and adds the accepted TieBreakQuestionDefinition[] from sections 12.8–12.9.

The expand-then-promote rollout is complete:

~~~text
compatibility release:
1.0 AVAILABLE
1.1 DRAFT

Flyway V8 activation:
1.0 RETIRED
1.1 AVAILABLE
~~~

New Sessions now bind 1.1. Existing 1.0 Sessions never auto-upgrade and remain executable with their original semantics. See ADR-0018.

Internal policy revisions are metadata inside the DefinitionVersion snapshot; they are not independent competing version lifecycles.

### 15.2 Clarification Policy Provenance

AssessmentDefinitionVersion may bind the expected/default ClarificationPolicy revision, while each accepted DimensionClarification records the actual AIProvenance used at runtime. Historical explanation uses both the immutable specification and actual accepted-run provenance.

### 15.3 Immutability and Traceability

Once a DefinitionVersion is executable/available to users, its specification is immutable. Semantic changes require a new DefinitionVersion.

Historical assessment behavior targets traceability and explainability, with reproducibility for deterministic components. LLM output is not claimed to be strictly reproducible across future executions.

---

## 16. Persistence / Audit Requirements

At minimum, retained assessment history must preserve:

~~~text
AssessmentSession
  ├─ AssessmentDefinitionVersion
  ├─ QuestionnaireResponse / answers
  ├─ InitialAssessmentResult
  │    └─ InitialDimensionResult[]
  ├─ DimensionClarification[]
  │    ├─ ClarificationResult (when accepted)
  │    └─ AIProvenance
  ├─ DimensionTieBreak (when required)
  └─ FinalAssessmentResult
       └─ FinalDimensionConclusion[]
~~~

Requirements:

1. submitted answers remain available for the retained Session lifecycle and are never overwritten by AI/new rules;
2. InitialAssessmentResult is retained and immutable;
3. clarification never overwrites baseline evidence;
4. accepted ClarificationResult and required AIProvenance are durable;
5. raw provider retries/transcripts are not MVP core-domain persistence;
6. FinalAssessmentResult retains per-dimension decision source;
7. 1.0 tie-break persists direct-pole USER_TIE_BREAK; 1.1 persists real question/option identifiers plus TIE_BREAK_QUESTION;
8. historical results are never silently recomputed under later DefinitionVersions;
9. deletion follows the project-wide historical Assessment deletion policy; immutability means “not mutated”, not “never deletable”.

---

## 17. Question Content Policy

The MVP uses original questions only. It does not copy official MBTI instrument items, 16Personalities questions, commercial assessment banks, or third-party questions without clear reuse rights. Simple translation of protected questions is not treated as original content.

Public personality theory and questionnaire-design methodology may inform the work, but final wording must be independently authored for this project.

---

## 18. Naming / Copyright / Trademark Boundary

### 18.1 MBTI

Myers-Briggs Type Indicator, Myers-Briggs, and MBTI are trademarks/registered trademarks of their respective rights holders. The project does not claim to be an official MBTI assessment, market itself as an MBTI Test, or copy official instrument items. References to MBTI in documentation are background/context only.

Official reference:

- https://www.myersbriggs.org/using-type-as-a-professional/mbti-permission-trademarks/

### 18.2 16Personalities

The project does not copy 16Personalities question wording, type-profile copy, or nicknames and is not presented as a 16Personalities product or compatible implementation.

Official references:

- https://www.16personalities.com/for-ai
- https://www.16personalities.com/terms

### 18.3 General Copyright Principle

Copyright generally protects concrete expression rather than ideas, procedures, methods, or systems. The project may independently implement a four-dimension scoring/assessment approach while avoiding copying protected question or explanatory wording.

References:

- https://www.copyright.gov/what-is-copyright/
- https://www.copyright.gov/help/faq/faq-protect.html

This section defines implementation boundaries and is not legal advice. Commercialization would require a separate trademark/licensing review.

---

## 19. MVP Scope

### In Scope

- 48 original questions;
- 12 questions per dimension;
- five-point Likert scale;
- balanced keying;
- deterministic scoring;
- raw-score ambiguity detection;
- AI clarification for ambiguous dimensions;
- deterministic FinalizationPolicy;
- final type composition;
- result/history presentation;
- assessment versioning;
- automated scoring/finalization tests.

### Out of Scope

- formal psychometric validation;
- large-scale norming;
- Cronbach’s alpha optimization;
- exploratory/confirmatory factor analysis;
- Item Response Theory;
- demographic norm calibration;
- clinical interpretation or professional diagnosis;
- hiring/employment assessment;
- sophisticated anti-gaming;
- adaptive item selection.

---

## 20. Future Improvement — Assessment Quality

Future item-quality work may review construct validity, social-desirability bias, wording bias, cultural bias, and whether questions within one dimension are overly repetitive. With sufficient lawful anonymous data, reliability/factor structure could also be studied.

These improvements are not blockers for the engineering MVP.

---

## 21. Future Improvement — Anti-Gaming

A future goal is to make it difficult to deliberately force a target type even when the user understands conventional personality-test patterns.

Potential techniques include:

- indirect situational items;
- contextual questions across work/learning/social/decision/planning/stress contexts;
- cross-checking the same construct with different wording/context;
- larger item pools with per-assessment selection;
- question/presentation randomization;
- consistency/confidence analysis;
- contextual AI clarification.

The design principle is:

~~~text
Question meaning     = clear to the user
Scoring implication  != obvious to the user
~~~

Anti-gaming must not rely on confusing language, double negatives, or deception. Stable question IDs and scoring definitions remain required.

---

## 22. Engineering Value of Future Anti-Gaming

Possible future concepts include QuestionBank, QuestionSelectionStrategy, ConsistencyScore, DimensionConfidence, and ClarificationTrigger.

These are future enhancements and do not change the current MVP.

---

## 23. MVP Acceptance Criteria

### Questionnaire

- [x] One complete assessment contains exactly 48 questions.
- [x] EI / SN / TF / JP each contain 12 questions.
- [x] Each dimension contains six Pole A-keyed and six Pole B-keyed items.
- [x] Each question accepts only 1..5.
- [x] Questionnaire completion requires all mandatory answers.

### Scoring

- [x] Each transformed item score is -2..+2.
- [x] Each dimension raw score is -24..+24.
- [x] Identical answers produce the same InitialAssessmentResult.
- [x] abs(rawScore) <= 2 sets ambiguous = true.
- [x] Percentage rounding does not affect ambiguity.

### Clarification Boundary

- [x] Non-ambiguous dimensions cannot enter clarification.
- [x] AI structured business output does not encode technical failure state.
- [x] RESOLVED/UNCLEAR domain result semantics are defined.
- [x] Technical failure maps to FAILED_RETRYABLE rather than finalization.
- [x] AI cannot overwrite InitialAssessmentResult.
- [x] AIProvenance is Backend-owned execution context.
- [x] Intermediate turns are not durable ClarificationResult values.
- [x] Complete AI transcript persistence is not required by MVP.
- [ ] Provider-backed LLM execution, parsing, prompt/runtime context, and safety/privacy review are Step 8 work.

### Finalization

- [x] Non-ambiguous dimensions use questionnaire baseline.
- [x] Accepted clarification may confirm or flip an ambiguous baseline.
- [x] Baseline and final preference remain visible when clarification flips.
- [x] UNCLEAR + non-zero baseline uses deterministic questionnaire fallback.
- [x] Technical failure requires Retry or explicit Skip.
- [x] Skip/Decline + non-zero baseline uses questionnaire fallback.
- [x] Exact tie + UNCLEAR/SKIPPED requires explicit tie-break.
- [x] DimensionTieBreak persists 1.0 USER_TIE_BREAK or 1.1 TIE_BREAK_QUESTION provenance.
- [x] DefinitionVersion 1.1 has exactly one accepted contextual tie-break question per dimension.
- [x] Frontend never receives option-to-pole mappings.
- [x] Retired 1.0 Sessions remain executable without auto-upgrade.
- [x] Final type is composed only from four FinalDimensionConclusion values.

### Result Presentation / Versioning / History

- [x] Questionnaire percentage always comes from deterministic raw score.
- [x] AI does not generate adjusted percentages.
- [x] UI distinguishes evidence from final categorical conclusion.
- [x] Every AssessmentSession binds an exact AssessmentDefinitionVersion.
- [x] Executable/available DefinitionVersions are immutable.
- [x] Historical evidence/results are not overwritten by later versions.
- [x] DefinitionVersion 1.1 activation is complete for new Sessions.
- [x] Users can view completed Assessment history.
- [ ] Provider-backed LLM interaction remains the next product milestone.
- [ ] Historical deletion orchestration remains blocked on the Group sharing boundary.

---

# Appendix A — MVP Question Bank v1.0 (unchanged and reused by DefinitionVersion 1.1)

> This question bank is the accepted MVP baseline. It satisfies the engineering requirements but has not undergone professional psychometric validation.  
> Future wording/item changes require a new AssessmentDefinitionVersion rather than in-place mutation.

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

1. The assessment uses 48 original questions, 12 per dimension.
2. It uses a five-point Likert scale and 6+6 balanced keying per dimension.
3. Backend scoring uses centered deterministic raw scores in -24..+24.
4. abs(rawScore) <= 2 defines an ambiguous dimension.
5. Only ambiguous dimensions may enter clarification.
6. InitialAssessmentResult is deterministic, immutable baseline evidence.
7. AI produces bounded structured clarification output and never directly decides final type.
8. AI business resolution is RESOLVED or UNCLEAR; technical failure maps to FAILED_RETRYABLE outside model output.
9. Intermediate AI turns do not directly become durable ClarificationResult values.
10. AIProvenance is recorded by Backend / AI Integration for the accepted execution.
11. FinalizationPolicy is deterministic business logic.
12. Accepted clarification may flip an ambiguous questionnaire preference while preserving the baseline.
13. Technical failure never directly finalizes; the user must Retry or explicitly Skip.
14. Skip/Decline with a non-zero baseline uses questionnaire fallback.
15. Exact tie with UNCLEAR/SKIPPED requires explicit version-aware tie-break.
16. DimensionTieBreak is a durable business fact: 1.0 direct pole / USER_TIE_BREAK; 1.1 contextual option / TIE_BREAK_QUESTION.
17. Four FinalDimensionConclusion values deterministically compose the final type.
18. Questionnaire percentages always come from deterministic raw score; AI never creates adjusted percentages.
19. AssessmentDefinitionVersion is the authoritative immutable specification snapshot.
20. DefinitionVersion 1.1 is active for new Sessions; 1.0 is retired for new bindings but retained Sessions remain executable.
21. Historical interpretation emphasizes traceability/explainability; deterministic components are reproducible.
22. MVP does not persist complete AI transcripts or complete LLM retry history.
23. MVP does not use third-party protected question banks or third-party product branding.
24. Professional psychometric validation and advanced anti-gaming remain future work.

---

## 24. Design Summary

The current design can be summarized as:

~~~text
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
~~~

The main portfolio value is the combination of Domain Modeling, explicit business rules, AI boundary design, explainability, persistence, testing, versioning, and the Frontend/Backend contract.

For the MVP, assessment content must be reasonable, complete, and implementable; system design, engineering quality, explainability, and testability are the primary goals. Assessment-quality and anti-gaming improvements remain future work.
