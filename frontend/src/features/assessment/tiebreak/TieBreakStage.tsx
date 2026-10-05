import { useState } from 'react'

import { isApiError } from '../../../shared/api/apiError'
import { Button } from '../../../shared/ui'
import {
  useDimensionTieBreakInteractionQuery,
  useSubmitDimensionTieBreakMutation,
} from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import { buildTieBreakReadModel } from './tieBreakReadModel'

interface TieBreakStageProps {
  session: AssessmentSession
  dimensionCode: string
}

function tieBreakMutationErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'TIE_BREAK_NOT_REQUIRED') {
      return 'This tie-break is no longer required in the current assessment state. Refresh the session before trying another action.'
    }

    if (error.code === 'TIE_BREAK_ALREADY_DECIDED') {
      return 'A tie-break decision has already been accepted for this dimension. Refresh the session to continue from the authoritative result.'
    }

    if (error.code === 'INVALID_DIMENSION_TIE_BREAK') {
      return 'This tie-break answer is not valid for the bound assessment version and dimension.'
    }

    if (error.code === 'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION') {
      return 'This assessment changed in another request or tab. Refresh the session before trying again.'
    }
  }

  return 'Unable to save the tie-break decision right now.'
}

function tieBreakInteractionErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'TIE_BREAK_NOT_REQUIRED') {
      return 'This tie-break is no longer required in the current assessment state. Refresh the session to continue.'
    }

    if (error.code === 'TIE_BREAK_INTERACTION_UNAVAILABLE') {
      return 'This tie-break interaction is no longer available for the current assessment state.'
    }
  }

  return 'Unable to load the tie-break interaction right now.'
}

