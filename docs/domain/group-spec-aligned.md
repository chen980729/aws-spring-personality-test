# Group Domain Specification

> **File:** `docs/domain/group-spec-aligned.md`  
> **Status:** MVP Baseline / Accepted — design-frozen, implementation pending  
> **Last updated:** 2026-10-06  
> **Scope:** Group lifecycle, membership, join requests, admin authority, result sharing consent, aggregate boundaries, invariants, commands, queries, and persistence facts.

---

## 1. Purpose

Group is the major Supporting Business Domain planned for the MVP.

Its purpose is not to build a complex social network or enterprise authorization system. It provides a realistic collaboration domain for demonstrating:

- Spring Boot domain modeling;
- Entity lifecycle modeling;
- authorization;
- privacy and consent;
- cross-Aggregate orchestration;
- transactional consistency;
- PostgreSQL uniqueness constraints;
- concurrency reasoning;
- bounded responsibility between Assessment and Group;
- automated testing and API contract design.

The MVP prioritizes clear semantics for Group, Membership, JoinRequest, and Sharing; testable lifecycle transitions; separation of Admin authority from Creator history; explicit sharing of completed Assessment results only; retained relationship/consent history; bounded Aggregate size; and implementation complexity appropriate for the portfolio.

---

## 2. MVP Product Boundary

The intended flow is:

~~~text
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
Member optionally shares the finalized result
of one specific COMPLETED AssessmentSession
~~~

MVP supports:

- Create Group;
- join discovery by Group Code;
- JoinRequest approval/rejection;
- member listing;
- Admin transfer;
- leaving and disbanding;
- explicitly sharing one completed Assessment result;
- replacing/stopping the shared result;
- viewing results explicitly shared inside the Group.

MVP excludes email invitations, Group avatars, multiple Admins, kick/blacklist, chat/feed, complex RBAC, organization hierarchy, and restoring a disbanded Group.

Earlier drafts treated Admin transfer as future scope and discussed automatically sharing the latest result. This accepted specification supersedes those drafts: **single-Admin transfer is MVP, and sharing is explicitly bound to one specific completed AssessmentSession result.**

---

## 3. Ubiquitous Language

### 3.1 Group

A long-lived collaboration/sharing space that owns identity, name, GroupCode, lifecycle, creator history, and current Admin authority. It does not own User, Assessment results, or unbounded Membership/JoinRequest history collections.

### 3.2 Creator

The User who created the Group. Creator is immutable historical information represented by createdByUserId, not a permanent Role or authority.

### 3.3 Admin

The single current Active Member with Group-management authority. For an Active Group, Group.adminUserId identifies the Admin and can change through transfer.

### 3.4 GroupMembership

The business fact that a User is or was a member of a Group. Join intent is not Membership.

### 3.5 JoinRequest

A User’s intent to establish GroupMembership.

~~~text
JoinRequest = intent
GroupMembership = established relationship
~~~

### 3.6 GroupAssessmentShare

The consent/visibility fact that an Active Member explicitly shares the finalized result of one specific completed AssessmentSession with one Group. Group stores only the AssessmentSession reference; Assessment remains authoritative for ownership, completion, and the finalized result.

---

## 4. Core Domain Objects

The Group Domain uses four separate Aggregate Roots:

~~~text
Group
GroupMembership
JoinRequest
GroupAssessmentShare
~~~

Explicit identifiers include GroupId, MembershipId, JoinRequestId, ShareId, UserId, AssessmentSessionId, and GroupCode.

---

## 5. Group Lifecycle

GroupStatus:

~~~text
ACTIVE -> DISBANDED
~~~

DISBANDED is terminal. SUSPENDED/ARCHIVED/PAUSED are outside current requirements.

An ACTIVE Group has exactly one Admin, and that Admin must have an ACTIVE Membership in the same Group.

After disbanding:

