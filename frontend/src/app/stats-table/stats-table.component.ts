import {Component, Input, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {MessageCount} from "../channel-data";
import {TableModule} from "@openng/optimus-ui/table";

@Component({
  selector: 'app-stats-table',
  templateUrl: './stats-table.component.html',
  imports: [
    TableModule
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./stats-table.component.scss']
})
export class StatsTableComponent implements OnInit {
  @Input() stats: MessageCount[];

  @Input() firstColumn: String;

  constructor() { }

  ngOnInit(): void {
  }

}
