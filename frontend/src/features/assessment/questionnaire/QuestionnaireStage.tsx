import {
  useMemo,
  useState,
} from 'react'

import { isApiError } from '../../../shared/api/apiError'
import {
  useSessionQuestionnaireQuery,
  useSubmitQuestionnaireMutation,
} from '../api/assessmentQueries'
import type {
  PublicQuestion,
  SessionQuestionnaire,
} from '../api/assessmentTypes'
import { useQuestionnaireAutosave } from './useQuestionnaireAutosave'
import { useQuestionnaireDraft } from './useQuestionnaireDraft'

interface QuestionnaireFormProps {
  sessionId: string
  questionnaire: SessionQuestionnaire
}

function buildAnswerSnapshot(
  questions: PublicQuestion[],
  answersByQuestionId: Record<string, number>,
) {
  return questions.flatMap(
    (question) => {
      const value =
        answersByQuestionId[
          question.questionId
        ]

      if (value === undefined) {
        return []
      }

      return [
        {
          questionId: question.questionId,
          value,
        },
      ]
    },
  )
}

function getSubmissionErrorMessage(
  error: unknown,
): string {
  if (isApiError(error)) {
    if (
      error.code === 'QUESTIONNAIRE_INCOMPLETE'
    ) {
      return 'Answer every question before submitting.'
    }

    if (
      error.code ===
      'INVALID_QUESTIONNAIRE_RESPONSE'
    ) {
      return 'One or more answers are no longer valid for this assessment. Refresh the page before trying again.'
    }

    if (
      error.code ===
      'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION'
    ) {
      return 'This assessment changed in another request or tab. Refresh the page before trying again.'
    }
  }

  return 'Unable to submit the questionnaire right now. Your answers remain on this page.'
}

function QuestionnaireForm({
  sessionId,
  questionnaire,
}: QuestionnaireFormProps) {
  const [preparingSubmission, setPreparingSubmission] =
    useState(false)

  const {
    answersByQuestionId,
    revision,
    setAnswer,
  } = useQuestionnaireDraft(
    questionnaire.response.answers,
  )

  const questions =
    questionnaire.questionnaire.questions

  const answerScale =
    questionnaire.questionnaire.answerScale

  const answerSnapshot = useMemo(
    () =>
      buildAnswerSnapshot(
        questions,
        answersByQuestionId,
      ),
    [
      answersByQuestionId,
      questions,
    ],
  )

  const autosave =
    useQuestionnaireAutosave({
      sessionId,
      answers: answerSnapshot,
      revision,
    })

  const submitMutation =
    useSubmitQuestionnaireMutation(
      sessionId,
    )

  const answeredCount =
    answerSnapshot.length

  const questionnaireComplete =
    answeredCount === questions.length

  const submissionInProgress =
    preparingSubmission ||
    submitMutation.isPending

  async function handleSubmit() {
    if (
      !questionnaireComplete ||
      submissionInProgress
    ) {
      return
    }

    setPreparingSubmission(true)

    await autosave.prepareForSubmit()

    try {
      await submitMutation.mutateAsync(
        answerSnapshot,
      )
    } catch {
      autosave.resumeAfterSubmitFailure()
    } finally {
      setPreparingSubmission(false)
    }
  }

  return (
    <section aria-labelledby="questionnaire-heading">
      <h2 id="questionnaire-heading">
        Questionnaire
      </h2>

      <p>
        Answered {answeredCount} of {questions.length}
      </p>

      {!submissionInProgress &&
        autosave.status === 'saved' && (
          <p role="status">
            Saved
          </p>
        )}

      {!submissionInProgress &&
        autosave.status === 'pending' && (
          <p role="status">
            Unsaved changes
          </p>
        )}

      {!submissionInProgress &&
        autosave.status === 'saving' && (
          <p role="status">
            Saving...
          </p>
        )}

      {!submissionInProgress &&
        autosave.status === 'error' && (
          <div>
            <p role="alert">
              Autosave failed. Your current answers are
              still on this page.
            </p>

            <button
              type="button"
              onClick={autosave.retry}
            >
              Retry autosave
            </button>
          </div>
        )}

      {submissionInProgress && (
        <p role="status">
          Submitting questionnaire...
        </p>
      )}

      <form>
        {questions.map((question) => (
          <fieldset
            key={question.questionId}
            disabled={submissionInProgress}
          >
            <legend>
              {question.position}. {question.prompt}
            </legend>

            {answerScale.map((option) => {
              const inputId =
                `${question.questionId}-${option.value}`

              return (
                <div key={option.value}>
                  <input
                    id={inputId}
                    type="radio"
                    name={question.questionId}
                    value={option.value}
                    checked={
                      answersByQuestionId[
                        question.questionId
                      ] === option.value
                    }
                    onChange={() =>
                      setAnswer(
                        question.questionId,
                        option.value,
                      )
                    }
                  />

                  <label htmlFor={inputId}>
                    {option.label}
                  </label>
                </div>
              )
            })}
          </fieldset>
        ))}
      </form>

      <section aria-labelledby="questionnaire-submit-heading">
        <h3 id="questionnaire-submit-heading">
          Submit questionnaire
        </h3>

        <p>
          Submission is final. Your answers cannot be
          changed after the questionnaire is submitted.
        </p>

        {!questionnaireComplete && (
          <p>
            Answer every question before submitting.
          </p>
        )}

        <button
          type="button"
          onClick={handleSubmit}
          disabled={
            !questionnaireComplete ||
            submissionInProgress
          }
        >
          {submissionInProgress
            ? 'Submitting...'
            : 'Submit questionnaire'}
        </button>

        {submitMutation.isError && (
          <p role="alert">
            {getSubmissionErrorMessage(
              submitMutation.error,
            )}
          </p>
        )}
      </section>
    </section>
  )
}

interface QuestionnaireStageProps {
  sessionId: string
}

export function QuestionnaireStage({
  sessionId,
}: QuestionnaireStageProps) {
  const questionnaireQuery =
    useSessionQuestionnaireQuery(sessionId)

  if (questionnaireQuery.isPending) {
    return <p>Loading questionnaire...</p>
  }

  if (questionnaireQuery.isError) {
    return (
      <p role="alert">
        Unable to load the questionnaire.
      </p>
    )
  }

  return (
    <QuestionnaireForm
      key={sessionId}
      sessionId={sessionId}
      questionnaire={questionnaireQuery.data}
    />
  )
}
