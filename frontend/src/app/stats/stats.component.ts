import { Component, OnInit } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {BackendService} from "../backend.service";
import {Channel, ChannelData, ChannelStats} from "../channel-data";
import {Location} from "@angular/common";

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {
  channelData: ChannelData = new ChannelData();
  selectedChannel: Channel;
  startDate: Date;
  endDate: Date = new Date();
  stats: ChannelStats;
  dataLoaded: boolean;
  channelNotFound: true;

  constructor(
    public router: Router,
    private route: ActivatedRoute,
    private backendService: BackendService,
    private location: Location
  ) { }

  ngOnInit(): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      this.channelData.channels.unshift.apply(this.channelData.channels, [{ name: "all", id: "all" }])
      const channel = this.route.snapshot.paramMap.get('channel');
      if (channel == undefined) {
        this.router.navigate(["/stats", this.channelData.channels[1].id]).then();
      }
      else {
        this.route.pathFromRoot[1].queryParams.subscribe(params => {
          if (Object.prototype.hasOwnProperty.call(params, 'startDate')) {
            this.startDate = new Date(params['startDate']);
          }
          if (Object.prototype.hasOwnProperty.call(params, 'endDate')) {
            this.endDate = new Date(params['endDate']);
          }
        });

        this.selectChannel(this.route.snapshot.paramMap.get('channel') ?? '');
      }
    });
  }

  navigateToChannel(channel: Channel): void {
    this.router.navigate(['/stats', channel.id ]).then(() => {
      this.loadStats(channel);
    });
  }

  private selectChannel(channelId: string): void {
    let channel = this.findChannel(channelId);
    if (channel == undefined) {
      this.channelNotFound = true;
      this.dataLoaded = true;
      return;
    }

    this.loadStats(channel, this.startDate, this.endDate);
  }

  private loadStats(channel: Channel, startDate: Date | null = null, endDate: Date | null = null) {
    this.selectedChannel = channel;
    this.dataLoaded = false;
    let startDateString = this.formatDate(startDate);
    let endDateString = this.formatDate(endDate);
    this.backendService.getChannelStats(channel, startDateString, endDateString).subscribe(response => {
      if (startDate == null) {
        this.startDate = new Date(response.firstMessageDate);
        this.endDate = new Date();
      }
      this.stats = response;
      this.dataLoaded = true;
      this.updateUrl();
    });
  }

  private findChannel(channelId: string): Channel | undefined {
    return this.channelData.channels.find((item) => { return item.id == channelId } );
  }

  navigateToArchive(): void {
    if (this.selectedChannel.id == 'all') {
      this.router.navigate(['/']).then();
    }
    else {
      this.router.navigate(['/' + this.selectedChannel.id]).then();
    }
  }

  reloadData() {
    this.loadStats(this.selectedChannel, this.startDate, this.endDate);
  }

  resetDateRange() {
    this.startDate = new Date(this.stats.firstMessageDate);
    this.endDate = new Date();
    this.loadStats(this.selectedChannel);
  }

  private updateUrl(): void {
    let url = '/stats/' + this.selectedChannel.id;

    let parameters: string[] = [];
    if (this.formatDate(this.startDate) != this.formatDate(new Date(this.stats.firstMessageDate))) {
      parameters.push('startDate=' + encodeURIComponent(this.formatDate(this.startDate)));
    }
    if (this.formatDate(this.endDate) != this.formatDate(new Date())) {
      parameters.push('endDate=' + encodeURIComponent(this.formatDate(this.endDate)));
    }
    if (parameters.length > 0) {
      url += '?';
      url += parameters.join("&")
    }

    this.location.go(url);
  }

  private formatDate(date: Date | null): string {
    if (date == null) {
      return "";
    }
    else {
      return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().split('T')[0]
    }
  }
}
