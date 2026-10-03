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
import { useLoginMutation } from '../api/authQueries'
import type { LoginRequest } from '../api/authTypes'
import { sanitizeReturnTo } from '../navigation/returnTo'

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
    <main>
      <h1>Login</h1>

      {searchParams.get('registered') === '1' && (
        <p role="status">
          Account created. Sign in to continue.
        </p>
      )}

      <form onSubmit={onSubmit} noValidate>
        <div>
          <label htmlFor="login-email">
            Email
          </label>
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            {...register('email', {
              required: 'Email is required.',
            })}
          />
          {errors.email?.message && (
            <p role="alert">
              {errors.email.message}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="login-password">
            Password
          </label>
          <input
            id="login-password"
            type="password"
            autoComplete="current-password"
            {...register('password', {
              required: 'Password is required.',
              maxLength: {
                value: 128,
                message:
                  'Password must be 128 characters or fewer.',
              },
            })}
          />
          {errors.password?.message && (
            <p role="alert">
              {errors.password.message}
            </p>
          )}
        </div>

        {errors.root?.server?.message && (
          <p role="alert">
            {errors.root.server.message}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
        >
          {submitting
            ? 'Signing in...'
            : 'Sign in'}
        </button>
      </form>

      <p>
        Need an account?{' '}
        <Link to={registerPath}>
          Create one
        </Link>
      </p>
    </main>
  )
}
