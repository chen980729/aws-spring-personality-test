# Group Domain Specification

> **File:** `docs/domain/group-spec.md`  
> **Status:** MVP Baseline / Accepted  
> **Last updated:** 2026-09-19  
> **Scope:** Group lifecycle, membership, join requests, admin authority, result sharing consent, aggregate boundaries, invariants, commands, queries, and persistence facts.

---

## 1. Purpose

`Group` 是本项目 MVP 中的主要 Supporting Business Domain Model。

它的主要目标不是实现复杂的社交网络或组织权限系统，而是提供一个足够真实、可解释、可测试的协作场景，用于展示：

- Spring Boot domain modeling
- Entity lifecycle modeling
- authorization
- privacy / consent design
- cross-aggregate orchestration
- transaction consistency
- PostgreSQL uniqueness constraints
- concurrency considerations
- Assessment 与 Group 的 bounded responsibility
- automated testing
- API contract design

MVP 优先保证：

1. Group、Membership、JoinRequest、Sharing 的业务语义明确；
2. 生命周期转换可测试且不会产生矛盾状态；
3. Admin authority 与 Creator historical fact 清晰分离；
4. Group 只能获得用户明确授权共享的 completed Assessment result；
5. Membership / Sharing history 可以被保留；
6. Aggregate 不因历史 Membership / JoinRequest 无限增长而膨胀；
7. 实现复杂度与 Portfolio Project 目标匹配。

---

## 2. MVP Product Boundary

MVP Group flow：

```text
Create Group
    ↓
Creator becomes Admin + Member
    ↓
Other user enters Group Code
    ↓
Join Request
    ↓
Admin Approve / Reject
    ↓
Active Membership
    ↓
Member optionally shares the finalized result of one specific COMPLETED AssessmentSession
```

MVP 支持：

- Create Group
- Join by Group Code
- Join Request
- Approve / Reject Request
- View Members
- Transfer Admin
- Leave Group
- Disband Group
- Explicitly share one completed Assessment result
- Change shared result
- Stop sharing result
- View results explicitly shared inside the Group

MVP 不实现：

- Email Invitation
- Group Avatar
- Multiple Admins
- Kick Member
- Blacklist
- Group Chat / Feed
- Complex RBAC
- Organization hierarchy
- Restoring a disbanded Group

> **Scope alignment note:** earlier requirement drafts treated `Admin Transfer` as a future feature and considered sharing the user's "latest result". This specification supersedes those preliminary decisions: **single-admin transfer is now part of MVP**, and sharing is **explicitly bound to one specific completed `AssessmentSession` result**.

---

## 3. Ubiquitous Language

### 3.1 Group

`Group` 表示一个长期存在的协作 / sharing space。

它拥有：

- identity
- name
- group code
- lifecycle
- creator historical fact
- current admin authority

它不拥有：

- User
- AssessmentResult
- Membership history collection
- JoinRequest history collection

### 3.2 Creator

`Creator` 表示：

> 创建 Group 的 User。

它是 immutable historical fact：

```text
Group.createdByUserId
```

Creator **不是 Role**，也不拥有永久管理权限。

### 3.3 Admin

`Admin` 表示：

> 当前唯一具有 Group management authority 的 Active Member。

MVP 每个 Active Group 恰好只有一个 Admin：

```text
Group.adminUserId
```

Admin 可以转让。

### 3.4 GroupMembership

`GroupMembership` 表示：

> 一个 User 已经成为、或曾经成为某个 Group 成员这一业务事实。

Join intent 不属于 Membership。

### 3.5 JoinRequest

`JoinRequest` 表示：

> User 希望建立 GroupMembership 的申请。

因此：

```text
JoinRequest = intent
GroupMembership = established relationship
```

### 3.6 GroupAssessmentShare

`GroupAssessmentShare` 表示：

> 一个 Active Member 明确同意将某一次具体、已完成的 `AssessmentSession` 所产生的 finalized result 分享给某一个 Group。

它是 consent / visibility fact，而不是 Assessment data 本身。Group 只长期引用 `AssessmentSessionId`；该 Session 是否可分享、其 finalized result 是什么，仍由 Assessment Domain 决定。

---

## 4. Core Domain Objects

MVP Group Domain 包含四个独立 Aggregate Root：

```text
Group Domain
│
├── Group
│
├── GroupMembership
│
├── JoinRequest
│
└── GroupAssessmentShare
```

Identity / reference types：

```text
GroupId
MembershipId
JoinRequestId
ShareId
UserId
AssessmentSessionId
GroupCode
```

这些 ID 应作为明确的 domain identifiers 使用，而不是依赖隐式 object graph。

---

## 5. Group Lifecycle

### 5.1 Status

```text
GroupStatus
- ACTIVE
- DISBANDED
```

生命周期：

```text
ACTIVE
  |
  └──> DISBANDED
```

`DISBANDED` 是 terminal state。

禁止：

```text
DISBANDED -> ACTIVE
```

MVP 不需要：

```text
SUSPENDED
ARCHIVED
PAUSED
```

这些状态没有当前业务需求。

### 5.2 Active Group

对任意 `ACTIVE` Group：

```text
exactly one Admin exists
```

并且：

```text
Admin must have an ACTIVE Membership
in the same Group
```

### 5.3 Disbanded Group

Group 被解散以后：

