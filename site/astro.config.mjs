// Pages are pre-rendered for Cairo (the app's default city) so search engines see real times;
// in the browser everything is recomputed for the visitor's own city and clock.
process.env.TZ = 'Africa/Cairo';

import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

export default defineConfig({
  site: 'https://egyjs.github.io',
  base: '/afnon-android-tv-launcher',
  trailingSlash: 'always',
  integrations: [
    sitemap({ i18n: { defaultLocale: 'ar', locales: { ar: 'ar', en: 'en' } } }),
    // UI mocks for review (src/mocks): served by `npm run dev` at /mocks/, never built.
    {
      name: 'afnon-mocks',
      hooks: {
        'astro:config:setup': ({ command, injectRoute }) => {
          if (command === 'dev') injectRoute({ pattern: '/mocks/[...path]', entrypoint: './src/mocks/MockPage.astro' });
        },
      },
    },
  ],
});