- no new JoinRequest may be created;
- pending requests cannot be manually approved/rejected;
- Admin transfer and LeaveGroup are unavailable;
- no share may be created/replaced;
- all Active Memberships end;
- all Pending JoinRequests are rejected;
- all Active Shares end.

The last adminUserId may be retained as historical information.

---

## 6. Group Creation

CreateGroup atomically creates:

~~~text
Group
- status = ACTIVE
- createdByUserId = creator
- adminUserId = creator

GroupMembership
- userId = creator
- status = ACTIVE
~~~

An Active Group may never exist without an Active Admin Membership. Group creation and creator Membership creation therefore share one local database transaction.

### 6.1 Group Code

GroupCode is a Backend-generated, globally unique, immutable discovery identifier allocated at creation.

~~~text
knowing GroupCode
!= being a Member
!= having access to shared results
~~~

Used codes are not reassigned. A disbanded Group no longer accepts requests through its code. Exact format/collision strategy belongs to Database/Application design.

---

## 7. Creator / Admin / Member Semantics

~~~text
Creator = immutable historical fact
Admin   = mutable current authority
Member  = active membership relationship
~~~

A creator may transfer Admin and later leave. Creator status gives no permanent Admin right, permanent Membership, privacy override, or special result visibility.

---

## 8. Admin Model

The MVP has exactly one Admin. Admin authority lives on Group.adminUserId rather than a MembershipRole column. This avoids inconsistent multiple-Admin Membership rows.

A role model may be introduced later only if multiple Admins become a real requirement.

---

## 9. Admin Transfer

TransferAdmin requires:

~~~text
Group.status == ACTIVE
currentAdminId == Group.adminUserId
newAdminId != currentAdminId
newAdminId has ACTIVE Membership in this Group
~~~

The command sets Group.adminUserId = newAdminId.

Recipient confirmation is intentionally excluded from MVP. The trade-off is accepted because the new Admin can transfer again or disband when appropriate.

---

## 10. GroupMembership Lifecycle

MembershipStatus:

~~~text
ACTIVE -> ENDED
~~~

End reasons:

~~~text
LEFT
GROUP_DISBANDED
~~~

Historical Memberships are never reactivated. Rejoining creates a new Membership row/identity and keeps prior Membership history intact.

---

## 11. Membership Uniqueness

A User may belong to multiple Groups and may have multiple historical Memberships for the same Group, but for one (GroupId, UserId):

~~~text
at most one ACTIVE Membership
~~~

---

## 12. Leave Group

LeaveGroup requires an Active Group, an Active Membership for the user, and userId != Group.adminUserId.

The Membership transitions to ENDED with endReason = LEFT and endedAt = now.

The current Admin cannot leave directly. They must first TransferAdmin or DisbandGroup. If the Admin is the only Member, DisbandGroup prevents a business deadlock.

---

## 13. JoinRequest Lifecycle

JoinRequestStatus:

~~~text
PENDING -> APPROVED
        -> REJECTED
~~~

APPROVED and REJECTED are terminal.

Rejected requests distinguish:

~~~text
ADMIN_REJECTED
TIMED_OUT
GROUP_DISBANDED
~~~

All terminal transitions record resolvedAt. Manual Admin resolution records resolvedBy; system timeout/disband resolution may leave resolvedBy empty. rejectionReason exists only for REJECTED.

---

## 14. Request to Join

RequestToJoinGroup requires:

~~~text
Group.status == ACTIVE
no ACTIVE Membership
no PENDING JoinRequest
rejection cooldown elapsed
~~~

A successful request is PENDING with requestedAt = now and expiresAt = requestedAt + 7 days.

For one (GroupId, UserId), at most one PENDING request may exist, while historical requests remain retained.

---

## 15. Rejection Cooldown

ADMIN_REJECTED and TIMED_OUT requests impose an eight-hour cooldown:

~~~text
nextAllowedRequestAt = resolvedAt + 8 hours
~~~

GROUP_DISBANDED has no retry path because the Group is terminal. Retry count is otherwise unlimited in MVP.