export function TieBreakStage({
  session,
  dimensionCode,
}: TieBreakStageProps) {
  const [selectedValue, setSelectedValue] =
    useState<string | null>(null)
  const [confirming, setConfirming] =
    useState(false)

  const readModel =
    buildTieBreakReadModel(
      session,
      dimensionCode,
    )

  const interactionQuery =
    useDimensionTieBreakInteractionQuery(
      session.id,
      dimensionCode,
    )

  const mutation =
    useSubmitDimensionTieBreakMutation(
      session.id,
      dimensionCode,
    )

  if (!readModel) {
    return (
      <section className="assessment-stage">
        <h2>Tie-break</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          Unable to resolve tie-break details from the current assessment state.
        </p>
      </section>
    )
  }

  if (interactionQuery.isPending) {
    return (
      <section className="assessment-stage tie-break-stage">
        <h2>Tie-break</h2>
        <p>Loading the tie-break interaction...</p>
      </section>
    )
  }

  if (interactionQuery.isError) {
    return (
      <section className="assessment-stage tie-break-stage">
        <h2>Tie-break</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          {tieBreakInteractionErrorMessage(
            interactionQuery.error,
          )}
        </p>
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            void interactionQuery.refetch()
          }}
        >
          Retry loading tie-break
        </Button>
      </section>
    )
  }

  const interaction =
    interactionQuery.data

  if (
    interaction.dimensionCode !==
    dimensionCode
  ) {
    return (
      <section className="assessment-stage">
        <h2>Tie-break</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          The tie-break interaction does not match the current dimension. Refresh the session before continuing.
        </p>
      </section>
    )
  }

  const isContextual =
    interaction.interactionType ===
    'CONTEXTUAL_QUESTION'

  const directOptions =
    interaction.interactionType ===
    'DIRECT_POLE_SELECTION'
      ? interaction.allowedPoles.map(
          (pole) =>
            readModel.options.find(
              (option) =>
                option.pole === pole,
            ),
        )
      : []

  if (
    interaction.interactionType ===
      'DIRECT_POLE_SELECTION' &&
    (
      directOptions.length !== 2 ||
      directOptions.some(
        (option) => !option,
      )
    )
  ) {
    return (
      <section className="assessment-stage">
        <h2>Tie-break</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          The tie-break interaction does not match the questionnaire evidence for this dimension.
        </p>
      </section>
    )
  }

  if (
    isContextual &&
    interaction.options.length !== 2
  ) {
    return (
      <section className="assessment-stage">
        <h2>Tie-break</h2>
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          The contextual tie-break interaction is incomplete. Refresh the session before continuing.
        </p>
      </section>
    )
  }

  const selectedContextualOption =
    isContextual
      ? interaction.options.find(
          (option) =>
            option.optionId ===
            selectedValue,
        )
      : undefined

  function chooseValue(value: string) {
    mutation.reset()
    setSelectedValue(value)
    setConfirming(false)
  }

  function beginConfirmation() {
    if (!selectedValue) return

    mutation.reset()
    setConfirming(true)
  }

  function cancelConfirmation() {
    if (!mutation.isPending) {
      setConfirming(false)
    }
  }

  async function confirmTieBreak() {
    if (
      !selectedValue ||
      mutation.isPending
    ) {
      return
    }

    try {
      if (
        interaction.interactionType ===
        'DIRECT_POLE_SELECTION'
      ) {
        await mutation.mutateAsync({
          selectedPole: selectedValue,
        })
      } else {
        await mutation.mutateAsync({
          questionId:
            interaction.questionId,
          selectedOptionId:
            selectedValue,
        })
      }
    } catch {
      // Keep the accepted local selection for an explicit same-value retry.
    }
  }

  return (
    <section
      className="assessment-stage tie-break-stage"
      aria-labelledby="tie-break-heading"
    >
      <div className="assessment-stage__header">
        <div>
          <p className="assessment-stage__eyebrow">
            Stage 3 of 4 · final resolution
          </p>
          <h2 id="tie-break-heading">
            Tie-break required
          </h2>
          <p>
            {isContextual
              ? 'The earlier assessment steps left this dimension exactly tied. Answer one final context question so the Backend can resolve the remaining preference.'
              : (
                  <>
                    The questionnaire and clarification workflow did not produce a final preference for dimension{' '}
                    <strong>
                      {dimensionCode}
                    </strong>
                    . Choose the preference that best represents you.
                  </>
                )}
          </p>
        </div>

        <span
          className="assessment-stage__dimension-badge"
          data-dimension={dimensionCode}
          aria-label={
            `Dimension ${dimensionCode}`
          }
        />
      </div>

      <div className="tie-break-notice">
        <span aria-hidden="true">!</span>
        <p>
          This decision is final for this assessment session.
        </p>
      </div>

      {interaction.interactionType ===
      'DIRECT_POLE_SELECTION' ? (
        <fieldset
          className="tie-break-options"
          disabled={mutation.isPending}
        >
          <legend>
            Choose one preference for{' '}
            {dimensionCode}
          </legend>

          <div className="tie-break-options__grid">
            {directOptions.map((option) => {
              if (!option) return null

              const inputId =
                `tie-break-${dimensionCode}-${option.pole}`

              const checked =
                selectedValue ===
                option.pole

              return (
                <div
                  className="tie-break-option"
                  key={option.pole}
                >
                  <input
                    id={inputId}
                    type="radio"
                    name={
                      `tie-break-${dimensionCode}`
                    }
                    value={option.pole}
                    checked={checked}
                    onChange={() =>
                      chooseValue(
                        option.pole,
                      )
                    }
                  />

                  <label
                    htmlFor={inputId}
                    data-pole={option.pole}
                  >
                    <span>
                      {option.pole} — questionnaire evidence{' '}
                      {option.questionnairePercentage.toFixed(
                        1,
                      )}
                      %
                    </span>
                  </label>
                </div>
              )
            })}
          </div>
        </fieldset>
      ) : (
        <>
          <div className="tie-break-question">
            <p className="tie-break-question__instruction">
              {interaction.instruction}
            </p>
            <h3>
              {interaction.prompt}
            </h3>
          </div>

          <fieldset
            className="tie-break-options"
            disabled={mutation.isPending}
          >
            <legend>
              Choose the answer that feels more natural most of the time.
            </legend>

            <div className="tie-break-options__grid">
              {interaction.options.map(
                (option) => {
                  const inputId =
                    `tie-break-${dimensionCode}-${option.optionId}`

                  const checked =
                    selectedValue ===
                    option.optionId

                  return (
                    <div
                      className="tie-break-option tie-break-option--contextual"
                      key={option.optionId}
                    >
                      <input
                        id={inputId}
                        type="radio"
                        name={
                          `tie-break-${dimensionCode}`
                        }
                        value={
                          option.optionId
                        }
                        checked={checked}
                        onChange={() =>
                          chooseValue(
                            option.optionId,
                          )
                        }
                      />

                      <label htmlFor={inputId}>
                        <span className="tie-break-option__text">
                          {option.text}
                        </span>
                      </label>
                    </div>
                  )
                },
              )}
            </div>
          </fieldset>
        </>
      )}

      {!confirming && (
        <Button
          type="button"
          onClick={beginConfirmation}
          disabled={
            !selectedValue ||
            mutation.isPending
          }
        >
          Review tie-break decision
        </Button>
      )}

      {confirming &&
        selectedValue && (
          <div className="assessment-confirmation tie-break-confirmation">
            {interaction.interactionType ===
            'DIRECT_POLE_SELECTION' ? (
              <p>
                Confirm {selectedValue} for{' '}
                {dimensionCode}? After the Backend accepts this decision, it cannot be changed.
              </p>
            ) : (
              <>
                <p>
                  Confirm this answer? After the Backend accepts this decision, it cannot be changed.
                </p>
                <p className="tie-break-confirmation__selection">
                  {selectedContextualOption?.text}
                </p>
              </>
            )}

            <div className="assessment-confirmation__actions">
              <Button
                type="button"
                variant="ghost"
                onClick={
                  cancelConfirmation
                }
                disabled={
                  mutation.isPending
                }
              >
                Cancel
              </Button>

              <Button
                type="button"
                onClick={confirmTieBreak}
                isLoading={
                  mutation.isPending
                }
                loadingLabel="Saving decision..."
              >
                {interaction.interactionType ===
                'DIRECT_POLE_SELECTION'
                  ? `Confirm ${selectedValue} for ${dimensionCode}`
                  : 'Confirm answer'}
              </Button>
            </div>
          </div>
        )}

      {mutation.isError && (
        <p
          className="assessment-inline-alert"
          role="alert"
        >
          {tieBreakMutationErrorMessage(
            mutation.error,
          )}
        </p>
      )}
    </section>
  )
}
