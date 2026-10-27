import { Link, Route, Routes } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import { RequireAuth } from './components/RequireAuth'
import { SuspendedBanner } from './components/SuspendedBanner'
import { AdminPage } from './pages/AdminPage'
import { CommunityPage } from './pages/CommunityPage'
import { ContributorRequestsPage } from './pages/ContributorRequestsPage'
import { CreatePostPage } from './pages/CreatePostPage'
import { EditPostPage } from './pages/EditPostPage'
import { FollowListPage } from './pages/FollowListPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MePage } from './pages/MePage'
import { ModerationPage } from './pages/ModerationPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { PostDetailPage } from './pages/PostDetailPage'
import { PrivacyPage } from './pages/PrivacyPage'
import { ProfilePage } from './pages/ProfilePage'
import { RegisterPage } from './pages/RegisterPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { SavedPostsPage } from './pages/SavedPostsPage'
import { SearchPage } from './pages/SearchPage'
import { TermsPage } from './pages/TermsPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'

// Layout base + rutas. Las rutas placeholder se van completando por hito (ver docs/frontend-spec.md §6).
export default function App() {
  return (
    <div className="min-h-svh bg-page text-ink">
      <Navbar />
      <SuspendedBanner />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/terminos" element={<TermsPage />} />
          <Route path="/privacidad" element={<PrivacyPage />} />
          <Route path="/normas" element={<CommunityPage />} />
          <Route
            path="/posts/new"
            element={
              <RequireAuth>
                <CreatePostPage />
              </RequireAuth>
            }
          />
          <Route
            path="/posts/:id/edit"
            element={
              <RequireAuth>
                <EditPostPage />
              </RequireAuth>
            }
          />
          <Route path="/posts/:id" element={<PostDetailPage />} />
          <Route path="/users/:username/followers" element={<FollowListPage mode="followers" />} />
          <Route path="/users/:username/following" element={<FollowListPage mode="following" />} />
          <Route path="/users/:username" element={<ProfilePage />} />
          <Route
            path="/me"
            element={
              <RequireAuth>
                <MePage />
              </RequireAuth>
            }
          />
          <Route
            path="/saved"
            element={
              <RequireAuth>
                <SavedPostsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/notifications"
            element={
              <RequireAuth>
                <NotificationsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/moderation"
            element={
              <RequireAuth>
                <ModerationPage />
              </RequireAuth>
            }
          />
          <Route
            path="/contributor-requests"
            element={
              <RequireAuth>
                <ContributorRequestsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAuth>
                <AdminPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<PlaceholderPage title="No encontrado" />} />
        </Routes>
      </main>
      <footer className="mx-auto max-w-6xl px-4 py-8 text-sm text-ink-faint">
        <nav className="flex flex-wrap gap-x-4 gap-y-1 border-t border-sand-200 pt-4">
          <Link to="/terminos" className="hover:text-leaf-700">
            Términos y Condiciones
          </Link>
          <Link to="/privacidad" className="hover:text-leaf-700">
            Política de Privacidad
          </Link>
          <Link to="/normas" className="hover:text-leaf-700">
            Normas de la comunidad
          </Link>
        </nav>
      </footer>
    </div>
  )
}
