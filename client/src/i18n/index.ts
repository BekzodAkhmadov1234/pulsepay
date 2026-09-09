import { createI18n } from 'vue-i18n';
import { getLang } from '@/stores/lang';
import { uz, ru, en, zh, uz_c, type MessageSchema } from './messages';

export const i18n = createI18n<[MessageSchema], 'uz' | 'ru' | 'en' | 'zh' | 'uz_c'>({
  legacy: false, // use Composition API mode
  locale: getLang() as 'uz' | 'ru' | 'en' | 'zh' | 'uz_c',
  fallbackLocale: 'uz',
  messages: { uz, ru, en, zh, uz_c },
});
