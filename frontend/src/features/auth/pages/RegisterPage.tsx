import {
  Link,
  useNavigate,
  useSearchParams,
} from 'react-router'
import {
  useForm,
  type UseFormSetError,
} from 'react-hook-form'

import { isApiError } from '../../../shared/api/apiError'
import {
  Button,
  Card,
  FormField,
} from '../../../shared/ui'
import { useRegisterMutation } from '../api/authQueries'
import type { RegisterRequest } from '../api/authTypes'
import { sanitizeReturnTo } from '../navigation/returnTo'

import './AuthPage.css'

function applyRegisterError(
  error: unknown,
  setError: UseFormSetError<RegisterRequest>,
) {
  if (!isApiError(error)) {
    setError('root.server', {
      type: 'server',
      message:
        'Unable to create your account right now. Please try again.',
    })
    return
  }

  if (error.code === 'EMAIL_ALREADY_REGISTERED') {
    setError('email', {
      type: 'server',
      message: 'An account already exists for this email.',
    })
    return
  }

  if (error.code === 'INVALID_EMAIL') {
    setError('email', {
      type: 'server',
      message: 'Enter a valid email address.',
    })
    return
  }

  if (error.code === 'INVALID_PASSWORD') {
    setError('password', {
      type: 'server',
      message:
        'Password must be between 15 and 128 characters.',
    })
    return
  }

  if (error.code === 'VALIDATION_FAILED') {
    let mappedFieldError = false

    for (const fieldError of error.problem?.fieldErrors ?? []) {
      if (
        fieldError.field === 'email' ||
        fieldError.field === 'password' ||
        fieldError.field === 'displayName'
      ) {
        setError(fieldError.field, {
          type: 'server',
          message: fieldError.message,
        })
        mappedFieldError = true
      }
    }

    if (mappedFieldError) {
      return
    }
  }

  setError('root.server', {
    type: 'server',
    message:
      'Unable to create your account right now. Please try again.',
  })
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const registerMutation = useRegisterMutation()
  const returnTo = sanitizeReturnTo(
    searchParams.get('returnTo'),
  )

  const {
    register,
    handleSubmit,
    setError,
    formState: {
      errors,
      isSubmitting,
    },
  } = useForm<RegisterRequest>()

  const onSubmit = handleSubmit(async (values) => {
    try {
      await registerMutation.mutateAsync(values)

      const loginParams = new URLSearchParams({
        registered: '1',
      })

      if (returnTo !== '/assessments') {
        loginParams.set('returnTo', returnTo)
      }

      navigate(`/login?${loginParams.toString()}`, {
        replace: true,
      })
    } catch (error) {
      applyRegisterError(error, setError)
    }
  })

  const submitting =
    isSubmitting || registerMutation.isPending

  const loginPath =
    returnTo === '/assessments'
      ? '/login'
      : `/login?returnTo=${encodeURIComponent(returnTo)}`

  return (
    <main className="auth-page">
      <Card className="auth-card" padding="lg">
        <div className="auth-card__heading">
          <span className="auth-card__icon" aria-hidden="true">
            P
          </span>
          <p className="auth-card__eyebrow">Start your journey</p>
          <h1>Create account</h1>
          <p>Create an account to save assessments and revisit your results.</p>
        </div>

        <form className="auth-form" onSubmit={onSubmit} noValidate>
          <FormField
            htmlFor="register-display-name"
            label="Display name"
            error={errors.displayName?.message}
            required
          >
            <input
              id="register-display-name"
              autoComplete="name"
              placeholder="How should we call you?"
              aria-invalid={Boolean(errors.displayName)}
              {...register('displayName', {
                required: 'Display name is required.',
                maxLength: {
                  value: 80,
                  message:
                    'Display name must be 80 characters or fewer.',
                },
              })}
            />
          </FormField>

          <FormField
            htmlFor="register-email"
            label="Email"
            error={errors.email?.message}
            required
          >
            <input
              id="register-email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              aria-invalid={Boolean(errors.email)}
              {...register('email', {
                required: 'Email is required.',
              })}
            />
          </FormField>

          <FormField
            htmlFor="register-password"
            label="Password"
            hint="Use 15–128 characters."
            error={errors.password?.message}
            required
          >
            <input
              id="register-password"
              type="password"
              autoComplete="new-password"
              placeholder="Create a secure password"
              aria-invalid={Boolean(errors.password)}
              {...register('password', {
                required: 'Password is required.',
                minLength: {
                  value: 15,
                  message:
                    'Password must be at least 15 characters.',
                },
                maxLength: {
                  value: 128,
                  message:
                    'Password must be 128 characters or fewer.',
                },
              })}
            />
          </FormField>

          {errors.root?.server?.message && (
            <p className="auth-message auth-message--error" role="alert">
              {errors.root.server.message}
            </p>
          )}

          <Button
            type="submit"
            fullWidth
            isLoading={submitting}
            loadingLabel="Creating account..."
          >
            Create account
          </Button>
        </form>

        <p className="auth-card__switch">
          Already have an account?{' '}
          <Link to={loginPath}>
            Sign in
          </Link>
        </p>
      </Card>

      <p className="auth-page__note">
        One account keeps your assessment journey in one place.
      </p>
    </main>
  )
}
