import en from './locales/en.js';
import es from './locales/es.js';

const DICTIONARIES = { en, es };
const SUPPORTED = Object.keys(DICTIONARIES);
const DEFAULT_LANGUAGE = 'en';
const STORAGE_KEY = 'screening-loop.language';
const LOCALES = { en: 'en-US', es: 'es-EC' };
const listeners = [];

function readStored() {
  try {
    return window.localStorage.getItem(STORAGE_KEY);
  } catch (error) {
    return null;
  }
}

function writeStored(code) {
  try {
    window.localStorage.setItem(STORAGE_KEY, code);
  } catch (error) {
    return;
  }
}

function detect() {
  const stored = readStored();
  if (SUPPORTED.includes(stored)) {
    return stored;
  }
  const browser = (navigator.language || DEFAULT_LANGUAGE).slice(0, 2).toLowerCase();
  return SUPPORTED.includes(browser) ? browser : DEFAULT_LANGUAGE;
}

let current = detect();

export function getLanguage() {
  return current;
}

export function getLocale() {
  return LOCALES[current];
}

export function t(key, params = {}) {
  const text = DICTIONARIES[current][key] ?? DICTIONARIES[DEFAULT_LANGUAGE][key] ?? key;
  return text.replace(/\{(\w+)\}/g, (match, name) => (name in params ? String(params[name]) : match));
}

export function tp(key, count) {
  return t(`${key}.${count === 1 ? 'one' : 'other'}`, { count });
}

function translateAttributes(node) {
  node.dataset.i18nAttr.split(';').forEach((pair) => {
    const [attribute, key] = pair.split(':');
    if (attribute && key) {
      node.setAttribute(attribute.trim(), t(key.trim()));
    }
  });
}

export function applyTranslations(root = document) {
  document.documentElement.lang = current;
  root.querySelectorAll('[data-i18n]').forEach((node) => {
    node.textContent = t(node.dataset.i18n);
  });
  root.querySelectorAll('[data-i18n-attr]').forEach(translateAttributes);
  root.querySelectorAll('[data-lang-switch]').forEach(renderSwitch);
}

function renderSwitch(container) {
  container.setAttribute('role', 'group');
  container.setAttribute('aria-label', t('lang.label'));
  container.classList.add('lang-switch');
  container.replaceChildren(...SUPPORTED.map((code) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = code.toUpperCase();
    button.lang = code;
    button.setAttribute('aria-pressed', String(code === current));
    button.addEventListener('click', () => setLanguage(code));
    return button;
  }));
}

export function setLanguage(code) {
  if (!SUPPORTED.includes(code) || code === current) {
    return;
  }
  current = code;
  writeStored(code);
  applyTranslations();
  listeners.forEach((listener) => listener(code));
}

export function onLanguageChange(listener) {
  listeners.push(listener);
}

applyTranslations();
