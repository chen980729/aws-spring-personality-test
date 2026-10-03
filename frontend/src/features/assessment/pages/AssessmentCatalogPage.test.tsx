import { screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'

import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { AssessmentCatalogPage } from './AssessmentCatalogPage'

const catalogEndpoint =
  'http://localhost:5173/api/v1/assessments'

describe('AssessmentCatalogPage', () => {
  it('renders available assessments from the backend catalog', async () => {
    server.use(
      http.get(catalogEndpoint, () =>
        HttpResponse.json({
          items: [
            {
              code: 'SIXTEEN_PERSONALITY',
              name: 'Sixteen Personality Assessment',
              availableVersion: 'v1',
              questionCount: 48,
            },
          ],
        }),
      ),
    )

    renderWithProviders(
      <AssessmentCatalogPage />,
      {
        initialEntries: ['/assessments'],
      },
    )

    expect(
      screen.getByText('Loading assessments...'),
    ).toBeInTheDocument()

    const link = await screen.findByRole(
      'link',
      {
        name: 'Sixteen Personality Assessment',
      },
    )

    expect(link).toHaveAttribute(
      'href',
      '/assessments/SIXTEEN_PERSONALITY',
    )

    expect(
      screen.getByText('Version v1 · 48 questions'),
    ).toBeInTheDocument()
  })

  it('shows a recoverable error when the catalog request fails', async () => {
    server.use(
      http.get(catalogEndpoint, () =>
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

    renderWithProviders(
      <AssessmentCatalogPage />,
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Unable to load assessments right now.',
    )
  })
})
