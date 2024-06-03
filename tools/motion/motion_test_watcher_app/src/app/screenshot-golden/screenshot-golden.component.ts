import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';

import { ScreenshotGolden } from '../golden';
import { GoldensService } from '../goldens.service';

@Component({
  selector: 'app-screenshot-golden',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatButtonToggleModule, FormsModule],
  templateUrl: './screenshot-golden.component.html',
  styleUrl: './screenshot-golden.component.scss',
})
export class ScreenshotGoldenComponent implements OnChanges {
  constructor(
    private goldenService: GoldensService,
    private _snackBar: MatSnackBar,
  ) {}

  @Input() golden!: ScreenshotGolden;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['golden']) {
      if (
        (this.selectedImage == 'diff' && !this.golden.diffUrl) ||
        (this.selectedImage == 'expected' && !this.golden.expectedUrl)
      ) {
        this.selectedImage = 'actual';
      }
    }
  }

  selectedImage: 'actual' | 'diff' | 'expected' = 'actual';
  get selectedImageUrl(): String | undefined {
    switch (this.selectedImage) {
      case 'actual':
        return this.golden.actualUrl;
      case 'diff':
        return this.golden.diffUrl;
      case 'expected':
        return this.golden.expectedUrl;
    }
    return undefined;
  }

  updateGolden(): void {
    this.goldenService
      .updateGolden(this.golden)
      .pipe(finalize(() => {}))
      .subscribe((_) => this._snackBar.open('updated'));
  }
}
