import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from "@angular/common/http";
import {CommonModule} from "@angular/common";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {TabViewModule} from "primeng/tabview";
import {TableModule} from "primeng/table";
import {MultiSelectModule} from "primeng/multiselect";
import {FormsModule} from "@angular/forms";
import { MainComponent } from './main/main.component';
import {ContextMenuModule} from "primeng/contextmenu";
import {ButtonModule} from "primeng/button";
import {ToastModule} from "primeng/toast";
import {EncodeUrlParamsSafelyInterceptor} from "./encode-url-params-safely-interceptor";
import { StatsComponent } from './stats/stats.component';
import {PanelModule} from "primeng/panel";
import {DropdownModule} from "primeng/dropdown";
import { StatsTableComponent } from './stats-table/stats-table.component';
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {ReportsComponent} from "./reports/reports.component";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";

@NgModule({
  declarations: [
    AppComponent,
    MainComponent,
    ReportsComponent,
    StatsComponent,
    StatsTableComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    HttpClientModule,
    CommonModule,
    TabViewModule,
    TableModule,
    MultiSelectModule,
    FormsModule,
    ContextMenuModule,
    ButtonModule,
    ToastModule,
    PanelModule,
    DropdownModule,
    ProgressSpinnerModule,
    DialogModule,
    CalendarModule
  ],
  providers: [{
      provide: HTTP_INTERCEPTORS,
      useClass: EncodeUrlParamsSafelyInterceptor,
      multi: true,
  }],
  bootstrap: [AppComponent]
})
export class AppModule { }
