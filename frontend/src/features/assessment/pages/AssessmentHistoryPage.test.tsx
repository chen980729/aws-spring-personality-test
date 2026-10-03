import { screen } from '@testing-library/react'
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
} from 'react-router'

import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { AssessmentHistoryPage } from './AssessmentHistoryPage'

const origin = 'http://localhost:5173'

const firstSessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

const secondSessionId =
  '632eef76-4d68-4a12-97a9-6e19aa45fb47'

function renderHistoryPage(
  initialEntry = '/history',
) {
  return renderWithProviders(
    <Routes>
      <Route
        path="/history"
        element={<AssessmentHistoryPage />}
      />
    </Routes>,
    {
      initialEntries: [initialEntry],
    },
  )
}

describe('AssessmentHistoryPage', () => {
  it('requests completed history using one-based pagination and links records to the canonical session route', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions`,
        ({ request }) => {
          const url =
            new URL(request.url)

          expect(
            url.searchParams.get('status'),
          ).toBe('COMPLETED')

          expect(
            url.searchParams.get('page'),
          ).toBe('1')

          expect(
            url.searchParams.get('size'),
          ).toBe('20')

          return HttpResponse.json({
            items: [
              {
                sessionId:
                  firstSessionId,
                assessmentCode:
                  'SIXTEEN_PERSONALITY',
                assessmentVersion:
                  '1.0',
                finalType: 'INTJ',
                completedAt:
                  '2026-10-03T12:00:00Z',
              },
              {
                sessionId:
                  secondSessionId,
                assessmentCode:
                  'SIXTEEN_PERSONALITY',
                assessmentVersion:
                  '1.0',
                finalType: 'ENFP',
                completedAt:
                  '2026-10-02T12:00:00Z',
              },
            ],
            page: 1,
            size: 20,
            totalElements: 22,
            totalPages: 2,
          })
        },
      ),
    )

    renderHistoryPage()

    expect(
      await screen.findByRole('heading', {
        name: 'INTJ',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'ENFP',
      }),
    ).toBeInTheDocument()

    const resultLinks =
      screen.getAllByRole('link', {
        name: 'View result',
      })

    expect(resultLinks[0]).toHaveAttribute(
      'href',
      `/assessment-sessions/${firstSessionId}`,
    )

    expect(
      screen.getByRole('link', {
        name: 'Next page',
      }),
    ).toHaveAttribute(
      'href',
      '/history?page=2',
    )
  })

  it('uses the URL page as navigation state and exposes previous navigation', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions`,
        ({ request }) => {
          const url =
            new URL(request.url)

          expect(
            url.searchParams.get('page'),
          ).toBe('2')

          return HttpResponse.json({
            items: [
              {
                sessionId:
                  secondSessionId,
                assessmentCode:
                  'SIXTEEN_PERSONALITY',
                assessmentVersion:
                  '1.0',
                finalType: 'ENFP',
                completedAt:
                  '2026-10-02T12:00:00Z',
              },
            ],
            page: 2,
            size: 20,
            totalElements: 21,
            totalPages: 2,
          })
        },
      ),
    )

    renderHistoryPage(
      '/history?page=2',
    )

    expect(
      await screen.findByText(
        'Page 2 of 2',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('link', {
        name: 'Previous page',
      }),
    ).toHaveAttribute(
      'href',
      '/history',
    )

    expect(
      screen.queryByRole('link', {
        name: 'Next page',
      }),
    ).not.toBeInTheDocument()
  })

  it('falls back to backend page one when the page query parameter is invalid', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions`,
        ({ request }) => {
          const url =
            new URL(request.url)

          expect(
            url.searchParams.get('page'),
          ).toBe('1')

          return HttpResponse.json({
            items: [],
            page: 1,
            size: 20,
            totalElements: 0,
            totalPages: 0,
          })
        },
      ),
    )

    renderHistoryPage(
      '/history?page=invalid',
    )

    expect(
      await screen.findByRole('heading', {
        name: 'No completed assessments yet',
      }),
    ).toBeInTheDocument()
  })

  it('renders an empty-state action when there is no completed history', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions`,
        () =>
          HttpResponse.json({
            items: [],
            page: 1,
            size: 20,
            totalElements: 0,
            totalPages: 0,
          }),
      ),
    )

    renderHistoryPage()

    expect(
      await screen.findByRole('heading', {
        name: 'No completed assessments yet',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('link', {
        name: 'Browse assessments',
      }),
    ).toHaveAttribute(
      'href',
      '/assessments',
    )
  })

  it('shows a recoverable error when completed history cannot be loaded', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions`,
        () =>
          HttpResponse.json(
            {
              status: 500,
            },
            {
              status: 500,
            },
          ),
      ),
    )

    renderHistoryPage()

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Unable to load assessment history.',
    )
  })
})
