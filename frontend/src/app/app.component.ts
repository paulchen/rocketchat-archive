import {Component, OnInit} from '@angular/core';
import {BackendService} from "./backend.service";
import {Channel, ChannelData} from "./channel-data";
import {MessageData} from "./message-data";
import {User, UserData} from "./user-data";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  channelData: ChannelData;
  selectedChannel: Channel;
  messageData: MessageData;
  users: User[] = []
  limit = 100;
  loading = true;

  constructor(private backendService: BackendService) { }

  ngOnInit(): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      this.selectedChannel = this.channelData.channels[0];

      this.messageData = new MessageData();

      this.backendService.getUsers().subscribe(response => {
        this.users = response.users;
      });
    });
  }

  handleTabChange(event: any) {
    this.selectedChannel = this.channelData.channels[event.index];
  }

  handleTableChange(event: any) {
    let component = this;
    setTimeout(function() { component.reloadData(event) }, 100);
  }

  reloadData(event: any) {
    this.loading = true;

    const limit = event.rows;
    const first = event.first;
    const page = (first / limit) + 1;

    const sort = (event.sortOrder == -1) ? "desc" : "asc";

    const filters = event.filters
    let userIds = [];
    let message = "";
    if (filters) {
      if ("username" in filters && "value" in filters["username"] && filters["username"]["value"]) {
        userIds = filters["username"]["value"]
      }
      if ("message" in filters && "value" in filters["message"] && filters["message"]["value"]) {
        message = filters["message"]["value"]
      }
    }

    this.backendService.getMessages(this.selectedChannel, page, limit, sort, userIds, message).subscribe(response => {
      this.messageData = response;
      this.loading = false;
    });
  }
}
