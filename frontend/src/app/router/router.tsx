import { createBrowserRouter } from 'react-router'

import { LandingPage } from '../pages/LandingPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { LoginPage } from '../../features/auth/pages/LoginPage'
import { RegisterPage } from '../../features/auth/pages/RegisterPage'
import { AssessmentCatalogPage } from '../../features/assessment/pages/AssessmentCatalogPage'
import { AssessmentDetailPage } from '../../features/assessment/pages/AssessmentDetailPage'
import { AssessmentHistoryPage } from '../../features/assessment/pages/AssessmentHistoryPage'
import { AssessmentSessionPage } from '../../features/assessment/pages/AssessmentSessionPage'
import { AuthenticatedLayout } from './AuthenticatedLayout'
import { PublicLayout } from './PublicLayout'

export const router = createBrowserRouter([
    {
        element: <PublicLayout />,
        children: [
            {
                path: '/',
                element: <LandingPage />,
            },
            {
                path: '/login',
                element: <LoginPage />,
            },
            {
                path: '/register',
                element: <RegisterPage />,
            },
        ],
    },
    {
        element: <AuthenticatedLayout />,
        children: [
            {
                path: '/assessments',
                element: <AssessmentCatalogPage />,
            },
            {
                path: '/assessments/:assessmentCode',
                element: <AssessmentDetailPage />,
            },
            {
                path: '/assessment-sessions/:sessionId',
                element: <AssessmentSessionPage />,
            },
            {
                path: '/history',
                element: <AssessmentHistoryPage />,
            },
        ],
    },
    {
        path: '*',
        element: <NotFoundPage />,
    },
])