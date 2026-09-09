import { ref } from 'vue';
import { defineStore } from 'pinia';

export type LangCode = 'uz' | 'ru' | 'en' | 'zh' | 'uz_c';

export const LANG_KEY = 'pp_lang';

export const LANG_OPTIONS: { code: LangCode; label: string; display: string }[] = [
  { code: 'uz', label: 'UZ', display: "O'zbek" },
  { code: 'ru', label: 'RU', display: 'Русский' },
  { code: 'en', label: 'EN', display: 'English' },
  { code: 'zh', label: '中文', display: '中文' },
  { code: 'uz_c', label: 'УЗ', display: 'Ўзбек' },
];

/** Reads the persisted language without requiring Pinia — safe to call from API clients. */
export function getLang(): string {
  return localStorage.getItem(LANG_KEY) ?? 'uz';
}

export const useLangStore = defineStore('lang', () => {
  const lang = ref<LangCode>((localStorage.getItem(LANG_KEY) as LangCode | null) ?? 'uz');

  function setLang(code: LangCode) {
    lang.value = code;
    localStorage.setItem(LANG_KEY, code);
    // Sync vue-i18n locale — lazy import avoids circular dep at module-load time
    import('@/i18n').then(({ i18n }) => {
      i18n.global.locale.value = code;
    });
  }

  return { lang, setLang };
});