- 不允许新的 JoinRequest；
- 不允许 Approve / Reject；
- 不允许 TransferAdmin；
- 不允许 LeaveGroup；
- 不允许创建 / 修改 Sharing；
- 所有 Active Membership 被终止；
- 所有 Pending JoinRequest 被拒绝；
- 所有 Active Share 被终止。

`adminUserId` 可以保留最后一个 Admin，作为 historical fact。

---

## 6. Group Creation

创建 Group：

```text
CreateGroup(creator)
```

必须原子地产生：

```text
Group
- status = ACTIVE
- createdByUserId = creator
- adminUserId = creator

GroupMembership
- userId = creator
- status = ACTIVE
```

因此：

> Active Group 不能在没有 Active Admin Membership 的情况下存在。

实现阶段：

```text
Create Group
+
Create creator Membership
```

必须位于同一个 local database transaction 中。

### 6.1 Group Code

MVP 使用 `GroupCode` 作为加入 Group 的发现入口：

```text
Group Code
→ locate Group
→ submit JoinRequest
```

`GroupCode` 只是 discovery identifier：

```text
knowing GroupCode
!=
being a Member
!=
having access to shared results
```

MVP baseline：

- 由 Backend 生成；
- 创建 Group 时分配；
- 全局唯一；
- 创建后保持 immutable；
- 已使用的 code 不重新分配给其他 Group；
- `DISBANDED` Group 的 code 不再接受 JoinRequest。

具体字符集、长度以及 collision retry strategy 在 Database / Application Design 阶段决定。

---

## 7. Creator / Admin / Member Semantics

三者必须严格区分：

```text
Creator
= immutable historical fact

Admin
= mutable current authority

Member
= active membership relationship
```

例如：

```text
Alice creates Group
↓
createdBy = Alice
admin = Alice
Alice Membership = ACTIVE

Alice transfers admin to Bob
↓
createdBy = Alice
admin = Bob

Alice leaves
↓
createdBy = Alice
admin = Bob
Alice Membership = ENDED
```

这个状态完全合法。

Creator 本身不获得：

- permanent admin authority
- permanent membership
- privacy override
- special result visibility

---

## 8. Admin Model

MVP 只允许：

```text
exactly one Admin
```

因此 Admin authority 保存于：

```text
Group.adminUserId
```

而不是：

```text
GroupMembership.role = ADMIN
```

MVP 不需要：

```text
MembershipRole.ADMIN
MembershipRole.MEMBER
```

这样可以直接避免：

```text
two Membership rows accidentally marked ADMIN
```

如果未来支持 multiple admins，再引入 Membership Role / Group Role model。

---

## 9. Admin Transfer

Command：

```text
TransferAdmin(groupId, currentAdminId, newAdminId)
```

必须满足：

```text
Group.status == ACTIVE

currentAdminId == Group.adminUserId

newAdminId != currentAdminId

newAdminId has ACTIVE Membership in this Group
```

执行：

```text
Group.adminUserId = newAdminId
```

MVP 中：

> Admin transfer 不需要 recipient confirmation。

这是明确的 MVP simplification。

未来可以演化为：

```text
AdminTransferRequest
PENDING
ACCEPTED
REJECTED
```

但当前不实现。

### 9.1 Deliberate Trade-off

因为 Admin 不能直接 Leave，Admin transfer without consent 意味着某个 Member 可能被立即赋予 Admin responsibility。

当前接受该 trade-off，以避免引入额外 transfer workflow。

被转让的 Admin 仍可：

- 再次 TransferAdmin；
- 或在没有可转让 Member 时 Disband Group。

---

## 10. GroupMembership Lifecycle

### 10.1 Status

```text
MembershipStatus
- ACTIVE
- ENDED
```

生命周期：

```text
ACTIVE
  |
  └──> ENDED
```

终止原因：

```text
MembershipEndReason
- LEFT
- GROUP_DISBANDED
```

不使用：

```text
ACTIVE -> LEFT
```

作为 status，因为：

```text
LEFT
```

只描述主动离开，无法准确表达因 Group Disband 被终止的 Membership。

### 10.2 Historical Membership

Membership 不在离开后恢复。

例如：

```text
Membership #101
joinedAt = Jan 1
endedAt  = Feb 1
endReason = LEFT

Membership #257
joinedAt = Mar 10
status   = ACTIVE
```

User 重新加入时创建新的 Membership。

历史 Membership 保留。

---

## 11. Membership Uniqueness

User 可以：

- 同时属于多个 Group；
- 同时向多个不同 Group 申请加入。

但是，对同一个：

```text
(GroupId, UserId)
```

必须满足：

```text
at most one ACTIVE Membership
```

历史上可以存在多个 Membership：

```text
Membership #1 ENDED
Membership #2 ENDED
Membership #3 ACTIVE
```

---

## 12. Leave Group

Command：

```text
LeaveGroup(groupId, userId)
```

必须满足：

```text
Group.status == ACTIVE

user has ACTIVE Membership

userId != Group.adminUserId
```

执行：

```text
Membership.status = ENDED
Membership.endReason = LEFT
Membership.endedAt = now
```

### 12.1 Admin Cannot Leave

当前 Admin 不能直接执行：

```text
LeaveGroup
```

Admin 想离开时必须先：

```text
TransferAdmin
↓
LeaveGroup
```

如果 Group 只有 Admin 一个 Member：

```text
TransferAdmin impossible
LeaveGroup impossible
DisbandGroup available
```

不存在业务 deadlock。

---