nextAllowedRequestAt is derived from history and does not need separate persistence.

---

## 16. JoinRequest Expiration

A Pending request expires seven days after requestedAt.

~~~text
PENDING -> REJECTED
rejectionReason = TIMED_OUT
resolvedAt = expiresAt
~~~

resolvedAt represents the business expiration instant, not the later time when a scheduled/lazy process happens to perform the transition.

The seven-day rule is Domain policy; scheduler/lazy/background implementation is an Application/Infrastructure concern.

---

## 17. Approve JoinRequest

Approval revalidates:

~~~text
Group ACTIVE
actor is current Admin
request PENDING
request not expired
applicant has no ACTIVE Membership
~~~

It atomically transitions the request to APPROVED and creates a **new** ACTIVE Membership. Historical Membership is never reactivated.

---

## 18. Reject JoinRequest

Only the current Admin of an Active Group may reject a Pending request.

The request becomes REJECTED with ADMIN_REJECTED, resolvedAt = now, and resolvedBy = adminUserId, then enters the eight-hour cooldown.

---

## 19. Disband Group

Only the current Admin may DisbandGroup.

Disbanding atomically produces:

~~~text
Group ACTIVE -> DISBANDED

all ACTIVE Memberships
-> ENDED / GROUP_DISBANDED

all PENDING JoinRequests
-> REJECTED / GROUP_DISBANDED

all ACTIVE GroupAssessmentShares
-> ENDED / GROUP_DISBANDED
~~~

Group.disbandedAt is recorded and the Group cannot be restored in MVP.

---

## 20. Assessment Sharing Principle

Group follows **Minimum Sharing + Explicit Consent**.

Membership never automatically shares Assessment data. Admin/Creator cannot force or override consent.

Group does not receive complete Assessment history, InitialAssessmentResult, ClarificationResult, AI conversations, or questionnaire answers. MVP exposes only the finalized result of one explicitly selected completed AssessmentSession.

---

## 21. Why Share a Specific Result

The system does not automatically share “latest result”.

If Result #123 is shared and the user later creates Result #456, the existing Share remains bound to #123. Changing to #456 requires explicit ChangeSharedAssessmentResult.

This prevents visible data from changing without renewed consent.

---

## 22. Sharing Consent Scope

Consent is scoped per Membership + specific completed AssessmentSession, not globally per User/account/result.

The same User may independently share different results to different Groups or share nothing in another Group.

---

## 23. GroupAssessmentShare

GroupAssessmentShare is a separate Aggregate Root with conceptual fields:

~~~text
ShareId
MembershipId
AssessmentSessionId
ShareStatus
sharedAt
endedAt?
endReason?
~~~

It stores consent/visibility, not copied Assessment content.

---

## 24. Share Lifecycle

~~~text
ShareStatus
ACTIVE -> ENDED
~~~

Possible end reasons:

~~~text
STOPPED_BY_MEMBER
RESULT_REPLACED
MEMBERSHIP_ENDED
GROUP_DISBANDED
ASSESSMENT_DELETED
~~~

Share history is retained.

---

## 25. Share Invariants

One Active Membership may own zero or one Active Share.

A single completed AssessmentSession may be independently shared through Memberships in multiple Groups; each Share is a separate consent fact.

MVP does not support sharing multiple historical results simultaneously through one Membership.

---

## 26. Start Sharing

ShareAssessmentResult requires:

~~~text
Group ACTIVE
Membership ACTIVE
requesting user owns Membership
AssessmentSession belongs to same user
AssessmentSession.status == COMPLETED
FinalAssessmentResult exists
Membership has no ACTIVE Share
~~~

Only the Membership owner may create the Share; Admin cannot share on another member’s behalf.

AssessmentSessionId is a cross-module reference. The Group Domain cannot decide whether the Session is shareable. The Application use case asks the Assessment module to validate existence, ownership, COMPLETED state, and FinalAssessmentResult.

