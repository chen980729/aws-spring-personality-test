export const assessmentQueryKeys = {
  all: ['assessments'] as const,

  catalog: () =>
    ['assessments', 'catalog'] as const,

  details: (assessmentCode: string) =>
    [
      'assessments',
      'details',
      assessmentCode,
    ] as const,

  activeSession: (assessmentCode: string) =>
    [
      'assessments',
      assessmentCode,
      'active-session',
    ] as const,

  session: (sessionId: string) =>
    [
      'assessment-session',
      sessionId,
    ] as const,

  questionnaire: (sessionId: string) =>
    [
      'assessment-session',
      sessionId,
      'questionnaire',
    ] as const,

  tieBreakInteraction: (
    sessionId: string,
    dimensionCode: string,
  ) =>
    [
      'assessment-session',
      sessionId,
      'tie-break',
      dimensionCode,
    ] as const,

  historyRoot: () =>
    ['assessment-history'] as const,

  history: (
    page: number,
    size: number,
  ) =>
    [
      'assessment-history',
      page,
      size,
    ] as const,
}
