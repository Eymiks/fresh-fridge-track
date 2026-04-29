import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AnimatePresence } from "framer-motion";
import { WifiOff } from "lucide-react";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { useIsMobile } from "@/hooks/use-mobile";
import { useOnlineStatus } from "@/hooks/useOnlineStatus";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { DesktopSidebar } from "@/components/DesktopSidebar";
import { MobileMenu } from "@/components/MobileMenu";
import { NotificationChecker } from "@/components/NotificationChecker";
import Auth from "./pages/Auth.tsx";
import HouseholdSetup from "./pages/HouseholdSetup.tsx";
import Index from "./pages/Index.tsx";
import ProductDetail from "./pages/ProductDetail.tsx";
import Stats from "./pages/Stats.tsx";
import Credits from "./pages/Credits.tsx";
import Notifications from "./pages/Notifications.tsx";
import History from "./pages/History.tsx";
import Settings from "./pages/Settings.tsx";
import NotFound from "./pages/NotFound.tsx";

const queryClient = new QueryClient();

function Layout({ children }: { children: React.ReactNode }) {
  const isMobile = useIsMobile();
  if (!isMobile) {
    return (
      <div className="flex h-screen bg-background">
        <DesktopSidebar />
        <main className="flex-1 overflow-auto">{children}</main>
      </div>
    );
  }
  return (
    <>
      {children}
      <MobileMenu />
    </>
  );
}

function AppRoutes() {
  const { user, household, loading } = useAuth();
  const location = useLocation();
  const isOnline = useOnlineStatus();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-background">
        <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) return <Auth />;
  if (!household) return <HouseholdSetup />;

  return (
    <Layout>
      {!isOnline && (
        <div className="fixed top-0 inset-x-0 z-50 flex items-center justify-center gap-2 bg-warning text-warning-foreground py-2 text-xs font-semibold">
          <WifiOff className="w-3.5 h-3.5" />
          Hors ligne — les données affichées peuvent être obsolètes
        </div>
      )}
      <NotificationChecker />
      <AnimatePresence mode="sync">
        <Routes location={location} key={location.pathname}>
          <Route path="/" element={<Index />} />
          <Route path="/product/:id" element={<ProductDetail />} />
          <Route path="/stats" element={<Stats />} />
          <Route path="/credits" element={<Credits />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/history" element={<History />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/household" element={<Navigate to="/settings" replace />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </AnimatePresence>
    </Layout>
  );
}

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
