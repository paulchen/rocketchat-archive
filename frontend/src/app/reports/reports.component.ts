import { Component, OnInit } from '@angular/core';
import {BackendService} from "../backend.service";
import {Router} from "@angular/router";
import {Report, ReportData} from "../report-data";
import {TableModule} from "primeng/table";
import {ButtonDirective, ButtonLabel} from "primeng/button";
import { DatePipe } from "@angular/common";

@Component({
  selector: 'app-main',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss'],
  imports: [
    TableModule,
    ButtonDirective,
    ButtonLabel,
    DatePipe
]
})
export class ReportsComponent implements OnInit {
  reportData: ReportData | null;
  limit = 100;
  loading = true;
  private timeout: number;
  first: number = 0;

  constructor(
    private backendService: BackendService,
    public router: Router
  ) { }

  ngOnInit(): void {
    this.loadData(1, 1, "desc")
  }

  navigateToMessage(selectedReport: Report) {
    let url = '/' + encodeURIComponent(selectedReport.message.rid) + "/" + encodeURIComponent(selectedReport.message.id);
    this.router.navigate([url])
  }

  handleTableChange(event: any) {
    let component = this;
    clearTimeout(this.timeout);
    this.timeout = setTimeout(function() { component.reloadData(event) }, 100);
  }

  reloadData(event: any) {
    this.loading = true;

    const limit = event.rows;
    const first = event.first;
    const page = (first / limit) + 1;

    const sort = (event.sortOrder == -1) ? "desc" : "asc";

    this.first = (page - 1) * this.limit;

    this.loadData(page, limit, sort)
  }

  loadData(page: number, limit: number, sort: string) {
    this.backendService.getReports(page, limit, sort).subscribe({
      next: response => {
        this.reportData = response;
        this.loading = false;
        clearTimeout(this.timeout);
      },
      error: () => {
        this.loading = false;

        clearTimeout(this.timeout);
      }
    });
  }
}
