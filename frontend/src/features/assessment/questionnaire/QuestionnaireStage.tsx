import { useMemo, useState } from 'react'

import { Button } from '../../../shared/ui'
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
  return questions.flatMap((question) => {
    const value = answersByQuestionId[question.questionId]
    if (value === undefined) return []
    return [{ questionId: question.questionId, value }]
  })
}

function getSubmissionErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'QUESTIONNAIRE_INCOMPLETE') {
      return 'Answer every question before submitting.'
    }
    if (error.code === 'INVALID_QUESTIONNAIRE_RESPONSE') {
      return 'One or more answers are no longer valid for this assessment. Refresh the page before trying again.'
    }
    if (error.code === 'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION') {
      return 'This assessment changed in another request or tab. Refresh the page before trying again.'
    }
  }

  return 'Unable to submit the questionnaire right now. Your answers remain on this page.'
}

function QuestionnaireForm({ sessionId, questionnaire }: QuestionnaireFormProps) {
  const [preparingSubmission, setPreparingSubmission] = useState(false)
  const { answersByQuestionId, revision, setAnswer } = useQuestionnaireDraft(
    questionnaire.response.answers,
  )

  const questions = questionnaire.questionnaire.questions
  const answerScale = questionnaire.questionnaire.answerScale
  const answerSnapshot = useMemo(
    () => buildAnswerSnapshot(questions, answersByQuestionId),
    [answersByQuestionId, questions],
  )

  const autosave = useQuestionnaireAutosave({
    sessionId,
    answers: answerSnapshot,
    revision,
  })
  const submitMutation = useSubmitQuestionnaireMutation(sessionId)
  const answeredCount = answerSnapshot.length
  const questionnaireComplete = answeredCount === questions.length
  const submissionInProgress = preparingSubmission || submitMutation.isPending
  const progress = questions.length === 0 ? 0 : Math.round((answeredCount / questions.length) * 100)

  async function handleSubmit() {
    if (!questionnaireComplete || submissionInProgress) return

    setPreparingSubmission(true)
    await autosave.prepareForSubmit()

    try {
      await submitMutation.mutateAsync(answerSnapshot)
    } catch {
      autosave.resumeAfterSubmitFailure()
    } finally {
      setPreparingSubmission(false)
    }
  }

  return (
    <section className="assessment-stage questionnaire-stage" aria-labelledby="questionnaire-heading">
      <div className="assessment-stage__header">
        <div>
          <p className="assessment-stage__eyebrow">Stage 1 of 4</p>
          <h2 id="questionnaire-heading">Questionnaire</h2>
          <p>Choose the response that best reflects how you usually think or behave.</p>
        </div>

        <div className="questionnaire-progress-summary" aria-label={`${answeredCount} of ${questions.length} questions answered`}>
          <strong>{progress}%</strong>
          <span>complete</span>
        </div>
      </div>

      <div className="questionnaire-progress">
        <div className="questionnaire-progress__copy">
          <p>Answered {answeredCount} of {questions.length}</p>

          {!submissionInProgress && autosave.status === 'saved' && (
            <p className="questionnaire-save-status questionnaire-save-status--saved" role="status">Saved</p>
          )}
          {!submissionInProgress && autosave.status === 'pending' && (
            <p className="questionnaire-save-status" role="status">Unsaved changes</p>
          )}
          {!submissionInProgress && autosave.status === 'saving' && (
            <p className="questionnaire-save-status" role="status">Saving...</p>
          )}
          {submissionInProgress && (
            <p className="questionnaire-save-status" role="status">Submitting questionnaire...</p>
          )}
        </div>
        <div className="questionnaire-progress__track" aria-hidden="true">
          <span style={{ width: `${progress}%` }} />
        </div>
      </div>

      {!submissionInProgress && autosave.status === 'error' && (
        <div className="assessment-inline-alert assessment-inline-alert--with-action">
          <p role="alert">Autosave failed. Your current answers are still on this page.</p>
          <Button type="button" variant="secondary" size="sm" onClick={autosave.retry}>
            Retry autosave
          </Button>
        </div>
      )}

      <form className="questionnaire-form">
        {questions.map((question) => (
          <fieldset
            className="questionnaire-question"
            key={question.questionId}
            disabled={submissionInProgress}
          >
            <legend>
              <span className="questionnaire-question__number">{question.position}</span>
              <span>{question.position}. {question.prompt}</span>
            </legend>

            <div className="questionnaire-options">
              {answerScale.map((option) => {
                const inputId = `${question.questionId}-${option.value}`
                const checked = answersByQuestionId[question.questionId] === option.value

                return (
                  <div className="questionnaire-option" key={option.value}>
                    <input
                      id={inputId}
                      type="radio"
                      name={question.questionId}
                      value={option.value}
                      checked={checked}
                      onChange={() => setAnswer(question.questionId, option.value)}
                    />
                    <label htmlFor={inputId}>
                      <span className="questionnaire-option__marker" aria-hidden="true" />
                      <span>{option.label}</span>
                    </label>
                  </div>
                )
              })}
            </div>
          </fieldset>
        ))}
      </form>

      <section className="questionnaire-submit" aria-labelledby="questionnaire-submit-heading">
        <div>
          <p className="assessment-stage__eyebrow">Final step for questionnaire</p>
          <h3 id="questionnaire-submit-heading">Submit questionnaire</h3>
          <p>Submission is final. Your answers cannot be changed after the questionnaire is submitted.</p>
          {!questionnaireComplete && <p>Answer every question before submitting.</p>}
        </div>

        <Button
          type="button"
          onClick={handleSubmit}
          disabled={!questionnaireComplete || submissionInProgress}
          isLoading={submissionInProgress}
          loadingLabel="Submitting..."
        >
          Submit questionnaire
        </Button>

        {submitMutation.isError && (
          <p className="assessment-inline-alert" role="alert">
            {getSubmissionErrorMessage(submitMutation.error)}
          </p>
        )}
      </section>
    </section>
  )
}

interface QuestionnaireStageProps { sessionId: string }

export function QuestionnaireStage({ sessionId }: QuestionnaireStageProps) {
  const questionnaireQuery = useSessionQuestionnaireQuery(sessionId)

  if (questionnaireQuery.isPending) {
    return <p className="assessment-stage-loading">Loading questionnaire...</p>
  }

  if (questionnaireQuery.isError) {
    return <p className="assessment-inline-alert" role="alert">Unable to load the questionnaire.</p>
  }

  return (
    <QuestionnaireForm
      key={sessionId}
      sessionId={sessionId}
      questionnaire={questionnaireQuery.data}
    />
  )
}
