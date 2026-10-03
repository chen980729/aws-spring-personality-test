import {
  useCallback,
  useMemo,
  useState,
} from 'react'

import type { QuestionAnswer } from '../api/assessmentTypes'

export type QuestionnaireDraft =
  Record<string, number>

interface QuestionnaireDraftState {
  answersByQuestionId: QuestionnaireDraft
  revision: number
}

function createInitialDraft(
  persistedAnswers: QuestionAnswer[],
): QuestionnaireDraftState {
  return {
    answersByQuestionId: Object.fromEntries(
      persistedAnswers.map((answer) => [
        answer.questionId,
        answer.value,
      ]),
    ),
    revision: 0,
  }
}

export function useQuestionnaireDraft(
  persistedAnswers: QuestionAnswer[],
) {
  const [draft, setDraft] =
    useState<QuestionnaireDraftState>(() =>
      createInitialDraft(persistedAnswers),
    )

  const setAnswer = useCallback(
    (
      questionId: string,
      value: number,
    ) => {
      setDraft((current) => {
        if (
          current.answersByQuestionId[
            questionId
          ] === value
        ) {
          return current
        }

        return {
          answersByQuestionId: {
            ...current.answersByQuestionId,
            [questionId]: value,
          },
          revision: current.revision + 1,
        }
      })
    },
    [],
  )

  const answeredCount = useMemo(
    () =>
      Object.keys(
        draft.answersByQuestionId,
      ).length,
    [draft.answersByQuestionId],
  )

  return {
    answersByQuestionId:
      draft.answersByQuestionId,
    answeredCount,
    revision: draft.revision,
    setAnswer,
  }
}
