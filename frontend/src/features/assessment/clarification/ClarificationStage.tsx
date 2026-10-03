import { useState } from 'react'

import { isApiError } from '../../../shared/api/apiError'
import {
  useSkipClarificationMutation,
  useSkipRemainingClarificationsMutation,
} from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import type { ClarificationMode } from '../workflow/assessmentWorkflowResolver'
import { buildClarificationReadModel } from './clarificationReadModel'

interface ClarificationStageProps {
  session: AssessmentSession
  dimensionCode: string
  mode: ClarificationMode
}

type ConfirmationAction =
  | 'skip-one'
  | 'skip-remaining'
  | null

function stageHeading(
  mode: ClarificationMode,
): string {
  switch (mode) {
    case 'pending':
      return 'Clarification required'

    case 'in-progress':
      return 'Clarification in progress'

    case 'retryable':
      return 'Clarification needs retry'
  }
}

function stageDescription(
  mode: ClarificationMode,
): string {
  switch (mode) {
    case 'pending':
      return 'This dimension is ready for clarification.'

    case 'in-progress':
      return 'This dimension currently has an active clarification execution.'

    case 'retryable':
      return 'A previous clarification attempt ended in a retryable technical state.'
  }
}

function skipErrorMessage(
  error: unknown,
): string {
  if (isApiError(error)) {
    if (
      error.code === 'CLARIFICATION_NOT_ALLOWED'
    ) {
      return 'This clarification can no longer be skipped in the current assessment state. Refresh the session before trying another action.'
    }

    if (
      error.code ===
      'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION'
    ) {
      return 'This assessment changed in another request or tab. Refresh the session before trying again.'
    }
  }

  return 'Unable to update the clarification workflow right now.'
}

export function ClarificationStage({
  session,
  dimensionCode,
  mode,
}: ClarificationStageProps) {
  const [
    confirmationAction,
    setConfirmationAction,
  ] = useState<ConfirmationAction>(null)

  const skipOneMutation =
    useSkipClarificationMutation(
      session.id,
    )

  const skipRemainingMutation =
    useSkipRemainingClarificationsMutation(
      session.id,
    )

  const readModel =
    buildClarificationReadModel(
      session,
      dimensionCode,
    )

  if (!readModel) {
    return (
      <section>
        <h2>Clarification</h2>

        <p role="alert">
          Unable to resolve clarification details from the
          current assessment state.
        </p>
      </section>
    )
  }

  const { evidence } = readModel

  const mutationPending =
    skipOneMutation.isPending ||
    skipRemainingMutation.isPending

  const mutationError =
    skipOneMutation.error ??
    skipRemainingMutation.error

  function beginConfirmation(
    action: Exclude<
      ConfirmationAction,
      null
    >,
  ) {
    skipOneMutation.reset()
    skipRemainingMutation.reset()
    setConfirmationAction(action)
  }

  function cancelConfirmation() {
    if (mutationPending) {
      return
    }

    setConfirmationAction(null)
  }

  async function handleSkipOne() {
    try {
      await skipOneMutation.mutateAsync(
        dimensionCode,
      )

      setConfirmationAction(null)
    } catch {
      // Error state is rendered below.
    }
  }

  async function handleSkipRemaining() {
    try {
      await skipRemainingMutation.mutateAsync()

      setConfirmationAction(null)
    } catch {
      // Error state is rendered below.
    }
  }

  return (
    <section aria-labelledby="clarification-heading">
      <h2 id="clarification-heading">
        {stageHeading(mode)}
      </h2>

      <p>
        {stageDescription(mode)}
      </p>

      <dl>
        <div>
          <dt>Current dimension</dt>
          <dd>{readModel.dimensionCode}</dd>
        </div>

        <div>
          <dt>Clarification state</dt>
          <dd>{readModel.status}</dd>
        </div>

        <div>
          <dt>Questionnaire preference</dt>
          <dd>
            {readModel.questionnairePreference ??
              'No baseline preference (exact tie)'}
          </dd>
        </div>
      </dl>

      <section aria-labelledby="questionnaire-evidence-heading">
        <h3 id="questionnaire-evidence-heading">
          Questionnaire evidence
        </h3>

        <p>
          {evidence.poleA}:{' '}
          {evidence.poleAPercentage.toFixed(1)}%
          {' · '}
          {evidence.poleB}:{' '}
          {evidence.poleBPercentage.toFixed(1)}%
        </p>
      </section>

      {session.workflow
        .pendingClarificationDimensions
        .length > 0 && (
        <p>
          Pending dimensions:{' '}
          {session.workflow
            .pendingClarificationDimensions
            .join(', ')}
        </p>
      )}

      {session.workflow
        .retryableClarificationDimensions
        .length > 0 && (
        <p>
          Retryable dimensions:{' '}
          {session.workflow
            .retryableClarificationDimensions
            .join(', ')}
        </p>
      )}

      <section aria-labelledby="clarification-actions-heading">
        <h3 id="clarification-actions-heading">
          Clarification actions
        </h3>

        {confirmationAction === null && (
          <>
            <button
              type="button"
              onClick={() =>
                beginConfirmation(
                  'skip-one',
                )
              }
              disabled={mutationPending}
            >
              Skip this dimension
            </button>

            <button
              type="button"
              onClick={() =>
                beginConfirmation(
                  'skip-remaining',
                )
              }
              disabled={mutationPending}
            >
              Skip all remaining clarifications
            </button>
          </>
        )}

        {confirmationAction ===
          'skip-one' && (
          <div>
            <p>
              Skip {dimensionCode}? This clarification will
              become terminal and cannot be resumed.
            </p>

            <button
              type="button"
              onClick={cancelConfirmation}
              disabled={mutationPending}
            >
              Cancel
            </button>

            <button
              type="button"
              onClick={handleSkipOne}
              disabled={mutationPending}
            >
              {skipOneMutation.isPending
                ? 'Skipping...'
                : `Confirm skip ${dimensionCode}`}
            </button>
          </div>
        )}

        {confirmationAction ===
          'skip-remaining' && (
          <div>
            <p>
              Skip every remaining clarification? Pending,
              active, and retryable clarification work will
              be marked as skipped. Exact ties may still
              require a tie-break.
            </p>

            <button
              type="button"
              onClick={cancelConfirmation}
              disabled={mutationPending}
            >
              Cancel
            </button>

            <button
              type="button"
              onClick={handleSkipRemaining}
              disabled={mutationPending}
            >
              {skipRemainingMutation.isPending
                ? 'Skipping remaining...'
                : 'Confirm skip all remaining'}
            </button>
          </div>
        )}

        {mutationError && (
          <p role="alert">
            {skipErrorMessage(
              mutationError,
            )}
          </p>
        )}
      </section>

      <p>
        AI clarification execution will be connected in a
        later frontend checkpoint.
      </p>
    </section>
  )
}
