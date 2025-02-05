import {Component, Input, OnInit} from '@angular/core';
import {MessageCount} from "../channel-data";
import {TableModule} from "primeng/table";

@Component({
  selector: 'app-stats-table',
  templateUrl: './stats-table.component.html',
  imports: [
    TableModule
  ],
  styleUrls: ['./stats-table.component.scss']
})
export class StatsTableComponent implements OnInit {
  @Input() stats: MessageCount[];

  @Input() firstColumn: String;

  constructor() { }

  ngOnInit(): void {
  }

}
