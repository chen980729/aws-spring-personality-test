import type { AssessmentSession } from '../api/assessmentTypes'
import { buildAssessmentResultReadModel } from './assessmentResultReadModel'

interface AssessmentResultStageProps {
  session: AssessmentSession
}

export function AssessmentResultStage({
  session,
}: AssessmentResultStageProps) {
  const result =
    buildAssessmentResultReadModel(session)

  if (!result) {
    return (
      <section>
        <h2>Assessment complete</h2>

        <p role="alert">
          The assessment is complete, but the final result
          details could not be resolved from the current
          session response.
        </p>
      </section>
    )
  }

  return (
    <section aria-labelledby="assessment-result-heading">
      <h2 id="assessment-result-heading">
        Assessment complete
      </h2>

      <p>
        Final type:{' '}
        <strong>
          {result.finalType}
        </strong>
      </p>

      <section aria-labelledby="dimension-decisions-heading">
        <h3 id="dimension-decisions-heading">
          Dimension decisions
        </h3>

        {result.dimensions.length === 0 ? (
          <p>
            No dimension decision details were returned.
          </p>
        ) : (
          result.dimensions.map(
            (dimension) => (
              <article
                key={
                  dimension.dimensionCode
                }
                aria-labelledby={`result-${dimension.dimensionCode}`}
              >
                <h4
                  id={`result-${dimension.dimensionCode}`}
                >
                  {dimension.dimensionCode}
                </h4>

                <dl>
                  <div>
                    <dt>
                      Questionnaire preference
                    </dt>
                    <dd>
                      {dimension.questionnairePreference ??
                        'Exact tie — no preference'}
                    </dd>
                  </div>

                  <div>
                    <dt>Final preference</dt>
                    <dd>
                      {dimension.finalPreference}
                    </dd>
                  </div>

                  <div>
                    <dt>Decision source</dt>
                    <dd>
                      {dimension.sourceLabel}
                    </dd>
                  </div>
                </dl>

                <p>
                  {
                    dimension.sourceDescription
                  }
                </p>

                <p>
                  {
                    dimension.baselineRelationLabel
                  }
                </p>

                <p>
                  Questionnaire evidence:{' '}
                  {dimension.evidence.poleA}{' '}
                  {dimension.evidence.poleAPercentage.toFixed(
                    1,
                  )}
                  %
                  {' · '}
                  {dimension.evidence.poleB}{' '}
                  {dimension.evidence.poleBPercentage.toFixed(
                    1,
                  )}
                  %
                </p>
              </article>
            ),
          )
        )}
      </section>
    </section>
  )
}
