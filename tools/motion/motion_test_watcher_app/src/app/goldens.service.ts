import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, InjectionToken } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { Golden } from './golden';

export const ACCESS_TOKEN = new InjectionToken<string>('token');
export const SERVICE_PORT = new InjectionToken<string>('port');

@Injectable({ providedIn: 'root' })
export class GoldensService {
  private serverRoot: string;
  private defaultHeaders: { [heder: string]: string };

  constructor(
    private http: HttpClient,
    @Inject(ACCESS_TOKEN) config: string,
    @Inject(SERVICE_PORT) port: string,
  ) {
    this.serverRoot = `http://localhost:${port}`;
    this.defaultHeaders = {
      'Golden-Access-Token': config,
    };
  }

  getGoldens(): Observable<Golden[]> {
    return this.http
      .get<
        Golden[]
      >(`${this.serverRoot}/service/list`, { headers: this.defaultHeaders })
      .pipe(
        tap((x) => console.log(`listed goldens, got ${x.length} results`)),
        catchError(this.handleError<Golden[]>('e')),
      );
  }

  refreshGoldens(clear: boolean): Observable<Golden[]> {
    return this.http
      .post<Golden[]>(
        `${this.serverRoot}/service/refresh`,
        { clear },
        {
          headers: {
            ...this.defaultHeaders,
            'Content-Type': 'application/json',
          },
        },
      )
      .pipe(
        tap((_) => console.log(`refreshed goldens (clear)`)),
        catchError(this.handleError<Golden[]>('e')),
      );
  }

  updateGolden(golden: Golden): Observable<void> {
    return this.http
      .put<void>(
        `${this.serverRoot}/service/update?id=${golden.id}`,
        {},
        { headers: this.defaultHeaders },
      )
      .pipe(
        tap((_) => {
          console.log(`updated golden`);
          golden.updated = true;
        }),
        catchError(this.handleError<void>('update')),
      );
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }
}