## 13. JoinRequest Lifecycle

### 13.1 Status

```text
JoinRequestStatus
- PENDING
- APPROVED
- REJECTED
```

生命周期：

```text
             ┌──> APPROVED
PENDING -----|
             └──> REJECTED
```

`APPROVED` 和 `REJECTED` 都是 terminal state。

### 13.2 Resolution Metadata

建议保存：

```text
JoinRequestRejectionReason
- ADMIN_REJECTED
- TIMED_OUT
- GROUP_DISBANDED
```

所有从 `PENDING` 进入 terminal state 的 JoinRequest 都记录：

```text
resolvedAt
resolvedBy?
```

`resolvedBy` 对 Admin 手动处理的 `APPROVED` / `ADMIN_REJECTED` 保存当前 Admin；对系统触发的 `TIMED_OUT` / `GROUP_DISBANDED` 可以为空。

只有 `REJECTED` 状态需要：

```text
rejectionReason
```

`APPROVED` 已由 status 本身完整表达，因此不需要 `rejectionReason`。

---

## 14. Request to Join

Command：

```text
RequestToJoinGroup(groupId, userId)
```

必须满足：

```text
Group.status == ACTIVE

no ACTIVE Membership exists

no PENDING JoinRequest exists

rejection cooldown has elapsed
```

成功后：

```text
JoinRequest.status = PENDING
requestedAt = now
expiresAt = requestedAt + 7 days
```

### 14.1 Pending Request Uniqueness

对同一个：

```text
(GroupId, UserId)
```

最多存在：

```text
one PENDING JoinRequest
```

但历史 Request 不删除：

```text
Request #1 REJECTED
Request #2 REJECTED
Request #3 APPROVED
```

都可以保留。

---

## 15. Rejection Cooldown

被拒绝后可以重新申请。

MVP cooldown：

```text
8 hours
```

计算规则：

```text
nextAllowedRequestAt
=
resolvedAt + 8 hours
```

适用于：

```text
ADMIN_REJECTED
TIMED_OUT
```

`GROUP_DISBANDED` 无需重新申请，因为 Group 已经 terminal。

MVP 不限制拒绝次数。

未来可以增加：

- blacklist
- temporary ban
- configurable cooldown

但当前不实现。

`nextAllowedRequestAt` 当前不需要单独持久化，因为可以由历史事实计算。

---

## 16. JoinRequest Expiration

Group 必须在：

```text
7 days
```

内处理 JoinRequest。

如果超过：

```text
expiresAt = requestedAt + 7 days
```

仍然是 `PENDING`：

```text
PENDING -> REJECTED
rejectionReason = TIMED_OUT
resolvedAt = expiresAt
```

`resolvedAt` 应表达业务 expiration time，而不是 scheduled job 实际碰巧执行的时间。

例如 job 在：

```text
10:05
```

才处理一个：

```text
expiresAt = 10:00
```

的 Request，则业务上：

```text
resolvedAt = 10:00
```

### 16.1 Domain Rule vs Scheduling Mechanism

```text
"7 天后自动拒绝"
```

是 Domain Rule。

至于实现使用：

- scheduled job
- lazy expiration
- background process

属于 Application / Infrastructure concern。

---

## 17. Approve JoinRequest

Command：

```text
ApproveJoinRequest(joinRequestId, adminUserId)
```

必须重新验证：

```text
Group.status == ACTIVE

adminUserId == Group.adminUserId

JoinRequest.status == PENDING

JoinRequest has not expired

Applicant has no ACTIVE Membership
```

执行：

```text
JoinRequest
PENDING -> APPROVED

+

Create NEW GroupMembership
status = ACTIVE

JoinRequest.resolvedAt = now
JoinRequest.resolvedBy = adminUserId
JoinRequest.rejectionReason = null
```

Approve 绝不能恢复历史 Membership。

---

## 18. Reject JoinRequest

Command：

```text
RejectJoinRequest(joinRequestId, adminUserId)
```

必须满足：

```text
Group.status == ACTIVE

adminUserId == Group.adminUserId

JoinRequest.status == PENDING
```

执行：

```text
JoinRequest.status = REJECTED
rejectionReason = ADMIN_REJECTED
resolvedAt = now
resolvedBy = adminUserId
```

之后进入 8-hour cooldown。

---

## 19. Disband Group

只有当前 Admin 可以执行：

```text
DisbandGroup
```

执行后的业务结果：

```text
Group
ACTIVE -> DISBANDED
```

以及：

```text
all ACTIVE Memberships
-> ENDED
   endReason = GROUP_DISBANDED
```

```text
all PENDING JoinRequests
-> REJECTED
   rejectionReason = GROUP_DISBANDED
```

```text
all ACTIVE GroupAssessmentShares
-> ENDED
   endReason = GROUP_DISBANDED
```

并记录：

```text
Group.disbandedAt
```

Group 不允许恢复。

---

## 20. Assessment Sharing Principle

Group 遵循：

> **Minimum Sharing + Explicit Consent**

加入 Group：

```text
does NOT automatically share assessment data
```

Admin：

```text
cannot force a member to share
```

Creator：

```text
has no privacy override
```

Group 不获得：

- complete Assessment history
- InitialAssessmentResult
- ClarificationResult
- AI conversation
- questionnaire answers

MVP 只允许用户主动分享：

```text
one specific completed AssessmentSession result
```

---

## 21. Why Share a Specific Result

不使用：

