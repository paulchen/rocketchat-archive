import { Component, OnInit } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {BackendService} from "../backend.service";
import {Channel, ChannelData, ChannelStats} from "../channel-data";

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {
  channelData: ChannelData = new ChannelData();
  selectedChannel: Channel;
  stats: ChannelStats;
  dataLoaded: boolean;
  channelNotFound: true;

  constructor(
    public router: Router,
    private route: ActivatedRoute,
    private backendService: BackendService
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

    this.loadStats(channel);
  }

  private loadStats(channel: Channel) {
    this.selectedChannel = channel;
    this.dataLoaded = false;
    this.backendService.getChannelStats(channel).subscribe(response => {
      this.stats = response;
      this.dataLoaded = true;
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
}
