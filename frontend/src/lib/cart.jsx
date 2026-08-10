import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api } from '../lib/api';
import { useAuth } from './auth';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { user } = useAuth();
  const [cart, setCart] = useState(null);
  const [count, setCount] = useState(0);

  const load = useCallback(async () => {
    if (!user) { setCart(null); setCount(0); return; }
    try {
      const data = await api.get('/cart');
      setCart(data);
      setCount(data.itemCount || 0);
    } catch {
      setCart(null);
      setCount(0);
    }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const addItem = async (item) => {
    const data = await api.post('/cart/items', item);
    setCart(data);
    setCount(data.itemCount || 0);
    return data;
  };

  const updateQty = async (productId, quantity) => {
    const data = await api.put(`/cart/items/${productId}`, { quantity });
    setCart(data);
    setCount(data.itemCount || 0);
    return data;
  };

  const removeItem = async (productId) => {
    const data = await api.del(`/cart/items/${productId}`);
    setCart(data);
    setCount(data.itemCount || 0);
    return data;
  };

  const clear = async () => {
    await api.del('/cart');
    setCart(null);
    setCount(0);
  };

  return (
    <CartContext.Provider value={{ cart, count, addItem, updateQty, removeItem, clear, load }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  return useContext(CartContext);
}