```text
share latest result automatically
```

因为用户重新完成 Assessment 后：

```text
Result #123  ← consented
Result #456  ← newly created
```

如果 Group 自动切换至 #456：

> 可见内容会在没有新 consent 的情况下发生变化。

因此：

```text
Creating a new assessment result
does not alter an existing share.
```

用户想改为 #456 时必须显式执行：

```text
ChangeSharedAssessmentResult
```

---

## 22. Sharing Consent Scope

Consent 采用：

```text
per Membership + per completed AssessmentSession
```

而不是：

```text
per user
per result globally
per account
```

例如同一 User 可以：

```text
Group A -> Result #123
Group B -> Result #456
Group C -> no share
```

这表示：

> 用户只向特定 Group、在特定 Membership relationship 下，授权可见某个具体 Result。

---

## 23. GroupAssessmentShare

`GroupAssessmentShare` 是独立 Aggregate Root。

建议概念字段：

```text
GroupAssessmentShare
- ShareId
- MembershipId
- AssessmentSessionId
- ShareStatus
- sharedAt
- endedAt?
- endReason?
```

它不复制 Assessment 内容。

---

## 24. Share Lifecycle

### 24.1 Status

```text
ShareStatus
- ACTIVE
- ENDED
```

### 24.2 End Reason

```text
ShareEndReason
- STOPPED_BY_MEMBER
- RESULT_REPLACED
- MEMBERSHIP_ENDED
- GROUP_DISBANDED
- ASSESSMENT_DELETED
```

Share history 保留。

---

## 25. Share Invariants

对于一个 ACTIVE Membership：

```text
0 or 1 ACTIVE Share
```

即：

```text
one Membership
↓
zero or one explicitly selected COMPLETED AssessmentSession result
```

MVP 不支持同时向同一个 Group 分享多个历史 Result。

同一个 `COMPLETED AssessmentSession` 的 finalized result 可以被用户分别分享给多个不同 Group：

```text
Completed AssessmentSession #123 result
├── Group A
└── Group B
```

每个 Share 都是独立 consent。

---

## 26. Start Sharing

Command：

```text
ShareAssessmentResult(
  membershipId,
  assessmentSessionId
)
```

必须满足：

```text
Group.status == ACTIVE

Membership.status == ACTIVE

requesting user owns the Membership

referenced AssessmentSession belongs to the same user

referenced AssessmentSession.status == COMPLETED

referenced AssessmentSession has a FinalAssessmentResult

Membership has no ACTIVE Share
```

只有 Membership owner 可以开始分享。

Admin 不能替 Member 创建 Share。

这里的 `AssessmentSessionId` 只是跨模块 reference。`Group` Aggregate 自己不能判断 Session 是否完成，也不会读取 Assessment 内部对象。

`ShareAssessmentResult` Application Use Case 必须向 Assessment Module 验证：

```text
AssessmentSession exists
AND owner == sharing user
AND status == COMPLETED
AND FinalAssessmentResult exists
```

如果 Session 仍处于：

```text
IN_PROGRESS
AWAITING_CLARIFICATION
CLARIFICATION_IN_PROGRESS
ABANDONED
```

或者虽有 ID 但不存在 finalized result，则 Share command 必须被拒绝。

因此：

```text
having an AssessmentSessionId
!=
having a shareable assessment result
```

可选实现方式是在 Application Boundary 使用一个经过 Assessment Module 验证后返回的概念：

```text
ShareableAssessmentResultRef
└── assessmentSessionId
```

但它只是 boundary representation，不创建新的 Assessment Domain Entity。

---

## 27. Change Shared Result

假设：

```text
Share #10
Result #123
ACTIVE
```

Member 改为：

```text
Result #456
```

不直接修改：

```text
Share #10.resultId
```

而应该：

```text
Share #10
ACTIVE -> ENDED
endReason = RESULT_REPLACED

+

Share #11
Result #456
ACTIVE
```

原因：

> Consent 针对具体 Result；历史 consent 不应该被覆盖。

---

## 28. Stop Sharing

Command：

```text
StopSharingAssessmentResult
```

只有 Share owner 可以执行。

结果：

```text
ACTIVE -> ENDED
endReason = STOPPED_BY_MEMBER
endedAt = now
```

停止 Sharing 不影响：

- Membership
- Group
- FinalAssessmentResult

---

## 29. Membership End and Sharing

如果 Sharing owner 的 Membership 结束：

```text
Membership
ACTIVE -> ENDED
```

其 Active Share 必须同步终止：

```text
Share
ACTIVE -> ENDED
endReason = MEMBERSHIP_ENDED
```

如果用户之后重新加入：

```text
new Membership
```

旧 Share 不恢复。

用户必须重新表达 consent。

---

## 30. Viewer Access Rule

假设 Alice 分享 Result，Bob 希望查看。

Bob 可以查看的前提：

```text
Group == ACTIVE

Alice has ACTIVE Membership

Bob has ACTIVE Membership

Alice has ACTIVE Share
for this specific completed AssessmentSession result
in this Group
```

`canViewResult` 是 derived authorization，不是持久化 Boolean。

Bob 如果 Leave：

```text
Bob loses access immediately
```

但 Alice 的 Share 不受影响。

---

## 31. Historical Assessment Deletion

用户可以删除自己的 Assessment history。

如果某个：

```text
AssessmentSession
```

正在被 Group Share 引用，则删除该 historical Assessment 时必须：

