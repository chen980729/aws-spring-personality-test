import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  HttpResponse,
  http,
} from 'msw'
import {
  describe,
  expect,
  it,
} from 'vitest'
import {
  Route,
  Routes,
  useLocation,
} from 'react-router'

import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { AssessmentHistoryPage } from './AssessmentHistoryPage'
import { AssessmentSessionPage } from './AssessmentSessionPage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

function LocationProbe() {
  const location = useLocation()

  return (
    <p data-testid="location">
      {location.pathname}
      {location.search}
    </p>
  )
}

function completedSession() {
  return {
    id: sessionId,
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version: '1.0',
    },
    status: 'COMPLETED',
    questionnaire: {
      answers: [],
      submitted: true,
      submittedAt:
        '2026-10-03T06:00:00Z',
    },
    initialResult: {
      dimensions: [
        {
          dimensionCode: 'EI',
          rawScore: 0,
          questionnairePreference: null,
          ambiguous: true,
          evidence: {
            poleA: 'E',
            poleAPercentage: 50,
            poleB: 'I',
            poleBPercentage: 50,
          },
        },
      ],
    },
    clarifications: [
      {
        dimensionCode: 'EI',
        status: 'SKIPPED',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ],
    tieBreaks: [
      {
        dimensionCode: 'EI',
        selectedPole: 'I',
        decidedAt:
          '2026-10-03T07:00:00Z',
      },
    ],
    finalResult: {
      finalType: 'INTJ',
      dimensions: [
        {
          dimensionCode: 'EI',
          questionnairePreference: null,
          finalPreference: 'I',
          source: 'USER_TIE_BREAK',
          overrodeBaseline: false,
        },
      ],
    },
    workflow: {
      pendingClarificationDimensions: [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [],
      completed: true,
    },
    createdAt:
      '2026-10-03T05:00:00Z',
    completedAt:
      '2026-10-03T07:00:00Z',
    abandonedAt: null,
  }
}

describe('assessment history navigation', () => {
  it('opens a history record on the canonical session route and returns to the same history page', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions`,
        ({ request }) => {
          const url = new URL(
            request.url,
          )

          expect(
            url.searchParams.get('page'),
          ).toBe('2')

          return HttpResponse.json({
            items: [
              {
                sessionId,
                assessmentCode:
                  'SIXTEEN_PERSONALITY',
                assessmentVersion:
                  '1.0',
                finalType: 'INTJ',
                completedAt:
                  '2026-10-03T07:00:00Z',
              },
            ],
            page: 2,
            size: 20,
            totalElements: 21,
            totalPages: 2,
          })
        },
      ),
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            completedSession(),
          ),
      ),
    )

    renderWithProviders(
      <>
        <Routes>
          <Route
            path="/history"
            element={
              <AssessmentHistoryPage />
            }
          />
          <Route
            path="/assessment-sessions/:sessionId"
            element={
              <AssessmentSessionPage />
            }
          />
        </Routes>

        <LocationProbe />
      </>,
      {
        initialEntries: [
          '/history?page=2',
        ],
      },
    )

    await user.click(
      await screen.findByRole('link', {
        name: 'View result',
      }),
    )

    expect(
      screen.getByTestId('location'),
    ).toHaveTextContent(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('INTJ'),
    ).toBeInTheDocument()

    const backLink =
      screen.getByRole('link', {
        name: '← Back to history',
      })

    expect(backLink).toHaveAttribute(
      'href',
      '/history?page=2',
    )

    await user.click(backLink)

    expect(
      screen.getByTestId('location'),
    ).toHaveTextContent(
      '/history?page=2',
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment history',
      }),
    ).toBeInTheDocument()
  })

  it('falls back to the assessment detail link for a direct session visit', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            completedSession(),
          ),
      ),
    )

    renderWithProviders(
      <Routes>
        <Route
          path="/assessment-sessions/:sessionId"
          element={
            <AssessmentSessionPage />
          }
        />
      </Routes>,
      {
        initialEntries: [
          `/assessment-sessions/${sessionId}`,
        ],
      },
    )

    const backLink =
      await screen.findByRole('link', {
        name: '← Back to assessment',
      })

    expect(backLink).toHaveAttribute(
      'href',
      '/assessments/SIXTEEN_PERSONALITY',
    )
  })
})
