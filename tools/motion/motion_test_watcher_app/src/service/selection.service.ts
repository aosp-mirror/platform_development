import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SelectionService {
  private selectedGoldenSource = new BehaviorSubject<string | null>(null);
  selectedGolden$: Observable<string | null> =
    this.selectedGoldenSource.asObservable();

  constructor() {}

  setSelectedGolden(goldenId: string | null): void {
    this.selectedGoldenSource.next(goldenId);
  }
}
