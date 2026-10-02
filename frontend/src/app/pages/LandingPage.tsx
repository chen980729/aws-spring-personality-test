import { BackendConnectionStatus } from '../components/BackendConnectionStatus'

export function LandingPage() {
  return (
    <main>
      <h1>Spring AWS Portfolio</h1>
      <p>Full-stack personality assessment application.</p>

      {import.meta.env.DEV && <BackendConnectionStatus />}
    </main>
  )
}
