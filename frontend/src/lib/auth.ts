export type UserRole = 'CLIENT' | 'FREELANCER';

export interface StoredUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
}

const tokenKey = 'worklink_token';
const userKey = 'worklink_user';

export function getStoredUser(): StoredUser | null {
  const stored = localStorage.getItem(userKey);
  if (!stored) return null;

  try {
    const user: unknown = JSON.parse(stored);
    if (typeof user !== 'object' || user === null) return null;
    const candidate = user as Partial<StoredUser>;
    if (typeof candidate.id !== 'number' || typeof candidate.firstName !== 'string'
      || typeof candidate.lastName !== 'string' || typeof candidate.email !== 'string'
      || (candidate.role !== 'CLIENT' && candidate.role !== 'FREELANCER')) return null;
    return candidate as StoredUser;
  } catch {
    return null;
  }
}

export function hasSession(): boolean {
  return Boolean(localStorage.getItem(tokenKey) && getStoredUser());
}

export function clearSession(): void {
  localStorage.removeItem(tokenKey);
  localStorage.removeItem(userKey);
}

export function dashboardPath(role: UserRole): string {
  return role === 'CLIENT' ? '/client/dashboard' : '/freelancer/dashboard';
}
