# Frontend F7-D — Landing Hero Checkpoint

> **Status:** Implemented and acceptance-tested
> **Date:** 2026-10-06
> **Scope:** Public Landing visual only
> **Assessment Definition status:** DefinitionVersion 1.0 remains AVAILABLE; 1.1 remains DRAFT

## 1. Purpose

F7-D replaces the temporary CSS-generated personality visual on the public Landing page with the final four-profile illustration supplied for the portfolio presentation.

The change is intentionally presentation-only.

It does not alter:

- public routes;
- Register/Login CTA behavior;
- Assessment business rules;
- DefinitionVersion semantics;
- Result content;
- Session/CSRF behavior;
- Backend API contracts.

## 2. Before and after

Before F7-D, the Landing hero used a CSS composition:

```text
glow
+ concentric orbit shapes
+ synthetic profile silhouette
+ E/I, S/N, T/F, J/P floating cards
```

That visual was useful as a temporary design placeholder, but it did not represent the final personality artwork.

F7-D replaces that entire composition with:

```text
Landing copy + CTAs
        │
        └── optimized four-profile illustration
```

The headline, lead copy, CTA links, application highlights and the "From first answer to useful result" feature section remain unchanged.

## 3. Asset ownership

The final Landing hero lives at:

```text
frontend/public/landing-personality-groups.webp
```

Repository asset properties at this checkpoint:

```text
format: WebP
dimensions: 900 × 859
size: 151,036 bytes (~151 KB)
alpha/transparency: preserved
WebP RIFF signature: valid
```

The optimized asset is derived from the supplied four-profile source illustration.

Like the F7-C result illustrations, this is presentation content and is not part of `AssessmentDefinitionVersion`.

## 4. Component boundary

`LandingPage.tsx` now renders one semantic image:

```tsx
<img
  src="/landing-personality-groups.webp"
  width="900"
  height="859"
  loading="eager"
  fetchPriority="high"
  alt="Four illustrated personality profiles representing the assessment experience"
/>
```

The old orbit/profile/dimension-card markup is removed rather than hidden behind CSS.

This keeps the DOM aligned with the final design instead of maintaining two competing visual systems.

## 5. First-screen performance decisions

The Landing illustration is expected to be one of the primary first-screen visual elements and a likely LCP candidate.

F7-D therefore uses:

- optimized WebP instead of the original multi-megabyte PNG;
- fixed intrinsic width and height to reserve layout space;
- `loading="eager"`;
- `fetchPriority="high"`;
- responsive CSS sizing rather than separate desktop/mobile image files.

The image remains a normal static file served by the existing S3 + CloudFront frontend deployment.

No additional image CDN or runtime transformation service is introduced for the current MVP.

## 6. Responsive behavior

Desktop:

```text
copy / CTA column | hero artwork
```

Tablet and smaller screens:

```text
copy / CTA
     ↓
hero artwork centered below
```

The artwork keeps its intrinsic aspect ratio through `object-fit: contain`.

The decorative four-quadrant backdrop is CSS-only and does not introduce semantic content.

## 7. Accessibility

The image has descriptive alt text because it is a meaningful visual representation of the product rather than a purely decorative flourish.

During F7-D regression testing, the Landing `<h1>` exposed an existing accessibility detail:

```text
Understand yourself.
<span>Discover your patterns.</span>
```

Without an explicit whitespace node, the accessible heading name became:

```text
Understand yourself.Discover your patterns.
```

The component was corrected to preserve the visual line treatment while producing the intended accessible name:

```text
Understand yourself. Discover your patterns.
```

The test was not weakened to accommodate accidental markup.

## 8. Automated coverage

F7-D adds a Landing-page component regression test covering:

- main heading;
- Register CTA destination;
- Login CTA destination;
- final hero image alt text;
- stable hero asset path;
- intrinsic 900 × 859 dimensions;
- eager loading;
- high fetch priority;
- continued rendering of the feature-section heading.

The existing F1-F7-C suite remains active.

At the F7-D code checkpoint:

```text
30 test files
102 tests
frontend lint: success
frontend production build: success
backend: success
backend Docker Image: success
```

Final acceptance was verified on the complete F7-D head with the binary asset and documentation present together:

```text
CI run: 37344916764
Frontend: 30 test files / 102 tests
Frontend lint: success
Frontend production build: success
Backend: success
Backend Docker Image: success
Overall CI: success
```

## 9. Acceptance criteria

F7-D is accepted when all of the following are true:

- [x] old CSS-generated profile/orbit/dimension markup removed;
- [x] final four-profile visual referenced by the Landing component;
- [x] optimized WebP stored in `frontend/public`;
- [x] asset path matches the component reference;
- [x] image is valid WebP;
- [x] responsive desktop/mobile layout defined;
- [x] intrinsic dimensions provided;
- [x] first-screen loading priority defined;
- [x] accessible image alternative text provided;
- [x] accessible Landing heading spacing corrected;
- [x] Landing regression test added;
- [x] living frontend/roadmap documentation aligned;
- [x] final feature-branch CI green on the complete F7-D head.

## 10. Remaining frontend work

F7-D does not activate DefinitionVersion 1.1.

The next frontend step is **F7-E — final frontend regression/documentation/release review**.

That checkpoint should review the complete F7-A–F7-D batch, including:

- retained DefinitionVersion 1.0 compatibility;
- staged 1.1 contextual Tie-break flow;
- Result content/images;
- Landing visual;
- final README/portfolio presentation alignment;
- release readiness before DefinitionVersion 1.1 activation.
