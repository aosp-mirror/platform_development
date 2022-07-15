import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";

import { AppComponent } from "./app.component";
import { ViewerWindowManagerComponent} from "viewers/viewer_window_manager/viewer_window_manager.component";

@NgModule({
  declarations: [
    AppComponent,
    ViewerWindowManagerComponent
  ],
  imports: [
    BrowserModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
