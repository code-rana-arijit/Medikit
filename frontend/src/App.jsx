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
import Health from './pages/Health';
import Loyalty from './pages/Loyalty';

import DistributorDashboard from './pages/distributor/Dashboard';
import DistributorProfile from './pages/distributor/Profile';
import DistributorCatalog from './pages/distributor/Catalog';
import DistributorOrders from './pages/distributor/Orders';
import DistributorFulfillments from './pages/distributor/Fulfillments';
import DistributorPurchase from './pages/distributor/Purchase';

import PharmacyDashboard from './pages/pharmacy/Dashboard';
import PharmacyOrders from './pages/pharmacy/Orders';
import PharmacyInventory from './pages/pharmacy/Inventory';

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
      <Route path="/health" element={<PublicLayout><Health /></PublicLayout>} />
      <Route path="/login" element={<PublicLayout><Login /></PublicLayout>} />
      <Route path="/register" element={<PublicLayout><Register /></PublicLayout>} />

      <Route path="/cart" element={<Protected><PublicLayout><Cart /></PublicLayout></Protected>} />
      <Route path="/checkout" element={<Protected><PublicLayout><Checkout /></PublicLayout></Protected>} />
      <Route path="/orders" element={<Protected><PublicLayout><Orders /></PublicLayout></Protected>} />
      <Route path="/orders/:id" element={<Protected><PublicLayout><OrderDetail /></PublicLayout></Protected>} />
      <Route path="/account" element={<Protected><PublicLayout><Account /></PublicLayout></Protected>} />
      <Route path="/loyalty" element={<Protected><PublicLayout><Loyalty /></PublicLayout></Protected>} />

      <Route path="/distributor/*" element={<Protected roles={['DISTRIBUTOR', 'PHARMACIST', 'CUSTOMER']}><DistributorRoutes /></Protected>} />
      <Route path="/pharmacy/*" element={<Protected roles={['PHARMACIST', 'DISTRIBUTOR']}><PharmacyRoutes /></Protected>} />

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
    </Routes>
  );
}
