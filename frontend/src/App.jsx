import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar, { Footer } from './components/Navbar';
import { useAuth } from './lib/auth';
import { Spinner } from './components/ui';

import Home from './pages/Home';
import Products from './pages/Products';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import Login from './pages/Login';
import Register from './pages/Register';
import Account from './pages/Account';

const Health = lazy(() => import('./pages/Health'));
const Loyalty = lazy(() => import('./pages/Loyalty'));
const Coupons = lazy(() => import('./pages/Coupons'));
const Prescriptions = lazy(() => import('./pages/Prescriptions'));
const Campaigns = lazy(() => import('./pages/Campaigns'));

const DistributorDashboard = lazy(() => import('./pages/distributor/Dashboard'));
const DistributorProfile = lazy(() => import('./pages/distributor/Profile'));
const DistributorCatalog = lazy(() => import('./pages/distributor/Catalog'));
const DistributorOrders = lazy(() => import('./pages/distributor/Orders'));
const DistributorFulfillments = lazy(() => import('./pages/distributor/Fulfillments'));
const DistributorPurchase = lazy(() => import('./pages/distributor/Purchase'));

const PharmacyDashboard = lazy(() => import('./pages/pharmacy/Dashboard'));
const PharmacyOrders = lazy(() => import('./pages/pharmacy/Orders'));
const PharmacyInventory = lazy(() => import('./pages/pharmacy/Inventory'));
const PharmacySlots = lazy(() => import('./pages/pharmacy/Slots'));

const AdminDashboard = lazy(() => import('./pages/admin/Dashboard'));
const AdminVerifications = lazy(() => import('./pages/admin/Verifications'));
const AdminCampaigns = lazy(() => import('./pages/admin/Campaigns'));
const AdminDiscounts = lazy(() => import('./pages/admin/Discounts'));
const AdminProducts = lazy(() => import('./pages/admin/Products'));
const AdminOverview = lazy(() => import('./pages/admin/Overview'));
const AdminUsers = lazy(() => import('./pages/admin/Users'));

const PartnerDashboard = lazy(() => import('./pages/partner/Dashboard'));
const PartnerAvailable = lazy(() => import('./pages/partner/Available'));
const PartnerDeliveries = lazy(() => import('./pages/partner/MyDeliveries'));

function Page({ children }) {
  return <Suspense fallback={<div className="flex min-h-[60vh] items-center justify-center"><Spinner className="h-8 w-8" /></div>}>{children}</Suspense>;
}

function PublicLayout({ children }) {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1">{children}</main>
      <Footer />
    </div>
  );
}

function Protected({ children, roles }) {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PublicLayout><Home /></PublicLayout>} />
      <Route path="/products" element={<PublicLayout><Products /></PublicLayout>} />
      <Route path="/products/:id" element={<PublicLayout><ProductDetail /></PublicLayout>} />
      <Route path="/health" element={<PublicLayout><Page><Health /></Page></PublicLayout>} />
      <Route path="/login" element={<PublicLayout><Login /></PublicLayout>} />
      <Route path="/register" element={<PublicLayout><Register /></PublicLayout>} />

      <Route path="/cart" element={<Protected><PublicLayout><Cart /></PublicLayout></Protected>} />
      <Route path="/checkout" element={<Protected><PublicLayout><Checkout /></PublicLayout></Protected>} />
      <Route path="/orders" element={<Protected><PublicLayout><Orders /></PublicLayout></Protected>} />
      <Route path="/orders/:id" element={<Protected><PublicLayout><OrderDetail /></PublicLayout></Protected>} />
      <Route path="/account" element={<Protected><PublicLayout><Account /></PublicLayout></Protected>} />
      <Route path="/loyalty" element={<Protected><PublicLayout><Page><Loyalty /></Page></PublicLayout></Protected>} />
      <Route path="/discounts" element={<Protected><PublicLayout><Page><Coupons /></Page></PublicLayout></Protected>} />
      <Route path="/campaigns" element={<Protected><PublicLayout><Page><Campaigns /></Page></PublicLayout></Protected>} />
      <Route path="/prescriptions" element={<Protected><PublicLayout><Page><Prescriptions /></Page></PublicLayout></Protected>} />

      <Route path="/distributor/*" element={<Protected roles={['DISTRIBUTOR', 'PHARMACIST', 'CUSTOMER']}><Page><DistributorRoutes /></Page></Protected>} />
      <Route path="/pharmacy/*" element={<Protected roles={['PHARMACIST', 'DISTRIBUTOR']}><Page><PharmacyRoutes /></Page></Protected>} />
      <Route path="/partner/*" element={<Protected roles={['DELIVERY_PARTNER', 'DISTRIBUTOR', 'PHARMACIST', 'ADMIN']}><Page><PartnerRoutes /></Page></Protected>} />
      <Route path="/admin/*" element={<Protected roles={['ADMIN']}><Page><AdminRoutes /></Page></Protected>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function DistributorRoutes() {
  return (
    <Routes>
      <Route index element={<DistributorDashboard />} />
      <Route path="profile" element={<DistributorProfile />} />
      <Route path="catalog" element={<DistributorCatalog />} />
      <Route path="orders" element={<DistributorOrders />} />
      <Route path="fulfillments" element={<DistributorFulfillments />} />
      <Route path="purchase" element={<DistributorPurchase />} />
    </Routes>
  );
}

function PharmacyRoutes() {
  return (
    <Routes>
      <Route index element={<PharmacyDashboard />} />
      <Route path="orders" element={<PharmacyOrders />} />
      <Route path="inventory" element={<PharmacyInventory />} />
      <Route path="slots" element={<PharmacySlots />} />
    </Routes>
  );
}

function AdminRoutes() {
  return (
    <Routes>
      <Route index element={<AdminDashboard />} />
      <Route path="verifications" element={<AdminVerifications />} />
      <Route path="campaigns" element={<AdminCampaigns />} />
      <Route path="discounts" element={<AdminDiscounts />} />
      <Route path="products" element={<AdminProducts />} />
      <Route path="overview" element={<AdminOverview />} />
      <Route path="users" element={<AdminUsers />} />
    </Routes>
  );
}

function PartnerRoutes() {
  return (
    <Routes>
      <Route index element={<PartnerDashboard />} />
      <Route path="available" element={<PartnerAvailable />} />
      <Route path="deliveries" element={<PartnerDeliveries />} />
    </Routes>
  );
}
