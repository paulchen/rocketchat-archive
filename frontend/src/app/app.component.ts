import {Component, OnInit} from '@angular/core';
import {BackendService} from "./backend.service";
import {Channel, ChannelData} from "./channel-data";
import {MessageData} from "./message-data";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  channelData: ChannelData;
  selectedChannel: Channel;
  messageData: MessageData

  constructor(private backendService: BackendService) { }

  ngOnInit(): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      this.selectChannel(this.channelData.channels[0])
    });
  }

  selectChannel(channel: Channel) {
    this.selectedChannel = channel;

    this.backendService.getMessages(channel).subscribe(response => {
      this.messageData = response;
    });
  }

}
