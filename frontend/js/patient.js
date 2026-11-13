import { postJson } from './http.js';
import { getLanguage, onLanguageChange, t } from './i18n.js';

const SCAN_MS = 4000;
const TICK_MS = 100;

const DEMO_PATIENTS = [
  { label: 'patient.demo.1', document: '999-38-6612', birthDate: '1976-10-05' },
  { label: 'patient.demo.2', document: '999-32-7580', birthDate: '1962-06-28' },
  { label: 'patient.demo.3', document: '999-95-4540', birthDate: '1960-07-30' },
  { label: 'patient.demo.4', document: '999-72-3904', birthDate: '1955-09-27' },
];

const STEPS = ['details', 'face', 'chat'];

const message = document.getElementById('message');
const detailsForm = document.getElementById('details-form');
const documentInput = document.getElementById('document');
const birthDateInput = document.getElementById('birth-date');
const camera = document.getElementById('camera');
const video = document.getElementById('video');
const scanBar = document.getElementById('scan-bar');
const scanStatus = document.getElementById('scan-status');
const scanCaption = document.getElementById('scan-caption');
const startScan = document.getElementById('start-scan');
const log = document.getElementById('log');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const endSession = document.getElementById('end-session');

let stream = null;
let isBusy = false;

function showMessage(text) {
  message.textContent = text;
  message.hidden = !text;
}

function showStep(name) {
  STEPS.forEach((step, index) => {
    document.getElementById(`step-${step}`).hidden = step !== name;
    const mark = document.getElementById(`mark-${step}`);
    const current = STEPS.indexOf(name);
    mark.classList.toggle('is-active', step === name);
    mark.classList.toggle('is-done', index < current);
  });
  endSession.hidden = name !== 'chat';
}

function renderDemoList() {
  const list = document.getElementById('demo-list');
  list.replaceChildren(...DEMO_PATIENTS.map((patient) => {
    const item = document.createElement('li');
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = `${t(patient.label)} (${patient.document})`;
    button.addEventListener('click', () => {
      documentInput.value = patient.document;
      birthDateInput.value = patient.birthDate;
      showMessage('');
    });
    item.append(button);
    return item;
  }));
}

function stopCamera() {
  if (stream) {
    stream.getTracks().forEach((track) => track.stop());
    stream = null;
  }
  video.srcObject = null;
  camera.classList.remove('has-video', 'is-scanning');
}

async function openCamera() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    return false;
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false });
    video.srcObject = stream;
    await video.play();
    camera.classList.add('has-video');
    return true;
  } catch (error) {
    stream = null;
    return false;
  }
}

function pause(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function runScan() {
  camera.classList.add('is-scanning');
  scanStatus.textContent = t('patient.face.scanning');
  for (let elapsed = 0; elapsed <= SCAN_MS; elapsed += TICK_MS) {
    scanBar.style.width = `${Math.min(100, (elapsed / SCAN_MS) * 100)}%`;
    await pause(TICK_MS);
  }
  camera.classList.remove('is-scanning');
}

async function readError(response) {
  if (response.status === 429) {
    return t('patient.error.tooMany');
  }
  return t('patient.error.identity');
}

async function verify() {
  const liveness = await postJson('/api/chat/liveness');
  if (!liveness.ok) {
    return t('patient.error.face');
  }
  const { token } = await liveness.json();
  const response = await postJson('/api/chat/verify', {
    documentNumber: documentInput.value.trim(),
    birthDate: birthDateInput.value,
    livenessToken: token,
  });
  return response.status === 204 ? '' : readError(response);
}

async function handleStartScan() {
  if (isBusy) {
    return;
  }
  isBusy = true;
  startScan.disabled = true;
  showMessage('');
  scanBar.style.width = '0';
  const hasCamera = await openCamera();
  scanCaption.textContent = hasCamera
    ? t('patient.face.caption')
    : t('patient.face.noCamera');
  await runScan();
  stopCamera();
  scanStatus.textContent = t('patient.face.checking');
  const error = await verify();
  isBusy = false;
  startScan.disabled = false;
  if (error) {
    scanStatus.textContent = t('patient.face.ready');
    showMessage(error);
    showStep('details');
    documentInput.focus();
    return;
  }
  await enterChat();
}

function addBubble(text, kind) {
  const bubble = document.createElement('div');
  bubble.className = `bubble ${kind}`;
  bubble.textContent = text;
  log.append(bubble);
  log.scrollTop = log.scrollHeight;
}

async function ask(text, isVisible) {
  if (isVisible) {
    addBubble(text, 'me');
  }
  try {
    const response = await postJson('/api/chat/messages', { text, language: getLanguage() });
    if (response.status === 401 || response.status === 403) {
      showMessage(t('patient.error.sessionEnded'));
      showStep('details');
      return;
    }
    if (!response.ok) {
      addBubble(t('patient.chat.failed'), 'bot');
      return;
    }
    const reply = await response.json();
    addBubble(reply.reply, 'bot');
  } catch (error) {
    addBubble(t('patient.chat.failed'), 'bot');
  }
}

async function enterChat() {
  await fetch('/api/chat/session');
  log.replaceChildren();
  showStep('chat');
  await ask(t('patient.chat.greeting'), false);
  chatInput.focus();
}

async function start() {
  renderDemoList();
  try {
    const response = await fetch('/api/chat/session');
    if (response.ok) {
      log.replaceChildren();
      showStep('chat');
      await ask(t('patient.chat.greeting'), false);
      return;
    }
  } catch (error) {
    showMessage(t('common.unreachable'));
  }
  showStep('details');
}

function firstInvalidField() {
  if (!documentInput.value.trim()) {
    return { field: documentInput, text: t('patient.error.document') };
  }
  if (!documentInput.checkValidity()) {
    return { field: documentInput, text: t('patient.error.documentFormat') };
  }
  if (!birthDateInput.value) {
    return { field: birthDateInput, text: t('patient.error.birthDate') };
  }
  return null;
}

detailsForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const problem = firstInvalidField();
  if (problem) {
    showMessage(problem.text);
    problem.field.focus();
    return;
  }
  showMessage('');
  scanStatus.textContent = t('patient.face.ready');
  scanBar.style.width = '0';
  showStep('face');
  startScan.focus();
});

startScan.addEventListener('click', handleStartScan);

document.getElementById('back-to-details').addEventListener('click', () => {
  stopCamera();
  showStep('details');
});

chatForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const text = chatInput.value.trim();
  if (!text) {
    return;
  }
  chatInput.value = '';
  await ask(text, true);
});

document.getElementById('chips').addEventListener('click', (event) => {
  const button = event.target.closest('button[data-text]');
  if (button) {
    ask(button.dataset.text, true);
  }
});

endSession.addEventListener('click', async () => {
  await postJson('/api/logout');
  await fetch('/api/chat/session');
  stopCamera();
  log.replaceChildren();
  detailsForm.reset();
  showMessage('');
  showStep('details');
});

onLanguageChange(renderDemoList);

start();
