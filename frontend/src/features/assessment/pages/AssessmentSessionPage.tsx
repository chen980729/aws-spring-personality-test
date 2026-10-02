import { useParams } from 'react-router'

export function AssessmentSessionPage() {
    const { sessionId } = useParams()

    return (
        <main>
            <h1>Assessment Session</h1>
            <p>{sessionId}</p>
        </main>
    )
}