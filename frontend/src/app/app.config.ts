import {ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection} from '@angular/core';
import { provideRouter } from '@angular/router';

import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';
import {provideOptimus} from '@openng/optimus-ui/config';
import Aura from '@openng/optimus-ui-themes/aura';
import {provideHttpClient, withXhr} from '@angular/common/http';
import {routes} from "./app.routes";
import {definePreset} from "@openng/optimus-ui-themes";
import {ConfigService} from "./config.service";

// TODO get rid of primeflex
const CustomTheme = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}'
    }
  }
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideOptimus({
      theme: {
        preset: CustomTheme
      }
    }),
    provideHttpClient(withXhr()),
    provideAppInitializer(() => inject(ConfigService).loadConfig()),
  ]
};
