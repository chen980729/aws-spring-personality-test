import { useState } from 'react'

import { Button } from '../../../shared/ui'
import { isApiError } from '../../../shared/api/apiError'
import { useSubmitDimensionTieBreakMutation } from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import { buildTieBreakReadModel } from './tieBreakReadModel'

interface TieBreakStageProps {
  session: AssessmentSession
  dimensionCode: string
}

function tieBreakErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'TIE_BREAK_NOT_REQUIRED') {
      return 'This tie-break is no longer required in the current assessment state. Refresh the session before trying another action.'
    }
    if (error.code === 'INVALID_DIMENSION_TIE_BREAK') {
      return 'The selected preference is not valid for this dimension.'
    }
    if (error.code === 'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION') {
      return 'This assessment changed in another request or tab. Refresh the session before trying again.'
    }
  }
  return 'Unable to save the tie-break decision right now.'
}

export function TieBreakStage({ session, dimensionCode }: TieBreakStageProps) {
  const [selectedPole, setSelectedPole] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const readModel = buildTieBreakReadModel(session, dimensionCode)
  const mutation = useSubmitDimensionTieBreakMutation(session.id, dimensionCode)

  if (!readModel) {
    return (
      <section className="assessment-stage">
        <h2>Tie-break</h2>
        <p className="assessment-inline-alert" role="alert">
          Unable to resolve tie-break details from the current assessment state.
        </p>
      </section>
    )
  }

  function choosePole(pole: string) {
    mutation.reset()
    setSelectedPole(pole)
    setConfirming(false)
  }

  function beginConfirmation() {
    if (!selectedPole) return
    mutation.reset()
    setConfirming(true)
  }

  function cancelConfirmation() {
    if (!mutation.isPending) setConfirming(false)
  }

  async function confirmTieBreak() {
    if (!selectedPole || mutation.isPending) return
    try {
      await mutation.mutateAsync(selectedPole)
    } catch {
      // Keep selection for a safe retry.
    }
  }

  return (
    <section className="assessment-stage tie-break-stage" aria-labelledby="tie-break-heading">
      <div className="assessment-stage__header">
        <div>
          <p className="assessment-stage__eyebrow">Stage 3 of 4 · final resolution</p>
          <h2 id="tie-break-heading">Tie-break required</h2>
          <p>
            The questionnaire and clarification workflow did not produce a final preference for dimension <strong>{dimensionCode}</strong>. Choose the preference that best represents you.
          </p>
        </div>
        <span className="assessment-stage__dimension-badge" data-dimension={dimensionCode} aria-label={`Dimension ${dimensionCode}`} />
      </div>

      <div className="tie-break-notice">
        <span aria-hidden="true">!</span>
        <p>This decision is final for this assessment session.</p>
      </div>

      <fieldset className="tie-break-options" disabled={mutation.isPending}>
        <legend>Choose one preference for {dimensionCode}</legend>
        <div className="tie-break-options__grid">
          {readModel.options.map((option) => {
            const inputId = `tie-break-${dimensionCode}-${option.pole}`
            const checked = selectedPole === option.pole
            return (
              <div className="tie-break-option" key={option.pole}>
                <input
                  id={inputId}
                  type="radio"
                  name={`tie-break-${dimensionCode}`}
                  value={option.pole}
                  checked={checked}
                  onChange={() => choosePole(option.pole)}
                />
                <label htmlFor={inputId} data-pole={option.pole}>
                  <span>{option.pole} — questionnaire evidence {option.questionnairePercentage.toFixed(1)}%</span>
                </label>
              </div>
            )
          })}
        </div>
      </fieldset>

      {!confirming && (
        <Button
          type="button"
          onClick={beginConfirmation}
          disabled={!selectedPole || mutation.isPending}
        >
          Review tie-break decision
        </Button>
      )}

      {confirming && selectedPole && (
        <div className="assessment-confirmation tie-break-confirmation">
          <p>
            Confirm {selectedPole} for {dimensionCode}? After the Backend accepts this decision, it cannot be changed.
          </p>
          <div className="assessment-confirmation__actions">
            <Button type="button" variant="ghost" onClick={cancelConfirmation} disabled={mutation.isPending}>Cancel</Button>
            <Button
              type="button"
              onClick={confirmTieBreak}
              isLoading={mutation.isPending}
              loadingLabel="Saving decision..."
            >
              Confirm {selectedPole} for {dimensionCode}
            </Button>
          </div>
        </div>
      )}

      {mutation.isError && (
        <p className="assessment-inline-alert" role="alert">{tieBreakErrorMessage(mutation.error)}</p>
      )}
    </section>
  )
}
