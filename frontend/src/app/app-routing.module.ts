import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {MainComponent} from "./main/main.component";
import {StatsComponent} from "./stats/stats.component";
import {ReportsComponent} from "./reports/reports.component";

const routes: Routes = [
  { path: 'stats', component: StatsComponent },
  { path: 'stats/:channel', component: StatsComponent },
  { path: 'reports', component: ReportsComponent },
  { path: '**', component: MainComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
