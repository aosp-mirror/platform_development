import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { ACCESS_TOKEN, SERVICE_PORT } from './goldens.service';

const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');
const port = urlParams.get('port');

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    { provide: ACCESS_TOKEN, useValue: token },
    { provide: SERVICE_PORT, useValue: port },
  ],
};
