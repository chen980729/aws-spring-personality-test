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
import { useRegisterMutation } from '../api/authQueries'
import type { RegisterRequest } from '../api/authTypes'
import { sanitizeReturnTo } from '../navigation/returnTo'

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
    <main>
      <h1>Create account</h1>

      <form onSubmit={onSubmit} noValidate>
        <div>
          <label htmlFor="register-display-name">
            Display name
          </label>
          <input
            id="register-display-name"
            autoComplete="name"
            {...register('displayName', {
              required: 'Display name is required.',
              maxLength: {
                value: 80,
                message:
                  'Display name must be 80 characters or fewer.',
              },
            })}
          />
          {errors.displayName?.message && (
            <p role="alert">
              {errors.displayName.message}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="register-email">
            Email
          </label>
          <input
            id="register-email"
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
          <label htmlFor="register-password">
            Password
          </label>
          <input
            id="register-password"
            type="password"
            autoComplete="new-password"
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
            ? 'Creating account...'
            : 'Create account'}
        </button>
      </form>

      <p>
        Already have an account?{' '}
        <Link to={loginPath}>
          Sign in
        </Link>
      </p>
    </main>
  )
}
