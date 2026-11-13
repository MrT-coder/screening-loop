export function csrfToken() {
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.split('=')[1]) : '';
}

export function postJson(url, body) {
  const headers = { 'X-XSRF-TOKEN': csrfToken() };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  return fetch(url, {
    method: 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
