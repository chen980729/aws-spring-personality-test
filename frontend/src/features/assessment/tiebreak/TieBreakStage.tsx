import { useState } from 'react'

import { isApiError } from '../../../shared/api/apiError'
import { useSubmitDimensionTieBreakMutation } from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import { buildTieBreakReadModel } from './tieBreakReadModel'

interface TieBreakStageProps {
  session: AssessmentSession
  dimensionCode: string
}

function tieBreakErrorMessage(
  error: unknown,
): string {
  if (isApiError(error)) {
    if (
      error.code === 'TIE_BREAK_NOT_REQUIRED'
    ) {
      return 'This tie-break is no longer required in the current assessment state. Refresh the session before trying another action.'
    }

    if (
      error.code ===
      'INVALID_DIMENSION_TIE_BREAK'
    ) {
      return 'The selected preference is not valid for this dimension.'
    }

    if (
      error.code ===
      'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION'
    ) {
      return 'This assessment changed in another request or tab. Refresh the session before trying again.'
    }
  }

  return 'Unable to save the tie-break decision right now.'
}

export function TieBreakStage({
  session,
  dimensionCode,
}: TieBreakStageProps) {
  const [selectedPole, setSelectedPole] =
    useState<string | null>(null)

  const [confirming, setConfirming] =
    useState(false)

  const readModel =
    buildTieBreakReadModel(
      session,
      dimensionCode,
    )

  const mutation =
    useSubmitDimensionTieBreakMutation(
      session.id,
      dimensionCode,
    )

  if (!readModel) {
    return (
      <section>
        <h2>Tie-break</h2>

        <p role="alert">
          Unable to resolve tie-break details from the
          current assessment state.
        </p>
      </section>
    )
  }

  function choosePole(
    pole: string,
  ) {
    mutation.reset()
    setSelectedPole(pole)
    setConfirming(false)
  }

  function beginConfirmation() {
    if (!selectedPole) {
      return
    }

    mutation.reset()
    setConfirming(true)
  }

  function cancelConfirmation() {
    if (mutation.isPending) {
      return
    }

    setConfirming(false)
  }

  async function confirmTieBreak() {
    if (
      !selectedPole ||
      mutation.isPending
    ) {
      return
    }

    try {
      await mutation.mutateAsync(
        selectedPole,
      )
    } catch {
      // Mutation error is rendered below. Keep the same
      // selection so a safe retry can use the same pole.
    }
  }

  return (
    <section aria-labelledby="tie-break-heading">
      <h2 id="tie-break-heading">
        Tie-break required
      </h2>

      <p>
        The questionnaire and clarification workflow did
        not produce a final preference for dimension{' '}
        <strong>{dimensionCode}</strong>. Choose the
        preference that best represents you.
      </p>

      <p>
        This decision is final for this assessment session.
      </p>

      <fieldset disabled={mutation.isPending}>
        <legend>
          Choose one preference for {dimensionCode}
        </legend>

        {readModel.options.map(
          (option) => {
            const inputId =
              `tie-break-${dimensionCode}-${option.pole}`

            return (
              <div key={option.pole}>
                <input
                  id={inputId}
                  type="radio"
                  name={`tie-break-${dimensionCode}`}
                  value={option.pole}
                  checked={
                    selectedPole ===
                    option.pole
                  }
                  onChange={() =>
                    choosePole(
                      option.pole,
                    )
                  }
                />

                <label htmlFor={inputId}>
                  {option.pole}
                  {' — questionnaire evidence '}
                  {option.questionnairePercentage.toFixed(
                    1,
                  )}
                  %
                </label>
              </div>
            )
          },
        )}
      </fieldset>

      {!confirming && (
        <button
          type="button"
          onClick={beginConfirmation}
          disabled={
            !selectedPole ||
            mutation.isPending
          }
        >
          Review tie-break decision
        </button>
      )}

      {confirming &&
        selectedPole && (
          <div>
            <p>
              Confirm {selectedPole} for{' '}
              {dimensionCode}? After the Backend accepts
              this decision, it cannot be changed.
            </p>

            <button
              type="button"
              onClick={cancelConfirmation}
              disabled={mutation.isPending}
            >
              Cancel
            </button>

            <button
              type="button"
              onClick={confirmTieBreak}
              disabled={mutation.isPending}
            >
              {mutation.isPending
                ? 'Saving decision...'
                : `Confirm ${selectedPole} for ${dimensionCode}`}
            </button>
          </div>
        )}

      {mutation.isError && (
        <p role="alert">
          {tieBreakErrorMessage(
            mutation.error,
          )}
        </p>
      )}
    </section>
  )
}
