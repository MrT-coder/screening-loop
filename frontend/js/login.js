import { currentUser, safeNext, signIn } from './auth.js';

const GUEST_CREDENTIALS = { username: 'guest', password: 'guest-demo' };

const form = document.getElementById('login-form');
const message = document.getElementById('message');
const submit = form.querySelector('button[type="submit"]');
const guestButton = document.getElementById('guest-button');

function showMessage(text) {
  message.textContent = text;
  message.hidden = !text;
}

function nextPage() {
  return safeNext(new URLSearchParams(window.location.search).get('next'));
}

function setBusy(isBusy) {
  submit.disabled = isBusy;
  guestButton.disabled = isBusy;
}

async function attempt(username, password) {
  setBusy(true);
  showMessage('');
  try {
    const result = await signIn(username, password);
    if (result === 'ok') {
      window.location.replace(nextPage());
      return;
    }
    showMessage(result === 'invalid' ? 'Wrong user or password.' : 'The server had a problem. Please try again.');
  } catch (error) {
    showMessage('The server is not reachable.');
  }
  setBusy(false);
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  const data = new FormData(form);
  const username = String(data.get('username')).trim();
  const password = String(data.get('password'));
  if (!username || !password) {
    showMessage('Enter your user and password.');
    form.elements[username ? 'password' : 'username'].focus();
    return;
  }
  attempt(username, password);
});

guestButton.addEventListener('click', () => attempt(GUEST_CREDENTIALS.username, GUEST_CREDENTIALS.password));

currentUser().then((user) => {
  if (user) {
    window.location.replace(nextPage());
  }
});
