import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, InjectionToken } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, tap } from 'rxjs/operators';

import { MotionGolden, MotionGoldenData } from './golden';
import { RecordedMotion } from './recorded-motion';
import { Timeline } from './timeline';
import { VideoSource } from './video-source';
import { checkNotNull } from '../utils/preconditions';
import { Feature, recordedFeatureFactory } from './feature';

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

  getGoldens(): Observable<MotionGolden[]> {
    return this.http
      .get<
        MotionGolden[]
      >(`${this.serverRoot}/service/list`, { headers: this.defaultHeaders })
      .pipe(
        tap((x) => console.log(`listed goldens, got ${x.length} results`)),
        catchError(this.handleError<MotionGolden[]>('e')),
      );
  }

  loadRecordedMotion(golden: MotionGolden): Observable<RecordedMotion> {
    const videoUrl = checkNotNull(golden.videoUrl);
    return this.getMotionGoldenData(golden).pipe(
      map((data) => {
        const timeline = new Timeline(data.frame_ids);
        const videoSource = new VideoSource(videoUrl, timeline);
        const features = data.features.map((it) => recordedFeatureFactory(it));

        return new RecordedMotion(videoSource, timeline, features);
      }),
    );
  }

  getMotionGoldenData(golden: MotionGolden): Observable<MotionGoldenData> {
    return this.http
      .get<MotionGoldenData>(`${golden.actualUrl}`, {
        headers: this.defaultHeaders,
      })
      .pipe(
        tap((x) => console.log(`listed loaded golden data`)),
        catchError(this.handleError<MotionGoldenData>('e')),
      );
  }

  refreshGoldens(clear: boolean): Observable<MotionGolden[]> {
    return this.http
      .post<MotionGolden[]>(
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
        catchError(this.handleError<MotionGolden[]>('e')),
      );
  }

  updateGolden(golden: MotionGolden): Observable<void> {
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