```text
end all ACTIVE Shares referencing that AssessmentSession

endReason = ASSESSMENT_DELETED
```

Group 不能继续保留一个有效 authorization 指向已删除 Result。

具体数据库删除策略：

- physical delete
- soft delete
- historical identifier retention

留到 Database Design 阶段决定。

Domain Rule 只要求：

> 删除完成后，不存在任何 ACTIVE Share 可以访问该 historical Assessment 的 finalized result。

---

## 32. Assessment ↔ Group Boundary

正式 Architecture Rule：

```text
Assessment Domain
owns AssessmentSession
and its FinalAssessmentResult.

Group Domain
never owns, modifies,
or copies AssessmentSession
or FinalAssessmentResult.

Group Domain only owns
the authorization / consent fact
that a specific Membership shares
the finalized result of a specific
COMPLETED AssessmentSession
with its Group.
```

因此 Group 只长期引用：

```text
AssessmentSessionId
```

该 ID 指向 Assessment Aggregate Root，而不是为 `FinalAssessmentResult` 人为增加独立 identity。

Group 不保存：

```text
personalityType
dimensionScores
questionnaireAnswers
clarificationData
AI conversation
```

---

## 33. Cross-Domain Result Query

Group Member 查看共享结果时，Application Layer 可以：

```text
1. Load Group / viewer Membership
2. Validate Group access
3. Find owner's ACTIVE Share
4. Ask Assessment Domain for the shareable result of the referenced COMPLETED AssessmentSession
5. Assessment Domain validates that the Session still exists and exposes the permitted finalized-result representation
6. Compose query DTO
```

Query DTO 可以跨 Domain 组合数据。

这不改变 ownership：

```text
Assessment owns result
Group owns visibility
```

---

## 34. Shared Assessment Stability

Sharing model 要求：

> A referenced `AssessmentSession` must be `COMPLETED`, and its `FinalAssessmentResult` is immutable except for deletion / lifecycle removal semantics.

否则用户同意分享：

```text
Result #123
```

之后 #123 的内容如果还能任意变化，就会绕过 consent。

因此 Group Domain 假设：

```text
FinalAssessmentResult
= stable finalized evidence
```

---

## 35. Aggregate Boundary

MVP 正式采用：

```text
Group Domain
│
├── Group                  Aggregate Root
│
├── GroupMembership        Aggregate Root
│
├── JoinRequest            Aggregate Root
│
└── GroupAssessmentShare   Aggregate Root
```

不采用：

```text
Group
├── List<GroupMembership>
├── List<JoinRequest>
└── List<GroupAssessmentShare>
```

---

## 36. Why Membership / Request / Share Are Separate Aggregates

### 36.1 Unbounded History

Group 生命周期内可能存在：

```text
100 active members
500 historical memberships
1000 join requests
many historical shares
```

如果全部塞入 Group Aggregate：

```text
load Group
=
load large historical object graph
```

不合理。

### 36.2 Most Commands Need Only Partial State

例如：

```text
LeaveGroup
```

主要关心：

```text
Group status
current admin
target ACTIVE Membership
```

不需要加载全部 Membership history。

### 36.3 Disband Is a Batch Use Case

```text
DisbandGroup
↓
Group transition
+
N Membership transitions
+
M JoinRequest transitions
+
K Share transitions
```

不应为了一个批量 use case，把整个历史 collection 强行变成一个巨大 Aggregate。

---

## 37. Cross-Aggregate Orchestration

例如：

```text
ApproveJoinRequest
```

涉及：

```text
Group
JoinRequest
GroupMembership
```

由 Application Service 协调。

概念流程：

```text
ApproveJoinRequestUseCase

1. load JoinRequest
2. load Group
3. verify current Admin
4. verify Group ACTIVE
5. verify Request PENDING and non-expired
6. verify no ACTIVE Membership
7. approve Request
8. create Membership
9. persist changes
```

当前项目是：

```text
Spring Boot Modular Monolith
+
single PostgreSQL database
```

因此合理使用：

```text
@Transactional
```

完成 local atomic consistency。

MVP 不为此引入：

- Kafka
- Saga
- Outbox
- distributed transaction
- microservices

---

## 38. Concurrency Safety

Application 层：

```text
check -> write
```

本身不足以防止 race condition。

例如两个并发请求都可能看到：

```text
no ACTIVE Membership exists
```

然后同时创建。

因此关键 invariant 需要：

```text
Domain/Application validation
+
Database constraint
```

双层保护。

实现阶段还应评估：

```text
@Transactional
optimistic locking (@Version)
select / row locking where necessary
PostgreSQL constraints
```

特别需要验证的并发场景：

```text
TransferAdmin
vs
new Admin LeaveGroup

TransferAdmin
vs
DisbandGroup

ApproveJoinRequest
vs
ExpireJoinRequest

ApproveJoinRequest
vs
RejectJoinRequest

RequestToJoinGroup
vs
RequestToJoinGroup

ShareAssessmentResult
vs
LeaveGroup

ShareAssessmentResult
vs
DisbandGroup

ShareAssessmentResult
vs
DeleteHistoricalAssessment

ChangeSharedAssessmentResult
vs
StopSharingAssessmentResult
```

系统绝不能最终产生：

```text
Group.adminUserId = Bob
Bob Membership = ENDED
```

---

## 39. Database-Level Uniqueness Candidates

Database Design 阶段至少应落实：

