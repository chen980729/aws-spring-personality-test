import type { AssessmentSession } from '../api/assessmentTypes'
import { buildAssessmentResultReadModel } from './assessmentResultReadModel'

interface AssessmentResultStageProps {
  session: AssessmentSession
}

export function AssessmentResultStage({ session }: AssessmentResultStageProps) {
  const result = buildAssessmentResultReadModel(session)

  if (!result) {
    return (
      <section className="assessment-stage">
        <h2>Assessment complete</h2>
        <p className="assessment-inline-alert" role="alert">
          The assessment is complete, but the final result details could not be resolved from the current session response.
        </p>
      </section>
    )
  }

  return (
    <section className="assessment-stage result-stage" aria-labelledby="assessment-result-heading">
      <div className="result-hero">
        <p className="assessment-stage__eyebrow">Stage 4 of 4 · completed</p>
        <h2 id="assessment-result-heading">Assessment complete</h2>
        <div className="result-hero__type" aria-label={`Final type ${result.finalType}`}>
          {result.finalType}
        </div>
        <p>Your result is composed from the finalized preference for each personality dimension.</p>
      </div>

      <section className="result-dimensions" aria-labelledby="dimension-decisions-heading">
        <div className="result-dimensions__heading">
          <div>
            <p className="assessment-stage__eyebrow">Decision trace</p>
            <h3 id="dimension-decisions-heading">Dimension decisions</h3>
          </div>
          <span>{result.dimensions.length} dimensions</span>
        </div>

        {result.dimensions.length === 0 ? (
          <p>No dimension decision details were returned.</p>
        ) : (
          <div className="result-dimension-grid">
            {result.dimensions.map((dimension) => (
              <article
                className="result-dimension-card"
                key={dimension.dimensionCode}
                aria-labelledby={`result-${dimension.dimensionCode}`}
              >
                <div className="result-dimension-card__heading">
                  <h4 id={`result-${dimension.dimensionCode}`}>{dimension.dimensionCode}</h4>
                  <span>{dimension.finalPreference}</span>
                </div>

                <div className="result-evidence" aria-label={`Questionnaire evidence ${dimension.evidence.poleA} ${dimension.evidence.poleAPercentage.toFixed(1)}%, ${dimension.evidence.poleB} ${dimension.evidence.poleBPercentage.toFixed(1)}%`}>
                  <div className="result-evidence__bar" aria-hidden="true">
                    <span style={{ width: `${dimension.evidence.poleAPercentage}%` }} />
                  </div>
                  <p>
                    Questionnaire evidence: {dimension.evidence.poleA} {dimension.evidence.poleAPercentage.toFixed(1)}% · {dimension.evidence.poleB} {dimension.evidence.poleBPercentage.toFixed(1)}%
                  </p>
                </div>

                <dl className="result-dimension-card__data">
                  <div>
                    <dt>Questionnaire preference</dt>
                    <dd>{dimension.questionnairePreference ?? 'Exact tie — no preference'}</dd>
                  </div>
                  <div><dt>Final preference</dt><dd>{dimension.finalPreference}</dd></div>
                  <div><dt>Decision source</dt><dd>{dimension.sourceLabel}</dd></div>
                </dl>

                <p>{dimension.sourceDescription}</p>
                <p className="result-dimension-card__relation">{dimension.baselineRelationLabel}</p>
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  )
}
