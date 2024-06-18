import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';

import { MotionGolden } from '../golden';
import { GoldensService } from '../goldens.service';

@Component({
  selector: 'app-motion-golden',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  templateUrl: './motion-golden.component.html',
  styleUrl: './motion-golden.component.scss',
})
export class MotionGoldenComponent {
  constructor(
    private goldenService: GoldensService,
    private _snackBar: MatSnackBar,
  ) {}

  @Input() golden!: MotionGolden;

  updateGolden(): void {
    this.goldenService
      .updateGolden(this.golden)
      .pipe(finalize(() => {}))
      .subscribe((_) => this._snackBar.open('updated'));
  }
}