```text
at most one ACTIVE Membership
for (group_id, user_id)
```

以及：

```text
at most one PENDING JoinRequest
for (group_id, user_id)
```

以及：

```text
at most one ACTIVE Share
for membership_id
```

PostgreSQL 可考虑 partial unique index，例如概念上：

```sql
UNIQUE (group_id, user_id)
WHERE status = 'ACTIVE'
```

```sql
UNIQUE (group_id, user_id)
WHERE status = 'PENDING'
```

```sql
UNIQUE (membership_id)
WHERE status = 'ACTIVE'
```

最终 SQL 在 Database Design 阶段决定。

---

## 40. Commands

MVP 用户业务 Commands：

```text
CreateGroup

RequestToJoinGroup
ApproveJoinRequest
RejectJoinRequest

TransferAdmin
LeaveGroup
DisbandGroup

ShareAssessmentResult
ChangeSharedAssessmentResult
StopSharingAssessmentResult
```

系统 lifecycle operation：

```text
ExpirePendingJoinRequests
```

`ExpirePendingJoinRequests` 不是必须暴露给 Frontend 的 API Command。

---

## 41. Queries

MVP 主要 Queries：

```text
GetGroup
GetMyGroups

GetGroupMembers

GetPendingJoinRequests
GetMyJoinRequests

GetGroupSharedResults
GetMemberSharedResult
```

具体 REST endpoint naming 在 API Design 阶段决定。

Domain Specification 不绑定 REST URI。

---

## 42. Persisted Facts

### 42.1 Group

建议持久保存：

```text
groupId
name
groupCode
createdByUserId
adminUserId
status
createdAt
disbandedAt
```

### 42.2 GroupMembership

建议持久保存：

```text
membershipId
groupId
userId
status
joinedAt
endedAt
endReason
```

### 42.3 JoinRequest

建议持久保存：

```text
joinRequestId
groupId
userId
status
requestedAt
expiresAt
resolvedAt
resolvedBy
rejectionReason
```

### 42.4 GroupAssessmentShare

建议持久保存：

```text
shareId
membershipId
assessmentSessionId
status
sharedAt
endedAt
endReason
```

`GroupId` 不作为 `GroupAssessmentShare` 的独立 Domain fact 重复保存，因为 `MembershipId` 已唯一确定所属 Group。若 Database Design 为查询效率决定冗余保存 `group_id`，必须用 constraint / application validation 保证其与 Membership 一致，并把它视为 persistence denormalization，而不是第二个 source of truth。

---

## 43. Derived Facts — Do Not Persist by Default

当前不需要额外保存：

```text
isCreator
```

因为：

```text
userId == createdByUserId
```

可以推导。

不需要 Membership：

```text
role = ADMIN
```

因为：

```text
userId == adminUserId
```

可以推导。

不需要：

```text
canViewResult
```

因为它是 authorization policy 的计算结果。

不需要：

```text
latestAssessmentResultId
```

因为 Group 不拥有 Assessment history。

不需要：

```text
nextAllowedJoinRequestAt
```

因为可以从：

```text
latest rejection resolvedAt + 8 hours
```

计算。

Group 绝不能复制：

```text
personalityType
scores
answers
AI clarification content
```

---

## 44. Core Invariants

为了避免把不同 consistency boundary 的规则混为一谈，本节将 invariant 分为：

```text
Aggregate-local
Cross-Aggregate
Cross-Domain
```

### 44.1 Aggregate-Local Invariants

#### Group

**G-L1**  
`DISBANDED` is terminal.

**G-L2**  
Creator is immutable historical information and grants no permanent authority.

**G-L3**  
Only the current Admin may initiate Admin transfer or Disband the Group.

**G-L4**  
`GroupCode` is immutable after creation and does not itself grant Membership or access.

#### GroupMembership

**M-L1**  
Membership lifecycle is `ACTIVE -> ENDED`.

**M-L2**  
Historical Memberships are never reactivated.

**M-L3**  
An ended Membership retains its end reason.

#### JoinRequest

**J-L1**  
JoinRequest lifecycle is `PENDING -> APPROVED | REJECTED`.

**J-L2**  
`APPROVED` and `REJECTED` are terminal.

**J-L3**  
A pending request expires at its business `expiresAt`; actual scheduler execution time does not redefine the expiration instant.

#### GroupAssessmentShare

**S-L1**  
Share lifecycle is `ACTIVE -> ENDED`.

**S-L2**  
An ended Share retains the referenced AssessmentSession and end reason as historical consent evidence.

### 44.2 Cross-Aggregate Invariants inside Group Domain

**G-X1**  
An `ACTIVE` Group has exactly one current Admin reference.

**G-X2**  
The current Admin of an `ACTIVE` Group must have an `ACTIVE` Membership in the same Group.

**G-X3**  
Admin authority may only be transferred to another `ACTIVE` Member.

**G-X4**  
The current Admin cannot Leave the Group.

**M-X1**  
For one `(GroupId, UserId)`, at most one `ACTIVE` Membership may exist.

**M-X2**  
Rejoining creates a new Membership rather than reactivating history.

**J-X1**  
An `ACTIVE` Member cannot submit a JoinRequest for the same Group.

**J-X2**  
For one `(GroupId, UserId)`, at most one `PENDING` JoinRequest may exist.

**J-X3**  
Only the current Admin may manually approve / reject a pending request.

**J-X4**  
Approval creates a new `ACTIVE` Membership.

