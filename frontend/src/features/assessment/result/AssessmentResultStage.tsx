import type { AssessmentSession } from '../api/assessmentTypes'
import {
  getPersonalityLetterDescriptions,
  getPersonalityTypeDescription,
} from '../content/personalityContent'
import { getPersonalityImagePath } from '../content/personalityImages'
import { buildAssessmentResultReadModel } from './assessmentResultReadModel'

interface AssessmentResultStageProps {
  session: AssessmentSession
}

export function AssessmentResultStage({
  session,
}: AssessmentResultStageProps) {
  const result =
    buildAssessmentResultReadModel(
      session,
    )

  if (!result) {
    return (
      <section className="assessment-stage">
        <h2>Assessment complete</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          The assessment is complete, but the final result details could not be resolved from the current session response.
        </p>
      </section>
    )
  }

  const typeDescription =
    getPersonalityTypeDescription(
      result.finalType,
    )

  if (!typeDescription) {
    return (
      <section className="assessment-stage">
        <h2>Assessment complete</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          The final type was returned, but its explanatory content is not available in this frontend content version.
        </p>
      </section>
    )
  }

  const letterDescriptions =
    getPersonalityLetterDescriptions(
      typeDescription.typeCode,
    )

  const imagePath =
    getPersonalityImagePath(
      typeDescription.typeCode,
    )

  return (
    <section
      className="assessment-stage result-stage"
      aria-labelledby="assessment-result-heading"
    >
      <div className="result-hero">
        <div className="result-hero__copy">
          <p className="assessment-stage__eyebrow">
            Stage 4 of 4 · completed
          </p>
          <h2 id="assessment-result-heading">
            Assessment complete
          </h2>

          <div
            className="result-hero__type"
            aria-label={`Final type ${result.finalType}`}
          >
            {result.finalType}
          </div>

          <p className="result-hero__summary">
            {typeDescription.coreTendency}
          </p>
        </div>

        <div className="result-hero__visual">
          <span
            className="result-hero__visual-fallback"
            aria-hidden="true"
          >
            {result.finalType}
          </span>
          <img
            src={imagePath}
            alt={`${result.finalType} personality illustration`}
          />
        </div>
      </div>

      <section
        className="result-interpretation"
        aria-labelledby="result-letters-heading"
      >
        <div className="result-section-heading">
          <div>
            <p className="assessment-stage__eyebrow">
              Four preference dimensions
            </p>
            <h3 id="result-letters-heading">
              What your letters mean
            </h3>
          </div>
          <p>
            These letters describe preference tendencies, not ability levels or fixed rules of behavior.
          </p>
        </div>

        <div className="result-letter-grid">
          {letterDescriptions.map(
            (description) => (
              <article
                className="result-letter-card"
                key={description.letter}
              >
                <div className="result-letter-card__heading">
                  <span aria-hidden="true">
                    {description.letter}
                  </span>
                  <div>
                    <p>
                      {description.letter}
                    </p>
                    <h4>
                      {description.name}
                    </h4>
                  </div>
                </div>

                <p>
                  {description.coreMeaning}
                </p>

                <details className="result-letter-card__details">
                  <summary>
                    More context
                  </summary>

                  <div>
                    <h5>
                      You may tend to
                    </h5>
                    <ul>
                      {description.tendencies.map(
                        (tendency) => (
                          <li key={tendency}>
                            {tendency}
                          </li>
                        ),
                      )}
                    </ul>

                    <h5>
                      This does not mean
                    </h5>
                    <ul>
                      {description.doesNotMean.map(
                        (misconception) => (
                          <li
                            key={
                              misconception
                            }
                          >
                            {misconception}
                          </li>
                        ),
                      )}
                    </ul>
                  </div>
                </details>
              </article>
            ),
          )}
        </div>
      </section>

      <section
        className="result-profile"
        aria-labelledby="result-profile-heading"
      >
        <div className="result-section-heading">
          <div>
            <p className="assessment-stage__eyebrow">
              Combined tendency
            </p>
            <h3 id="result-profile-heading">
              Your {result.finalType} profile
            </h3>
          </div>
          <p>
            The four letters are combined here as a practical interpretation, not a deterministic prediction of your behavior.
          </p>
        </div>

        <div className="result-profile-grid">
          <article className="result-profile-card">
            <h4>Common strengths</h4>
            <ul>
              {typeDescription.strengths.map(
                (strength) => (
                  <li key={strength}>
                    {strength}
                  </li>
                ),
              )}
            </ul>
          </article>

          <article className="result-profile-card result-profile-card--watch">
            <h4>Possible blind spots</h4>
            <ul>
              {typeDescription.blindSpots.map(
                (blindSpot) => (
                  <li key={blindSpot}>
                    {blindSpot}
                  </li>
                ),
              )}
            </ul>
          </article>
        </div>

        <aside className="result-disclaimer">
          <strong>
            How to read this result
          </strong>
          <p>
            A preference closer to 50 / 50 should be interpreted less strongly. This assessment is an original self-reflection tool and is not an official MBTI® assessment or affiliated with third-party personality testing services.
          </p>
        </aside>
      </section>

      <section
        className="result-dimensions"
        aria-labelledby="dimension-decisions-heading"
      >
        <div className="result-dimensions__heading">
          <div>
            <p className="assessment-stage__eyebrow">
              Decision trace
            </p>
            <h3 id="dimension-decisions-heading">
              How this result was decided
            </h3>
          </div>
          <span>
            {result.dimensions.length}{' '}
            dimensions
          </span>
        </div>

        {result.dimensions.length === 0 ? (
          <p>
            No dimension decision details were returned.
          </p>
        ) : (
          <div className="result-dimension-grid">
            {result.dimensions.map(
              (dimension) => (
                <article
                  className="result-dimension-card"
                  key={
                    dimension.dimensionCode
                  }
                  aria-labelledby={`result-${dimension.dimensionCode}`}
                >
                  <div className="result-dimension-card__heading">
                    <h4
                      id={`result-${dimension.dimensionCode}`}
                    >
                      {
                        dimension.dimensionCode
                      }
                    </h4>
                    <span>
                      {
                        dimension.finalPreference
                      }
                    </span>
                  </div>

                  <div
                    className="result-evidence"
                    aria-label={`Questionnaire evidence ${dimension.evidence.poleA} ${dimension.evidence.poleAPercentage.toFixed(1)}%, ${dimension.evidence.poleB} ${dimension.evidence.poleBPercentage.toFixed(1)}%`}
                  >
                    <div
                      className="result-evidence__bar"
                      aria-hidden="true"
                    >
                      <span
                        style={{
                          width: `${dimension.evidence.poleAPercentage}%`,
                        }}
                      />
                    </div>
                    <p>
                      Questionnaire evidence:{' '}
                      {
                        dimension.evidence
                          .poleA
                      }{' '}
                      {dimension.evidence.poleAPercentage.toFixed(
                        1,
                      )}
                      % ·{' '}
                      {
                        dimension.evidence
                          .poleB
                      }{' '}
                      {dimension.evidence.poleBPercentage.toFixed(
                        1,
                      )}
                      %
                    </p>
                  </div>

                  <dl className="result-dimension-card__data">
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
                      <dt>
                        Final preference
                      </dt>
                      <dd>
                        {
                          dimension.finalPreference
                        }
                      </dd>
                    </div>
                    <div>
                      <dt>
                        Decision source
                      </dt>
                      <dd>
                        {
                          dimension.sourceLabel
                        }
                      </dd>
                    </div>
                  </dl>

                  <p>
                    {
                      dimension.sourceDescription
                    }
                  </p>
                  <p className="result-dimension-card__relation">
                    {
                      dimension.baselineRelationLabel
                    }
                  </p>
                </article>
              ),
            )}
          </div>
        )}
      </section>
    </section>
  )
}
