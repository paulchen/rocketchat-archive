import {Component, Input, OnInit} from '@angular/core';
import {MessageCount} from "../channel-data";

@Component({
  selector: 'app-stats-table',
  templateUrl: './stats-table.component.html',
  styleUrls: ['./stats-table.component.scss']
})
export class StatsTableComponent implements OnInit {
  @Input() stats: MessageCount[];

  @Input() firstColumn: String;

  constructor() { }

  ngOnInit(): void {
  }

}
