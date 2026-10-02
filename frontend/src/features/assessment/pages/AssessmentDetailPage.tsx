import { useParams } from 'react-router'

export function AssessmentDetailPage() {
    const { assessmentCode } = useParams()

    return (
        <main>
            <h1>Assessment</h1>
            <p>{assessmentCode}</p>
        </main>
    )
}