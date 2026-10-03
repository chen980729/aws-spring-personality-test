import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'

import { useSaveQuestionnaireProgressMutation } from '../api/assessmentQueries'
import type { QuestionAnswer } from '../api/assessmentTypes'

export type AutosaveStatus =
  | 'saved'
  | 'pending'
  | 'saving'
  | 'error'

interface UseQuestionnaireAutosaveOptions {
  sessionId: string
  answers: QuestionAnswer[]
  revision: number
  debounceMs?: number
}

export function useQuestionnaireAutosave({
  sessionId,
  answers,
  revision,
  debounceMs = 700,
}: UseQuestionnaireAutosaveOptions) {
  const {
    mutateAsync: saveProgress,
  } = useSaveQuestionnaireProgressMutation(
    sessionId,
  )

  const [status, setStatus] =
    useState<AutosaveStatus>('saved')

  const latestAnswersRef =
    useRef<QuestionAnswer[]>(answers)

  const latestRevisionRef =
    useRef(revision)

  const savedRevisionRef =
    useRef(0)

  const pausedRef =
    useRef(false)

  const activeSavePromiseRef =
    useRef<Promise<void> | null>(null)

  const timerRef =
    useRef<ReturnType<typeof setTimeout> | null>(
      null,
    )

  const mountedRef =
    useRef(false)

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const runSave = useCallback(async () => {
    clearTimer()

    if (pausedRef.current) {
      return
    }

    if (activeSavePromiseRef.current) {
      await activeSavePromiseRef.current
      return
    }

    const work = (async () => {
      while (
        !pausedRef.current &&
        latestRevisionRef.current >
          savedRevisionRef.current
      ) {
        const revisionBeingSaved =
          latestRevisionRef.current

        const answersBeingSaved =
          latestAnswersRef.current

        if (mountedRef.current) {
          setStatus('saving')
        }

        try {
          await saveProgress(
            answersBeingSaved,
          )
        } catch {
          if (mountedRef.current) {
            setStatus('error')
          }

          return
        }

        savedRevisionRef.current =
          revisionBeingSaved
      }

      if (
        mountedRef.current &&
        !pausedRef.current &&
        latestRevisionRef.current <=
          savedRevisionRef.current
      ) {
        setStatus('saved')
      }
    })()

    activeSavePromiseRef.current = work

    try {
      await work
    } finally {
      if (
        activeSavePromiseRef.current === work
      ) {
        activeSavePromiseRef.current = null
      }
    }
  }, [
    clearTimer,
    saveProgress,
  ])

  const retry = useCallback(() => {
    if (
      pausedRef.current ||
      latestRevisionRef.current <=
        savedRevisionRef.current
    ) {
      return
    }

    clearTimer()
    setStatus('pending')

    timerRef.current = setTimeout(
      () => {
        void runSave()
      },
      0,
    )
  }, [
    clearTimer,
    runSave,
  ])

  const prepareForSubmit = useCallback(
    async () => {
      pausedRef.current = true
      clearTimer()

      const activeSave =
        activeSavePromiseRef.current

      if (activeSave) {
        await activeSave
      }
    },
    [clearTimer],
  )

  const resumeAfterSubmitFailure =
    useCallback(() => {
      pausedRef.current = false
      clearTimer()

      if (
        latestRevisionRef.current <=
        savedRevisionRef.current
      ) {
        setStatus('saved')
        return
      }

      setStatus('pending')

      timerRef.current = setTimeout(
        () => {
          void runSave()
        },
        0,
      )
    }, [
      clearTimer,
      runSave,
    ])

  useEffect(() => {
    latestAnswersRef.current = answers
    latestRevisionRef.current = revision

    if (
      pausedRef.current ||
      revision <= savedRevisionRef.current
    ) {
      return
    }

    clearTimer()
    setStatus('pending')

    if (activeSavePromiseRef.current) {
      return
    }

    timerRef.current = setTimeout(
      () => {
        void runSave()
      },
      debounceMs,
    )

    return clearTimer
  }, [
    answers,
    clearTimer,
    debounceMs,
    revision,
    runSave,
  ])

  useEffect(() => {
    // React StrictMode intentionally runs effect
    // setup -> cleanup -> setup again in development.
    // Every setup must therefore restore the live state.
    mountedRef.current = true
    pausedRef.current = false

    return () => {
      mountedRef.current = false
      pausedRef.current = true
      clearTimer()
    }
  }, [clearTimer])

  return {
    status,
    retry,
    prepareForSubmit,
    resumeAfterSubmitFailure,
  }
}
