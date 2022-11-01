import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { DragDropModule } from "@angular/cdk/drag-drop";
import { ScrollingModule } from "@angular/cdk/scrolling";
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
import { MatRadioModule } from "@angular/material/radio";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientModule } from "@angular/common/http";
import { MatSliderModule } from "@angular/material/slider";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatTabsModule } from "@angular/material/tabs";
import { MatSnackBarModule } from "@angular/material/snack-bar";

import { AdbProxyComponent } from "./components/adb_proxy.component";
import { AppComponent } from "./components/app.component";
import { CollectTracesComponent } from "./components/collect_traces.component";
import { ParserErrorSnackBarComponent } from "./components/parser_error_snack_bar_component";
import { TraceConfigComponent } from "./components/trace_config.component";
import { TraceViewComponent } from "./components/trace_view.component";
import { UploadTracesComponent } from "./components/upload_traces.component";
import { WebAdbComponent } from "./components/web_adb.component";

import { CoordinatesTableComponent } from "viewers/components/coordinates_table.component";
import { HierarchyComponent } from "viewers/components/hierarchy.component";
import { ImeAdditionalPropertiesComponent } from "viewers/components/ime_additional_properties.component";
import { PropertiesComponent } from "viewers/components/properties.component";
import { PropertiesTableComponent } from "viewers/components/properties_table.component";
import { PropertyGroupsComponent } from "viewers/components/property_groups.component";
import { RectsComponent } from "viewers/components/rects/rects.component";
import { TransformMatrixComponent } from "viewers/components/transform_matrix.component";
import { TreeComponent } from "viewers/components/tree.component";
import { TreeNodeComponent } from "viewers/components/tree_node.component";
import { TreeNodeDataViewComponent } from "viewers/components/tree_node_data_view.component";
import { TreeNodePropertiesDataViewComponent } from "viewers/components/tree_node_properties_data_view.component";
import { ViewerInputMethodComponent } from "viewers/components/viewer_input_method.component";
import { ViewerProtologComponent} from "viewers/viewer_protolog/viewer_protolog.component";
import { ViewerScreenRecordingComponent } from "viewers/viewer_screen_recording/viewer_screen_recording.component";
import { ViewerSurfaceFlingerComponent } from "viewers/viewer_surface_flinger/viewer_surface_flinger.component";
import { ViewerTransactionsComponent } from "viewers/viewer_transactions/viewer_transactions.component";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";

@NgModule({
  declarations: [
    AppComponent,
    ViewerWindowManagerComponent,
    ViewerSurfaceFlingerComponent,
    ViewerInputMethodComponent,
    ViewerProtologComponent,
    ViewerTransactionsComponent,
    ViewerScreenRecordingComponent,
    CollectTracesComponent,
    UploadTracesComponent,
    AdbProxyComponent,
    WebAdbComponent,
    TraceConfigComponent,
    HierarchyComponent,
    PropertiesComponent,
    RectsComponent,
    TraceViewComponent,
    TreeComponent,
    TreeNodeComponent,
    TreeNodeDataViewComponent,
    TreeNodePropertiesDataViewComponent,
    PropertyGroupsComponent,
    TransformMatrixComponent,
    ParserErrorSnackBarComponent,
    PropertiesTableComponent,
    ImeAdditionalPropertiesComponent,
    CoordinatesTableComponent,
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
    HttpClientModule,
    MatSliderModule,
    MatRadioModule,
    MatTooltipModule,
    MatToolbarModule,
    MatTabsModule,
    MatSnackBarModule,
    ScrollingModule,
    DragDropModule,
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
