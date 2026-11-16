export function commerceStorageKey(storage: Storage, name: 'guest-cart-id' | 'customer-jwt' | 'admin-auth'): string {
  const key = 'commerce-' + name;
  const previous = 'thar-nectar-' + name;
  const value = storage.getItem(previous);
  if (value !== null) {
    if (storage.getItem(key) === null) storage.setItem(key, value);
    storage.removeItem(previous);
  }
  return key;
}