**J-X5**  
Disbanding rejects all pending requests and ends all active memberships.

**S-X1**  
Only an `ACTIVE` Membership may own an `ACTIVE` Share.

**S-X2**  
One Membership may have at most one `ACTIVE` Share.

**S-X3**  
Only the sharing Member may start, replace, or voluntarily stop their Share.

**S-X4**  
Ending the owner Membership ends its `ACTIVE` Share.

**S-X5**  
A viewer must have an `ACTIVE` Membership in the same `ACTIVE` Group.

**S-X6**  
Disbanding ends all `ACTIVE` Shares.

### 44.3 Cross-Domain Invariants — Group ↔ Assessment

**S-D1**  
A Share references one specific `AssessmentSessionId`, not an independently identified `FinalAssessmentResult`.

**S-D2**  
The referenced AssessmentSession must belong to the sharing Member.

**S-D3**  
The referenced AssessmentSession must be `COMPLETED`.

**S-D4**  
The referenced AssessmentSession must currently expose a valid `FinalAssessmentResult`.

**S-D5**  
Creating a newer Assessment result does not alter an existing Share.

**S-D6**  
Deleting the referenced historical Assessment ends all `ACTIVE` Shares referencing that AssessmentSession.

**S-D7**  
Assessment Domain owns the AssessmentSession and FinalAssessmentResult; Group Domain owns only consent / visibility.

### 44.4 Privacy and Uniqueness Invariants

**P1**  
Membership is not consent. Joining a Group never automatically shares Assessment data.

**P2**  
Admin and Creator have no privacy override.

**P3**  
The same completed AssessmentSession may be independently shared through different Memberships in different Groups.

**P4**  
Key uniqueness invariants (`ACTIVE` Membership, `PENDING` JoinRequest, `ACTIVE` Share) require both Application validation and Database-level protection.

---

## 45. Important Transactional Use Cases

The following use cases cross Aggregate boundaries and should be treated carefully:

### Create Group

```text
Create Group
+
Create creator Membership
```

must succeed or fail together.

### Approve JoinRequest

```text
Approve JoinRequest
+
Create Membership
```

must succeed or fail together.

### Leave Group

```text
End Membership
+
End owner's Active Share
```

must remain consistent.

### Disband Group

```text
Disband Group
+
End Active Memberships
+
Reject Pending Requests
+
End Active Shares
```

must leave no residual access rights.

### Delete Historical Assessment

```text
End Active Shares referencing AssessmentSession
+
Delete / retire historical AssessmentSession
```

must leave no active visibility relation.

The exact Repository / transaction implementation is an Application Design concern, not part of this Domain Specification.

---

## 46. Security / Privacy Principles

### Principle 1 — Membership Is Not Consent

```text
Join Group
!=
Share Assessment Result
```

### Principle 2 — Minimum Sharing

Group receives only what is necessary to display the explicitly shared finalized result.

### Principle 3 — No Admin Privacy Override

Admin authority controls Group management, not private Assessment ownership.

### Principle 4 — Consent Is Contextual

Consent applies to:

```text
specific Membership
+
specific Group
+
specific completed AssessmentSession result
```

### Principle 5 — Revocation Must Work

User can stop sharing without:

- leaving Group
- deleting AssessmentResult
- asking Admin

### Principle 6 — Lifecycle Removes Access

Leave / Disband / Result deletion must automatically invalidate relevant access.

---

## 47. Future Scope

Explicitly deferred from MVP:

```text
Email Invitation
Invite Token
Invite Expiration

Multiple Admins
Admin Transfer Acceptance Workflow

Kick Member
Blacklist
Temporary Ban

Group Avatar

Group Chat
Group Feed
Notifications

Complex RBAC
Organization hierarchy

Restore Disbanded Group

Share Multiple Results per Membership
Share Full Assessment History
Share AI Conversation
```

Future features should extend this model only when concrete product requirements exist.

---

## 48. Implementation Guidance

This specification defines Domain rules, not Java class shape.

Do not assume that every conceptual field must become:

```text
public setter
```

Domain implementation should prefer methods that represent business transitions, for example:

```text
group.transferAdmin(...)
group.disband(...)

membership.leave(...)
membership.endBecauseGroupDisbanded(...)

joinRequest.approve(...)
joinRequest.reject(...)
joinRequest.expire(...)

share.stop(...)
share.replace(...)
share.endBecauseMembershipEnded(...)
```

Avoid implementing lifecycle changes through arbitrary state mutation.

---

## 49. Testing Guidance

At minimum, Domain / Application tests should cover:

### Group

```text
creator becomes Admin + Member
non-admin cannot disband
disband is terminal
```

### Admin

```text
admin transfers to active member
cannot transfer to non-member
current admin cannot leave
old admin can leave after transfer
```

### Membership

```text
duplicate active membership rejected
left membership is not reactivated
rejoin creates new membership
```

### JoinRequest

```text
active member cannot apply
duplicate pending request rejected
admin approve creates membership
non-admin cannot approve
rejected user blocked for 8 hours
request expires after 7 days
```

### Sharing

```text
membership does not auto-share
member shares owned COMPLETED AssessmentSession result
cannot share another user's AssessmentSession
one active share per membership
new assessment does not replace share
replace share ends old share
stop share preserves membership
leave ends owner share
viewer leave removes viewer access
disband ends all shares
historical Assessment deletion ends related shares
```

