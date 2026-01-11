import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Suspense, lazy } from 'react'
import Header from './components/layout/Header'
import LoginModal from './components/auth/LoginModal'

// Pages (lazy loaded for better performance)
const HomePage = lazy(() => import('./pages/HomePage'))
const LandingPage = lazy(() => import('./pages/LandingPage'))
const SearchPage = lazy(() => import('./pages/SearchPage'))
const FavoritesPage = lazy(() => import('./pages/FavoritesPage'))
const ComparisonPage = lazy(() => import('./pages/ComparisonPage'))

// Query client for data fetching
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      retry: 3,
      refetchOnWindowFocus: false
    }
  }
})

// Loading component
function PageLoader() {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '60vh',
      fontSize: '1.125rem',
      color: 'var(--color-text-secondary)'
    }}>
      Cargando...
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="app">
          <a href="#main-content" className="skip-link">
            Saltar al contenido principal
          </a>
          <Header />
          <main className="main-content" id="main-content">
            <Suspense fallback={<PageLoader />}>
              <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/app" element={<HomePage />} />
                <Route path="/search" element={<SearchPage />} />
                <Route path="/favorites" element={<FavoritesPage />} />
                <Route path="/compare" element={<ComparisonPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </main>
          <LoginModal />
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App

