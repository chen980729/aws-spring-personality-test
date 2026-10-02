import { screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'

import { renderWithProviders } from '../../test/render'
import { server } from '../../test/msw/server'
import { BackendConnectionStatus } from './BackendConnectionStatus'

const healthEndpoint = 'http://localhost:5173/actuator/health'

describe('BackendConnectionStatus', () => {
  it('shows the backend health status returned by the API', async () => {
    server.use(
      http.get(healthEndpoint, () =>
        HttpResponse.json({
          status: 'UP',
        }),
      ),
    )

    renderWithProviders(<BackendConnectionStatus />)

    expect(
      screen.getByText('Backend connection: checking...'),
    ).toBeInTheDocument()

    expect(
      await screen.findByText('Backend connection: UP'),
    ).toBeInTheDocument()
  })

  it('shows an unavailable state when the backend request fails', async () => {
    server.use(
      http.get(healthEndpoint, () =>
        HttpResponse.json(
          {
            status: 'DOWN',
          },
          {
            status: 503,
          },
        ),
      ),
    )

    renderWithProviders(<BackendConnectionStatus />)

    expect(
      await screen.findByText('Backend connection: unavailable'),
    ).toBeInTheDocument()
  })
})
