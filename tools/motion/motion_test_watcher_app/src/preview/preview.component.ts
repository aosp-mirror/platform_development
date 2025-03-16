import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { MotionGolden } from '../model/golden';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NgIf } from '@angular/common';
import { PreviewService } from '../service/preview.service';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-preview',
  imports: [
    NgIf,
    FormsModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './preview.component.html',
  styleUrl: './preview.component.css',
})
export class PreviewComponent implements OnChanges, AfterViewInit {
  constructor(
    private sanitizer: DomSanitizer,
    private previewService: PreviewService
  ) {
    this.previewService.frameCount$.subscribe(
      (frames) => (this.frames = frames)
    );
  }

  @Input() selectedGolden: MotionGolden | null = null;
  videoUrl: SafeResourceUrl | null = null;
  frames: Array<string | number> | null = null;
  currentFrame: number | null = null;
  animationFrameId: number | null = null;
  playbackSpeed = 0.25;
  isPlaying: boolean = false

  @ViewChild('videoPlayer')
  videoPlayer!: ElementRef<HTMLVideoElement>;

  ngAfterViewInit(): void {
    if (this.videoPlayer && this.videoPlayer.nativeElement) {
      this.videoPlayer.nativeElement.addEventListener('play', () => {
        this.updateFrame();
        this.isPlaying = true;
      });

      this.videoPlayer.nativeElement.addEventListener('pause', () => {
        if (this.animationFrameId) {
          cancelAnimationFrame(this.animationFrameId);
          this.animationFrameId = null;
          this.isPlaying = false;
        }
      });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedGolden']) {
      const currentGolden: MotionGolden | null =
        changes['selectedGolden'].currentValue;

      if (currentGolden?.videoUrl) {
        this.videoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
          currentGolden.videoUrl
        );
        this.ngAfterViewInit()
      } else {
        this.videoUrl = null;
      }
    }
  }

  updateFrame() {
    this.calculateCurrentFrame();
    this.animationFrameId = requestAnimationFrame(() => this.updateFrame());
  }

  calculateCurrentFrame() {
    if (this.videoPlayer && this.videoPlayer.nativeElement && this.frames) {
      const currentTime = this.videoPlayer.nativeElement.currentTime;
      const totalDuration = this.videoPlayer.nativeElement.duration;
      const frameCount = this.frames.length - 2;

      if (currentTime && totalDuration) {
        this.currentFrame = Math.round(
          (currentTime / totalDuration) * frameCount
        );
        const nextFrame = this.frames[this.currentFrame];
        if (
          this.currentFrame < this.frames.length - 1 &&
          typeof nextFrame === 'number'
        ) {
          this.previewService.setCurrentFrameFromView(nextFrame);
        } else {
          this.previewService.setCurrentFrameFromView(0);
        }
      } else {
        this.currentFrame = null;
        this.previewService.setCurrentFrameFromView(0);
      }
    } else {
      this.currentFrame = null;
      this.previewService.setCurrentFrameFromView(0);
    }
  }

  goToStart() {
    if (this.videoPlayer && this.videoPlayer.nativeElement) {
      this.videoPlayer.nativeElement.currentTime = 0;
      this.previewService.setCurrentFrameFromView(0)
    }
  }

  togglePlayPause() {
    if (this.videoPlayer && this.videoPlayer.nativeElement) {
      if (this.videoPlayer.nativeElement.paused) {
        this.isPlaying = true
        this.videoPlayer.nativeElement.playbackRate = this.playbackSpeed;
        this.videoPlayer.nativeElement.play();
        this.updateFrame();
      } else {
        this.videoPlayer.nativeElement.pause();
        if (this.animationFrameId) {
          this.isPlaying = false
          cancelAnimationFrame(this.animationFrameId);
          this.animationFrameId = null;
        }
      }
    }
  }

  stepBackward() {
    if (this.videoPlayer && this.videoPlayer.nativeElement && this.frames && this.currentFrame !== null) {
      if(this.currentFrame > 0) {
        const frameCount = this.frames.length - 2;
        const totalDuration = this.videoPlayer.nativeElement.duration;
        const targetTime = (this.currentFrame - 1) / frameCount * totalDuration
        this.videoPlayer.nativeElement.currentTime = targetTime;
      }
    }
  }

  setPlaybackSpeed() {
    if (this.videoPlayer && this.videoPlayer.nativeElement) {
      this.videoPlayer.nativeElement.playbackRate = this.playbackSpeed;
    }
  }

  stepForward() {
    if (this.videoPlayer && this.videoPlayer.nativeElement && this.frames && this.currentFrame !== null) {
        const frameCount = this.frames.length - 2;
        const totalDuration = this.videoPlayer.nativeElement.duration;
      if(this.currentFrame < frameCount) {
        const targetTime = (this.currentFrame + 1) / frameCount * totalDuration
        this.videoPlayer.nativeElement.currentTime = targetTime;
      }

    }
  }

  goToEnd() {
    if (this.videoPlayer && this.videoPlayer.nativeElement) {
      this.videoPlayer.nativeElement.currentTime = this.videoPlayer.nativeElement.duration;
      this.updateFrame()
    }
  }
}