IN_PROGRESS, AWAITING_CLARIFICATION, CLARIFICATION_IN_PROGRESS, ABANDONED, nonexistent, or non-finalized Sessions cannot be shared.

A narrow boundary representation such as ShareableAssessmentResultRef may be used without introducing a new Assessment Domain Entity.

---

## 27. Change Shared Result

Replacing Result #123 with #456 does not mutate the existing Share’s result reference.

Instead:

~~~text
old Share ACTIVE -> ENDED / RESULT_REPLACED
+
new Share for Result #456 -> ACTIVE
~~~

Consent history is therefore preserved.

---

## 28. Stop Sharing

Only the Share owner may stop sharing.

~~~text
ACTIVE -> ENDED
endReason = STOPPED_BY_MEMBER
endedAt = now
~~~

Stopping a Share does not change Membership, Group, or FinalAssessmentResult.

---

## 29. Membership End and Sharing

When the sharing owner’s Membership ends, its Active Share ends with MEMBERSHIP_ENDED.

If the User later rejoins, the new Membership does not reactivate the old Share; consent must be expressed again.

---

## 30. Viewer Access Rule

A viewer may see a member’s shared result only when:

~~~text
Group is ACTIVE
sharing owner has ACTIVE Membership
viewer has ACTIVE Membership
sharing owner has ACTIVE Share
for the selected completed AssessmentSession in this Group
~~~

canViewResult is derived authorization, not persisted state.

A viewer who leaves loses access immediately; the owner’s Share remains unchanged.

---

## 31. Historical Assessment Deletion

Historical Assessment deletion must end every Active Share referencing the deleted AssessmentSession with ASSESSMENT_DELETED.

After deletion completes, no Active Share may continue to authorize access to that finalized result.

Physical delete vs soft delete vs retained identifier is a persistence/deletion-policy decision, not a Group Domain rule.

---

## 32. Assessment ↔ Group Boundary

Architecture rule:

~~~text
Assessment Domain
owns AssessmentSession
and FinalAssessmentResult.

Group Domain
never owns, modifies, or copies them.

Group Domain owns only
the consent/visibility fact
that one Membership shares
one specific completed AssessmentSession result.
~~~

Group retains only AssessmentSessionId as the cross-domain reference and never copies personality type, dimension scores, questionnaire answers, clarification data, or AI conversation content.

---

## 33. Cross-Domain Result Query

A Group shared-result query may:

~~~text
1. load Group + viewer Membership
2. validate Group access
3. find owner ACTIVE Share
4. ask Assessment for the shareable result of the referenced completed Session
5. Assessment validates the Session/result
6. compose a cross-domain query DTO
~~~

A composed read DTO does not change ownership: Assessment owns the result; Group owns visibility.

---

## 34. Shared Assessment Stability

A shared AssessmentSession must be COMPLETED and its FinalAssessmentResult stable/immutable except deletion/lifecycle removal. Otherwise previously granted consent could silently expose changing content.

---

## 35. Aggregate Boundary

The MVP intentionally uses separate Aggregate Roots:

~~~text
Group
GroupMembership
JoinRequest
GroupAssessmentShare
~~~

It does not model an unbounded Group Aggregate containing lists of all Memberships, requests, and shares.

---

## 36. Why Membership / Request / Share Are Separate Aggregates

### 36.1 Unbounded History

Long-lived Groups may accumulate many historical Memberships, JoinRequests, and Shares. Loading that complete history for every Group command would create an unbounded Aggregate/object graph.

### 36.2 Most Commands Need Only Partial State

For example, LeaveGroup mainly needs Group status/current Admin plus the target Active Membership, not all historical relationships.

### 36.3 Disband Is a Batch Use Case

DisbandGroup coordinates one Group transition plus N Membership, M JoinRequest, and K Share transitions. A batch use case does not justify making the entire history one Aggregate.

---

## 37. Cross-Aggregate Orchestration

Cross-Aggregate use cases are coordinated by Application Services.

Example approval flow:

