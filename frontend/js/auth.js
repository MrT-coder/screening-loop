import { csrfToken } from './http.js';
import { onLanguageChange, t } from './i18n.js';

const LOGIN_PAGE = 'login.html';

export async function currentUser() {
  try {
    const response = await fetch('/api/me');
    return response.ok ? await response.json() : null;
  } catch (error) {
    return null;
  }
}

export async function signIn(username, password) {
  const response = await fetch('/api/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-XSRF-TOKEN': csrfToken() },
    body: new URLSearchParams({ username, password }),
  });
  if (response.ok) {
    await fetch('/api/me');
    return 'ok';
  }
  return response.status === 401 || response.status === 403 ? 'invalid' : 'error';
}

export async function signOut() {
  await fetch('/api/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrfToken() } });
  window.location.assign(LOGIN_PAGE);
}

export function goToLogin() {
  const next = window.location.pathname + window.location.search;
  window.location.replace(`${LOGIN_PAGE}?next=${encodeURIComponent(next)}`);
}

export async function requireStaff() {
  const user = await currentUser();
  if (!user) {
    goToLogin();
    return null;
  }
  const role = user.role.toLowerCase();
  document.getElementById('user-label').textContent = `${user.username} (${t(`role.${role}`)})`;
  onLanguageChange(() => window.location.reload());
  document.getElementById('logout-button').addEventListener('click', signOut);
  return user;
}

export function safeNext(value) {
  const isLocalPath = typeof value === 'string' && value.startsWith('/') && !value.startsWith('//');
  return isLocalPath ? value : '/';
}

export function handleExpiredSession(response) {
  if (response.status === 401) {
    goToLogin();
    return true;
  }
  return false;
}
