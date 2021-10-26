import { Component, OnInit } from '@angular/core';
import {Router} from "@angular/router";
import {BackendService} from "../backend.service";
import {Channel, ChannelData, ChannelStats} from "../channel-data";

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {
  channelData: ChannelData = new ChannelData();
  selectedChannel: string;
  stats: ChannelStats;
  dataLoaded: boolean;

  constructor(
    public router: Router,
    private backendService: BackendService
  ) { }

  ngOnInit(): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      this.selectChannel(this.channelData.channels[0])
    });
  }

  private selectChannel(channel: Channel): void {
    this.selectedChannel = channel.name;
    this.loadStats(this.selectedChannel);
  }

  loadStats(channelName: string) {
    let channel = this.findChannel(channelName);
    this.backendService.getChannelStats(channel).subscribe(response => {
      this.stats = response;
      this.dataLoaded = true;
    });
  }

  private findChannel(channelName: string): Channel {
    for (let i = 0; i < this.channelData.channels.length; i++) {
      if (this.channelData.channels[i].name == channelName) {
        return this.channelData.channels[i];
      }
    }
    return this.channelData.channels[0];
  }
}