### Concurrency / Persistence

Integration tests should verify DB constraints prevent:

```text
duplicate ACTIVE Membership
duplicate PENDING JoinRequest
duplicate ACTIVE Share
```

even under competing writes.

Cross-domain application tests should also verify:

```text
cannot share IN_PROGRESS AssessmentSession
cannot share AWAITING_CLARIFICATION AssessmentSession
cannot share CLARIFICATION_IN_PROGRESS AssessmentSession
cannot share ABANDONED AssessmentSession
can share only owned COMPLETED AssessmentSession with FinalAssessmentResult
```

---

## 50. Architecture Summary

Final Group Domain model:

```text
                         User
                          │
             ┌────────────┴─────────────┐
             │                          │
             ▼                          ▼
           Group                   Assessment Domain
             │                          │
             │                    AssessmentSession
             │                    + FinalAssessmentResult
             │                          ▲
             ▼                          │
      GroupMembership                   │
             │                          │
             └── GroupAssessmentShare ──┘

      JoinRequest
          │
          └── intent to establish GroupMembership
```

Core responsibility boundary:

> **Assessment Domain owns the AssessmentSession and what its finalized result is.  
> Group Domain owns who may see that finalized result in a Group context.**

Aggregate boundary:

```text
Group
GroupMembership
JoinRequest
GroupAssessmentShare
```

are separate Aggregate Roots inside the same Group Domain module.

Cross-aggregate consistency is coordinated by the Application Layer and, in the MVP Modular Monolith, may use local PostgreSQL transactions.

---

## 51. Final MVP Decisions

1. `Group` lifecycle is `ACTIVE -> DISBANDED`; disband is irreversible.
2. Creator is immutable historical information, not a permanent role.
3. Creator automatically becomes the first Admin and first Active Member.
4. `GroupCode` is Backend-generated, globally unique, immutable, and only acts as the JoinRequest discovery identifier.
5. MVP has exactly one Admin per Active Group.
6. Admin authority is stored on Group and can be transferred to another Active Member.
7. Admin transfer does not require recipient approval in MVP.
8. Current Admin cannot Leave; they must transfer Admin or Disband the Group.
9. Membership lifecycle is `ACTIVE -> ENDED`.
10. Membership end reason distinguishes `LEFT` from `GROUP_DISBANDED`.
11. Rejoining creates a new Membership; historical Membership is never reactivated.
12. User may belong to multiple Groups.
13. A User may have at most one Active Membership per Group.
14. Join intent is modeled separately as `JoinRequest`.
15. JoinRequest lifecycle is `PENDING -> APPROVED | REJECTED`.
16. Only one Pending JoinRequest may exist per User per Group.
17. Rejected requests may be retried after 8 hours.
18. Pending requests expire after 7 days.
19. JoinRequest timeout is a Domain Rule; scheduling mechanism is Infrastructure / Application concern.
20. Disband rejects pending requests and ends active memberships.
21. Joining a Group does not automatically share Assessment data.
22. Group sharing targets one explicitly selected `COMPLETED AssessmentSession` and exposes only its finalized result.
23. Consent is per Membership + per completed AssessmentSession.
24. One Membership may have at most one Active Share.
25. A new Assessment Result never silently replaces an existing Share.
26. Replacing a shared Result ends the old Share and creates a new Share.
27. Leave / Disband / historical Assessment deletion terminates relevant active Shares.
28. Admin / Creator do not have privacy override.
29. Assessment Domain owns AssessmentSession / FinalAssessmentResult; Group Domain owns visibility / consent.
30. `Group`, `GroupMembership`, `JoinRequest`, and `GroupAssessmentShare` are separate Aggregate Roots.
31. Application Layer coordinates cross-Aggregate use cases.
32. Local PostgreSQL transactions are acceptable for MVP consistency.
33. Key uniqueness invariants should be protected by both Application validation and Database constraints.
34. Historical Membership, JoinRequest, and Share facts are retained rather than overwritten.
35. Group name is immutable in MVP; `RenameGroup` is outside MVP.
36. Email Invitation, Multiple Admins, Kick / Blacklist, complex RBAC, and multi-result sharing are outside MVP.


---

## 51.1 Remaining Non-Blocking Product Decision

### Group Name Mutability — Frozen for MVP

MVP adopts:

```text
Group name is immutable after Group creation.
```

There is no `RenameGroup` command/API in MVP. A future rename capability may be added only when a concrete product requirement exists.

### TIMED_OUT JoinRequest Cooldown

当前 baseline 仍保持：

```text
ADMIN_REJECTED -> 8h cooldown
TIMED_OUT      -> 8h cooldown
```

该规则是可实现且自洽的。

如果后续认为“Admin 未处理导致超时”不应继续惩罚申请者，可以把 `TIMED_OUT` 调整为立即允许重新申请。该调整属于 Product Rule change，不影响当前 Aggregate Boundary。

---

## 52. Downstream Design Work

This specification should be treated as the baseline for:

```text
Backend Package / Layer Boundary
↓
Application Use Case Mapping
↓
Java Domain Model / Aggregate Mapping
↓
PostgreSQL Schema Design
↓
Repository / JPA Mapping
↓
Transaction / Concurrency / Idempotency Design
↓
REST API Contract
↓
Spring Implementation
↓
Frontend Group Flow
↓
Automated Tests
```

Important design changes should update this specification or be recorded as an ADR when they affect architecture-level trade-offs.
