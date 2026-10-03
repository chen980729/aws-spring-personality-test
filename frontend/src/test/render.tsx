import type { ReactElement, ReactNode } from 'react'
import {
  QueryClient,
  QueryClientProvider,
  type QueryClientConfig,
} from '@tanstack/react-query'
import {
  render,
  type RenderOptions,
} from '@testing-library/react'
import { MemoryRouter } from 'react-router'

export function createTestQueryClient(
  config?: QueryClientConfig,
): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
    ...config,
  })
}

interface RenderWithProvidersOptions
  extends Omit<RenderOptions, 'wrapper'> {
  queryClient?: QueryClient
  initialEntries?: string[]
}

export function renderWithProviders(
  ui: ReactElement,
  {
    queryClient = createTestQueryClient(),
    initialEntries = ['/'],
    ...renderOptions
  }: RenderWithProvidersOptions = {},
) {
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>
          {children}
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return {
    queryClient,
    ...render(ui, {
      wrapper: Wrapper,
      ...renderOptions,
    }),
  }
}
