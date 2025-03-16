/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, InjectionToken } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, tap } from 'rxjs/operators';

import { MotionGolden, MotionGoldenData } from '../model/golden';
import { RecordedMotion } from '../model/recorded-motion';
import { Timeline } from '../model/timeline';
import { VideoSource } from '../model/video-source';
import { checkNotNull } from '../util/preconditions';
import { Feature, recordedFeatureFactory } from '../model/feature';

export const ACCESS_TOKEN = new InjectionToken<string>('token');
export const SERVICE_PORT = new InjectionToken<string>('port');

@Injectable({ providedIn: 'root' })
export class GoldensService {
  private serverRoot: string;
  private defaultHeaders: { [heder: string]: string };

  constructor(
    private http: HttpClient,
    @Inject(ACCESS_TOKEN) config: string,
    @Inject(SERVICE_PORT) port: string
  ) {
    this.serverRoot = `http://localhost:${port}`;
    this.defaultHeaders = {
      'Golden-Access-Token': config,
    };
  }

  getGoldens(): Observable<MotionGolden[]> {
    return this.http
      .get<MotionGolden[]>(`${this.serverRoot}/service/list`, {
        headers: this.defaultHeaders,
      })
      .pipe(
        tap((x) => console.log(`listed goldens, got ${x.length} results`)),
        catchError(this.handleError<MotionGolden[]>('e'))
      );
  }

  loadRecordedMotion(golden: MotionGolden): Observable<RecordedMotion> {
    const videoUrl = checkNotNull(golden.videoUrl);
    return this.getActualGoldenData(golden).pipe(
      map((data) => {
        const timeline = new Timeline(data.frame_ids);
        const videoSource = new VideoSource(videoUrl, timeline);
        const features = data.features.map((it) => recordedFeatureFactory(it));

        return new RecordedMotion(videoSource, timeline, features);
      })
    );
  }

  getActualGoldenData(golden: MotionGolden): Observable<MotionGoldenData> {
    return this.http
      .get<MotionGoldenData>(`${golden.actualUrl}`, {
        headers: this.defaultHeaders,
      })
      .pipe(
        tap((x) => console.log(`listed loaded golden data`)),
        catchError(this.handleError<MotionGoldenData>('e'))
      );
  }

  getExpectedGoldenData(golden: MotionGolden): Observable<MotionGoldenData> {
    return this.http
      .get<MotionGoldenData>(`${golden.expectedUrl}`, {
        headers: this.defaultHeaders,
      })
      .pipe(
        tap((x) => console.log('listed expected golden data')),
        catchError(this.handleError<MotionGoldenData>('e'))
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
        }
      )
      .pipe(
        tap((_) => console.log(`refreshed goldens (clear)`)),
        catchError(this.handleError<MotionGolden[]>('e'))
      );
  }

  updateGolden(golden: MotionGolden): Observable<void> {
    return this.http
      .put<void>(
        `${this.serverRoot}/service/update?id=${golden.id}`,
        {},
        { headers: this.defaultHeaders }
      )
      .pipe(
        tap((_) => {
          console.log(`updated golden`);
          golden.updated = true;
        }),
        catchError(this.handleError<void>('update'))
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
