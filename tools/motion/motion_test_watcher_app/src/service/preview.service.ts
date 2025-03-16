import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PreviewService {
  private frameCountSource = new BehaviorSubject<Array<string | number> | null>(null);
  frameCount$ = this.frameCountSource.asObservable();
  updateFrames(frames: Array<string | number> | null) {
    this.frameCountSource.next(frames);
  }


  private currentFrameFromView = new BehaviorSubject<number | null>(null);
  currentFrameFromView$ = this.currentFrameFromView.asObservable();
  setCurrentFrameFromView(frame: number | null) {
    this.currentFrameFromView.next(frame);
  }
}
