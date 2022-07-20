import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { CommonModule } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatGridListModule } from "@angular/material/grid-list";
import { MatListModule } from "@angular/material/list";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { FormsModule } from "@angular/forms";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientModule } from "@angular/common/http";

import { AppComponent } from "./app.component";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";
import { CollectTracesComponent } from "./collect_traces.component";
import { AdbProxyComponent } from "./adb_proxy.component";
import { WebAdbComponent } from "./web_adb.component";
import { TraceConfigComponent } from "./trace_config.component";

@NgModule({
  declarations: [
    AppComponent,
    ViewerWindowManagerComponent,
    CollectTracesComponent,
    AdbProxyComponent,
    WebAdbComponent,
    TraceConfigComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatGridListModule,
    FormsModule,
    MatListModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    BrowserAnimationsModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
