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
import { useLoginMutation } from '../api/authQueries'
import type { LoginRequest } from '../api/authTypes'
import { sanitizeReturnTo } from '../navigation/returnTo'

import './AuthPage.css'

function applyLoginError(
  error: unknown,
  setError: UseFormSetError<LoginRequest>,
) {
  if (
    isApiError(error) &&
    error.code === 'INVALID_CREDENTIALS'
  ) {
    setError('root.server', {
      type: 'server',
      message: 'The email or password is incorrect.',
    })
    return
  }

  if (
    isApiError(error) &&
    error.code === 'INVALID_EMAIL'
  ) {
    setError('email', {
      type: 'server',
      message: 'Enter a valid email address.',
    })
    return
  }

  if (
    isApiError(error) &&
    error.code === 'VALIDATION_FAILED'
  ) {
    let mappedFieldError = false

    for (const fieldError of error.problem?.fieldErrors ?? []) {
      if (
        fieldError.field === 'email' ||
        fieldError.field === 'password'
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
      'Unable to sign in right now. Please try again.',
  })
}

export function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const loginMutation = useLoginMutation()
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
  } = useForm<LoginRequest>()

  const onSubmit = handleSubmit(async (values) => {
    try {
      await loginMutation.mutateAsync(values)
      navigate(returnTo, {
        replace: true,
      })
    } catch (error) {
      applyLoginError(error, setError)
    }
  })

  const submitting =
    isSubmitting || loginMutation.isPending

  const registerPath =
    returnTo === '/assessments'
      ? '/register'
      : `/register?returnTo=${encodeURIComponent(returnTo)}`

  return (
    <main className="auth-page">
      <Card className="auth-card" padding="lg">
        <div className="auth-card__heading">
          <span className="auth-card__icon" aria-hidden="true">
            P
          </span>
          <p className="auth-card__eyebrow">Welcome back</p>
          <h1>Login</h1>
          <p>Sign in to continue your personality journey.</p>
        </div>

        {searchParams.get('registered') === '1' && (
          <p className="auth-message auth-message--success" role="status">
            Account created. Sign in to continue.
          </p>
        )}

        <form className="auth-form" onSubmit={onSubmit} noValidate>
          <FormField
            htmlFor="login-email"
            label="Email"
            error={errors.email?.message}
            required
          >
            <input
              id="login-email"
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
            htmlFor="login-password"
            label="Password"
            error={errors.password?.message}
            required
          >
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              placeholder="Enter your password"
              aria-invalid={Boolean(errors.password)}
              {...register('password', {
                required: 'Password is required.',
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
            loadingLabel="Signing in..."
          >
            Sign in
          </Button>
        </form>

        <p className="auth-card__switch">
          Need an account?{' '}
          <Link to={registerPath}>
            Create one
          </Link>
        </p>
      </Card>

      <p className="auth-page__note">
        Your assessment history stays connected to your account.
      </p>
    </main>
  )
}
