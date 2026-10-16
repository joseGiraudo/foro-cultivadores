import { Route, Routes } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import { RequireAuth } from './components/RequireAuth'
import { SuspendedBanner } from './components/SuspendedBanner'
import { AdminPage } from './pages/AdminPage'
import { CreatePostPage } from './pages/CreatePostPage'
import { EditPostPage } from './pages/EditPostPage'
import { FollowListPage } from './pages/FollowListPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MePage } from './pages/MePage'
import { ModerationPage } from './pages/ModerationPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { PostDetailPage } from './pages/PostDetailPage'
import { ProfilePage } from './pages/ProfilePage'
import { RegisterPage } from './pages/RegisterPage'
import { SavedPostsPage } from './pages/SavedPostsPage'

// Layout base + rutas. Las rutas placeholder se van completando por hito (ver docs/frontend-spec.md §6).
export default function App() {
  return (
    <div className="min-h-svh bg-cream-50 text-ink">
      <Navbar />
      <SuspendedBanner />
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
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
    </div>
  )
}
