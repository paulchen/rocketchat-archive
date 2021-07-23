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
  messageData: MessageData;
  currentPage: number;

  constructor(private backendService: BackendService) { }

  ngOnInit(): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      this.selectChannel(this.channelData.channels[0])
    });
  }

  selectChannel(channel: Channel) {
    this.selectedChannel = channel;
    this.currentPage = 1;

    this.loadMessages();
  }

  private loadMessages() {
    this.backendService.getMessages(this.selectedChannel, this.currentPage).subscribe(response => {
      this.messageData = response;
    });
  }

  loadFirstPage() {
    this.currentPage = 1;
    this.loadMessages();
  }

  loadNextPage() {
    this.currentPage++;
    if (this.currentPage > this.messageData.pages) {
      this.currentPage = this.messageData.pages;
    }
    this.loadMessages();
  }

  loadPreviousPage() {
    this.currentPage--;
    if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    this.loadMessages()
  }

  loadLastPage() {
    this.currentPage = this.messageData.pages;
    this.loadMessages();
  }

}
