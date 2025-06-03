import { Component } from '@angular/core';
import { MatDialogRef, MatDialogContent, MatDialogTitle, MatDialogActions } from '@angular/material/dialog';
import { MatInput } from '@angular/material/input';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-dialog-content',
  templateUrl: './dialog.component.html',
  standalone: true,
  imports: [
     MatInput,
     MatFormField,
     FormsModule,
     MatButton,
     MatDialogContent,
     MatDialogTitle,
     MatDialogActions,
     MatLabel
  ]
})
export class DialogContentComponent {
  inputText: string = '';

  constructor(public dialogRef: MatDialogRef<DialogContentComponent>) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  getArtifacts(): void {
    this.dialogRef.close(this.getInvocationId(this.inputText));
  }

  // invocationIdentifier could either be the invocation url or just the invocation id
  getInvocationId(invocationIdentifier : String) : String {
    if (invocationIdentifier.startsWith("http")) {
    const parts: string[] = invocationIdentifier.split('/')
    const keyword = "invocation";
    const invocationIndex: number = parts.indexOf(keyword);

    if (invocationIndex !== -1 && invocationIndex + 1 < parts.length) {
      const invocationId: string = parts[invocationIndex + 1];
      return invocationId;
    } else {
      if (invocationIndex === -1) {
        console.error(`The keyword '${keyword}' was not found in the URL path segments.`);
      } else {
        console.error(`The URL structure is unexpected; no segment found after '${keyword}'.`);
      }
      return "";
    }
  }else{
    return invocationIdentifier
  }
}
}