~~~text
1. load JoinRequest
2. load Group
3. verify current Admin
4. verify Group ACTIVE
5. verify Request PENDING and non-expired
6. verify no ACTIVE Membership
7. approve Request
8. create Membership
9. persist atomically
~~~

The MVP is a Spring Boot Modular Monolith with one PostgreSQL database, so local @Transactional consistency is appropriate. Kafka, Saga, Outbox, distributed transactions, and microservice decomposition are not introduced solely for this workflow.

---

## 38. Concurrency Safety

Application “check then write” validation alone cannot prevent competing requests from observing the same precondition.

Critical invariants therefore require both:

~~~text
Domain/Application validation
+
Database-level protection
~~~

Implementation should evaluate transactions, optimistic locking where useful, targeted row locks where necessary, and PostgreSQL constraints.

Important races include Admin transfer vs leave/disband, approve vs expire/reject, duplicate join requests, Share creation vs membership/disband/deletion, and replace vs stop sharing.

The system must never end with an Admin reference pointing to an ended Membership.

---

## 39. Database-Level Uniqueness Candidates

At minimum the persistence design should enforce:

~~~text
at most one ACTIVE Membership for (group_id, user_id)
at most one PENDING JoinRequest for (group_id, user_id)
at most one ACTIVE Share for membership_id
~~~

PostgreSQL partial unique indexes are the preferred candidate. Final SQL belongs to Database Design.

---

## 40. Commands

MVP user commands:

~~~text
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
~~~

System lifecycle operation:

~~~text
ExpirePendingJoinRequests
~~~

Expiration need not be exposed as a Frontend command.

---

## 41. Queries

Main MVP queries:

~~~text
GetGroup
GetMyGroups
GetGroupMembers
GetPendingJoinRequests
GetMyJoinRequests
GetGroupSharedResults
GetMemberSharedResult
~~~

The Domain specification does not bind REST URI naming.

---

## 42. Persisted Facts

### 42.1 Group

~~~text
groupId
name
groupCode
createdByUserId
adminUserId
status
createdAt
disbandedAt
~~~

### 42.2 GroupMembership

~~~text
membershipId
groupId
userId
status
joinedAt
endedAt
endReason
~~~

### 42.3 JoinRequest

~~~text
joinRequestId
groupId
userId
status
requestedAt
expiresAt
resolvedAt
resolvedBy
rejectionReason
~~~

### 42.4 GroupAssessmentShare

~~~text
shareId
membershipId
assessmentSessionId
status
sharedAt
endedAt
endReason
~~~

GroupId is not a second independent Domain fact on GroupAssessmentShare because MembershipId already determines the Group. A denormalized group_id may be introduced for query efficiency only if constraints/application validation guarantee consistency; it remains persistence optimization rather than a second source of truth.

---

## 43. Derived Facts — Do Not Persist by Default

Do not persist isCreator, Membership role=ADMIN, canViewResult, latestAssessmentResultId, or nextAllowedJoinRequestAt merely because they are convenient.

They are derived respectively from createdByUserId, adminUserId, authorization policy, Assessment ownership/history, or latest rejection resolvedAt + eight hours.

Group must never copy personalityType, scores, answers, or AI clarification content.

---

## 44. Core Invariants

To keep consistency boundaries explicit, invariants are grouped as Aggregate-local, cross-Aggregate, and cross-Domain rules.

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

~~~text
Group name is immutable after Group creation.
~~~

There is no RenameGroup command/API in MVP. A future rename capability may be added only when a concrete product requirement exists.

### TIMED_OUT JoinRequest Cooldown

The frozen MVP baseline remains:

~~~text
ADMIN_REJECTED -> 8h cooldown
TIMED_OUT      -> 8h cooldown
~~~

This rule is implementable and internally consistent.

If later product feedback concludes that an applicant should not be penalized for an Admin’s failure to process a request, TIMED_OUT may be changed to allow immediate re-application. That would be a Product Rule change and would not affect the current Aggregate boundaries.

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
